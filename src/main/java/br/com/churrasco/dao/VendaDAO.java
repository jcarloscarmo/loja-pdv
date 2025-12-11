package br.com.churrasco.dao;

import br.com.churrasco.model.Caixa;
import br.com.churrasco.model.Encomenda;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import br.com.churrasco.model.Produto;
import br.com.churrasco.model.Venda;
import br.com.churrasco.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VendaDAO {

    private CaixaDAO caixaDAO = new CaixaDAO();

    // --- 1. SALVAR VENDA (COM DESCONTO) ---
    public int salvarVenda(Venda venda, List<ItemVenda> itens, List<Pagamento> pagamentos) throws SQLException {
        // ATUALIZADO: Incluindo a coluna 'desconto'
        String sqlVenda = "INSERT INTO vendas (id, data_hora, valor_total, forma_pagamento, usuario_id, caixa_id, desconto) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlItem = "INSERT INTO itens_venda (venda_id, produto_id, quantidade, valor_unitario, total_item, custo_unitario) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlPagamento = "INSERT INTO pagamentos_venda (venda_id, tipo, valor) VALUES (?, ?, ?)";
        String sqlUpdateEstoque = "UPDATE produtos SET estoque = estoque - ? WHERE id = ?";

        Connection conn = null;
        int vendaIdParaSalvar = 0;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // TRAVA O BANCO (Início da Transação)

            if (venda.getId() != null && venda.getId() > 0) {
                vendaIdParaSalvar = venda.getId();
                restaurarEstoqueEncomenda(conn, vendaIdParaSalvar);
                excluirEncomendaInterno(conn, vendaIdParaSalvar);
            } else {
                vendaIdParaSalvar = getProximoIdVenda(conn);
            }

            // Busca caixa usando a MESMA conexão para evitar erro de Snapshot
            Caixa caixaAberto = caixaDAO.buscarCaixaAberto(conn);
            int caixaId = (caixaAberto != null) ? caixaAberto.getId() : 0;

            // A. Inserir Cabeçalho da Venda
            try (PreparedStatement pstmtVenda = conn.prepareStatement(sqlVenda)) {
                pstmtVenda.setInt(1, vendaIdParaSalvar);
                pstmtVenda.setString(2, venda.getDataHora().toString());
                pstmtVenda.setDouble(3, venda.getValorTotal()); // Valor Líquido (Já com desconto aplicado)
                pstmtVenda.setString(4, venda.getFormaPagamento());
                pstmtVenda.setInt(5, 1); // Usuário (pode ajustar depois para pegar da Sessao)
                pstmtVenda.setInt(6, caixaId);
                // Salva o desconto (Se for null, salva 0.0)
                pstmtVenda.setDouble(7, venda.getDesconto() != null ? venda.getDesconto() : 0.0);

                pstmtVenda.executeUpdate();
            }

            // B. Inserir Itens
            try (PreparedStatement pstmtItem = conn.prepareStatement(sqlItem)) {
                for (ItemVenda item : itens) {
                    pstmtItem.setInt(1, vendaIdParaSalvar);
                    pstmtItem.setInt(2, item.getProduto().getId());
                    pstmtItem.setDouble(3, item.getQuantidade());
                    pstmtItem.setDouble(4, item.getPrecoUnitario());
                    pstmtItem.setDouble(5, item.getTotalItem());
                    pstmtItem.setDouble(6, item.getCustoUnitario());
                    pstmtItem.addBatch();
                }
                pstmtItem.executeBatch();
            }

            // C. Inserir Pagamentos
            try (PreparedStatement pstmtPagamento = conn.prepareStatement(sqlPagamento)) {
                for (Pagamento pagamento : pagamentos) {
                    pstmtPagamento.setInt(1, vendaIdParaSalvar);
                    pstmtPagamento.setString(2, pagamento.getTipo());
                    pstmtPagamento.setDouble(3, pagamento.getValor());
                    pstmtPagamento.addBatch();
                }
                pstmtPagamento.executeBatch();
            }

            // D. Baixar Estoque
            try (PreparedStatement pstmtUpdateEstoque = conn.prepareStatement(sqlUpdateEstoque)) {
                for (ItemVenda item : itens) {
                    pstmtUpdateEstoque.setDouble(1, item.getQuantidade());
                    pstmtUpdateEstoque.setInt(2, item.getProduto().getId());
                    pstmtUpdateEstoque.addBatch();
                }
                pstmtUpdateEstoque.executeBatch();
            }

            conn.commit(); // DESTRAVA O BANCO (Sucesso)

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { } }
            throw e;
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) { } }
        }
        return vendaIdParaSalvar;
    }

    // --- ENCOMENDAS ---
    public void salvarEncomenda(Encomenda enc) throws SQLException {
        if (existeEncomenda(enc.getId())) {
            atualizarEncomendaCabecalho(enc);
        } else {
            inserirNovaEncomenda(enc);
        }
    }

    private boolean existeEncomenda(Integer id) {
        if (id == null) return false;
        String sql = "SELECT 1 FROM encomendas WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    private void inserirNovaEncomenda(Encomenda enc) throws SQLException {
        Connection conn = null;
        String sqlUpdateEstoque = "UPDATE produtos SET estoque = estoque - ? WHERE id = ?";

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            if (enc.getId() == null || enc.getId() == 0) {
                enc.setId(getProximoIdVenda(conn));
            }

            String sqlEnc = "INSERT INTO encomendas (id, nome_cliente, data_retirada, valor_total, status) VALUES (?, ?, ?, ?, ?)";
            String sqlItem = "INSERT INTO itens_encomenda (encomenda_id, produto_id, quantidade, valor_unitario, total_item) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sqlEnc)) {
                pstmt.setInt(1, enc.getId());
                pstmt.setString(2, enc.getNomeCliente());
                pstmt.setString(3, (enc.getDataRetirada() != null) ? enc.getDataRetirada().toString() : LocalDateTime.now().toString());
                pstmt.setDouble(4, enc.getValorTotal());
                pstmt.setString(5, "PENDENTE");
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmtItem = conn.prepareStatement(sqlItem)) {
                for (ItemVenda item : enc.getItens()) {
                    pstmtItem.setInt(1, enc.getId());
                    pstmtItem.setInt(2, item.getProduto().getId());
                    pstmtItem.setDouble(3, item.getQuantidade());
                    pstmtItem.setDouble(4, item.getPrecoUnitario());
                    pstmtItem.setDouble(5, item.getTotalItem());
                    pstmtItem.addBatch();
                }
                pstmtItem.executeBatch();
            }

            try (PreparedStatement pstmtEstoque = conn.prepareStatement(sqlUpdateEstoque)) {
                for (ItemVenda item : enc.getItens()) {
                    pstmtEstoque.setDouble(1, item.getQuantidade());
                    pstmtEstoque.setInt(2, item.getProduto().getId());
                    pstmtEstoque.addBatch();
                }
                pstmtEstoque.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { }
            throw e;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }
    }

    private void atualizarEncomendaCabecalho(Encomenda enc) throws SQLException {
        String sqlUpdate = "UPDATE encomendas SET nome_cliente=?, valor_total=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
            pstmt.setString(1, enc.getNomeCliente());
            pstmt.setDouble(2, enc.getValorTotal());
            pstmt.setInt(3, enc.getId());
            pstmt.executeUpdate();
        }
    }

    public void cancelarEncomenda(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            restaurarEstoqueEncomenda(conn, id);
            excluirEncomendaInterno(conn, id);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { }
            e.printStackTrace();
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }
    }

    private void restaurarEstoqueEncomenda(Connection conn, int encomendaId) throws SQLException {
        String sqlBusca = "SELECT produto_id, quantidade FROM itens_encomenda WHERE encomenda_id = ?";
        String sqlUpdate = "UPDATE produtos SET estoque = estoque + ? WHERE id = ?";
        List<ItemVenda> itensParaDevolver = new ArrayList<>();

        try (PreparedStatement pstmtBusca = conn.prepareStatement(sqlBusca)) {
            pstmtBusca.setInt(1, encomendaId);
            ResultSet rs = pstmtBusca.executeQuery();
            while(rs.next()){
                ItemVenda i = new ItemVenda();
                Produto p = new Produto();
                p.setId(rs.getInt("produto_id"));
                i.setProduto(p);
                i.setQuantidade(rs.getDouble("quantidade"));
                itensParaDevolver.add(i);
            }
        }

        try (PreparedStatement pstmtUp = conn.prepareStatement(sqlUpdate)) {
            for(ItemVenda item : itensParaDevolver){
                pstmtUp.setDouble(1, item.getQuantidade());
                pstmtUp.setInt(2, item.getProduto().getId());
                pstmtUp.addBatch();
            }
            pstmtUp.executeBatch();
        }
    }

    private void excluirEncomendaInterno(Connection conn, int id) throws SQLException {
        String sqlItens = "DELETE FROM itens_encomenda WHERE encomenda_id = ?";
        String sqlEnc = "DELETE FROM encomendas WHERE id = ?";
        try (PreparedStatement p1 = conn.prepareStatement(sqlItens)) {
            p1.setInt(1, id);
            p1.executeUpdate();
        }
        try (PreparedStatement p2 = conn.prepareStatement(sqlEnc)) {
            p2.setInt(1, id);
            p2.executeUpdate();
        }
    }

    // --- LEITURAS ---
    public List<Encomenda> buscarTodasEncomendasPendentes() {
        List<Encomenda> lista = new ArrayList<>();
        String sql = "SELECT * FROM encomendas WHERE status = 'PENDENTE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Encomenda enc = new Encomenda();
                enc.setId(rs.getInt("id"));
                enc.setNomeCliente(rs.getString("nome_cliente"));
                String dt = rs.getString("data_retirada");
                if (dt != null) enc.setDataRetirada(LocalDateTime.parse(dt));
                enc.setValorTotal(rs.getDouble("valor_total"));
                enc.setStatus(rs.getString("status"));
                enc.setItens(buscarItensPorEncomenda(enc.getId()));
                lista.add(enc);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    private List<ItemVenda> buscarItensPorEncomenda(int encomendaId) {
        String sql = "SELECT i.*, p.nome, p.unidade, p.preco_venda FROM itens_encomenda i JOIN produtos p ON i.produto_id = p.id WHERE i.encomenda_id = ?";
        List<ItemVenda> itens = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, encomendaId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                Produto p = new Produto();
                p.setId(rs.getInt("produto_id"));
                p.setNome(rs.getString("nome"));
                p.setUnidade(rs.getString("unidade"));
                p.setPrecoVenda(rs.getDouble("preco_venda"));
                ItemVenda item = new ItemVenda();
                item.setProduto(p);
                item.setQuantidade(rs.getDouble("quantidade"));
                item.setPrecoUnitario(rs.getDouble("valor_unitario"));
                item.setTotalItem(rs.getDouble("total_item"));
                itens.add(item);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return itens;
    }

    public int getProximoIdVenda() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getProximoIdVenda(conn);
        } catch (SQLException e) { return 1; }
    }

    private int getProximoIdVenda(Connection conn) {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 AS proximo_id FROM (SELECT id FROM vendas UNION ALL SELECT id FROM encomendas)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt("proximo_id");
        } catch (SQLException e) { e.printStackTrace(); }
        return 1;
    }

    public double buscarTotalPorTipo(java.time.LocalDate data, String tipoPagamento) {
        String sql = "SELECT SUM(p.valor) as total FROM pagamentos_venda p INNER JOIN vendas v ON p.venda_id = v.id WHERE date(v.data_hora) = date(?) AND p.tipo = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.toString());
            pstmt.setString(2, tipoPagamento);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    // ATUALIZADO: Traz o campo 'desconto' do banco
    public List<Venda> buscarVendasDetalhadasPorData(java.time.LocalDate data) {
        String sql = """
            SELECT 
                v.id, v.data_hora, v.valor_total, v.forma_pagamento, v.desconto,
                (SELECT SUM(i.custo_unitario * i.quantidade) FROM itens_venda i WHERE i.venda_id = v.id) as val_custo,
                SUM(CASE WHEN p.tipo = 'DINHEIRO' THEN p.valor ELSE 0 END) as val_dinheiro, 
                SUM(CASE WHEN p.tipo = 'DÉBITO' THEN p.valor ELSE 0 END) as val_debito, 
                SUM(CASE WHEN p.tipo = 'CRÉDITO' THEN p.valor ELSE 0 END) as val_credito, 
                SUM(CASE WHEN p.tipo = 'PIX' THEN p.valor ELSE 0 END) as val_pix 
            FROM vendas v 
            LEFT JOIN pagamentos_venda p ON v.id = p.venda_id 
            WHERE date(v.data_hora) = date(?) 
            GROUP BY v.id 
            ORDER BY v.data_hora DESC
        """;
        List<Venda> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Venda v = mapVendaDetalhada(rs);
                v.setValorCusto(rs.getDouble("val_custo"));
                lista.add(v);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    // ATUALIZADO: Traz o campo 'desconto' do banco
    public List<Venda> buscarVendasDetalhadasPorCaixa(int caixaId) {
        String sql = """
            SELECT 
                v.id, v.data_hora, v.valor_total, v.forma_pagamento, v.desconto,
                (SELECT SUM(i.custo_unitario * i.quantidade) FROM itens_venda i WHERE i.venda_id = v.id) as val_custo,
                SUM(CASE WHEN p.tipo = 'DINHEIRO' THEN p.valor ELSE 0 END) as val_dinheiro, 
                SUM(CASE WHEN p.tipo = 'DÉBITO' THEN p.valor ELSE 0 END) as val_debito, 
                SUM(CASE WHEN p.tipo = 'CRÉDITO' THEN p.valor ELSE 0 END) as val_credito, 
                SUM(CASE WHEN p.tipo = 'PIX' THEN p.valor ELSE 0 END) as val_pix 
            FROM vendas v 
            LEFT JOIN pagamentos_venda p ON v.id = p.venda_id 
            WHERE v.caixa_id = ? 
            GROUP BY v.id 
            ORDER BY v.data_hora DESC
        """;
        List<Venda> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, caixaId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Venda v = mapVendaDetalhada(rs);
                v.setValorCusto(rs.getDouble("val_custo"));
                lista.add(v);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    private Venda mapVendaDetalhada(ResultSet rs) throws SQLException {
        Venda v = new Venda();
        v.setId(rs.getInt("id"));
        v.setDataHora(java.time.LocalDateTime.parse(rs.getString("data_hora")));
        v.setValorTotal(rs.getDouble("valor_total"));
        v.setFormaPagamento(rs.getString("forma_pagamento"));

        // Mapeia o desconto (se a coluna existir)
        try { v.setDesconto(rs.getDouble("desconto")); } catch (Exception e) { v.setDesconto(0.0); }

        v.setValorDinheiro(rs.getDouble("val_dinheiro"));
        v.setValorDebito(rs.getDouble("val_debito"));
        v.setValorCredito(rs.getDouble("val_credito"));
        v.setValorPix(rs.getDouble("val_pix"));
        return v;
    }

    public List<ItemVenda> buscarItensPorVenda(int vendaId) {
        String sql = "SELECT i.*, p.nome, p.unidade, p.preco_venda FROM itens_venda i JOIN produtos p ON i.produto_id = p.id WHERE i.venda_id = ?";
        List<ItemVenda> itens = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vendaId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                Produto p = new Produto();
                p.setId(rs.getInt("produto_id"));
                p.setNome(rs.getString("nome"));
                p.setUnidade(rs.getString("unidade"));
                p.setPrecoVenda(rs.getDouble("preco_venda"));
                ItemVenda item = new ItemVenda();
                item.setProduto(p);
                item.setQuantidade(rs.getDouble("quantidade"));
                item.setPrecoUnitario(rs.getDouble("valor_unitario"));
                try { item.setCustoUnitario(rs.getDouble("custo_unitario")); } catch (SQLException ex) { item.setCustoUnitario(0.0); }
                item.setTotalItem(rs.getDouble("total_item"));
                itens.add(item);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return itens;
    }

    public List<Pagamento> buscarPagamentosPorVenda(int vendaId) {
        String sql = "SELECT * FROM pagamentos_venda WHERE venda_id = ?";
        List<Pagamento> pagamentos = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vendaId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                pagamentos.add(new Pagamento(rs.getString("tipo"), rs.getDouble("valor")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return pagamentos;
    }
}