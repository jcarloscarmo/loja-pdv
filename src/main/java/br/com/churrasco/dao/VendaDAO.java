package br.com.churrasco.dao;

import br.com.churrasco.model.Caixa; // Importante
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import br.com.churrasco.model.Produto;
import br.com.churrasco.model.Venda;
import br.com.churrasco.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VendaDAO {

    private CaixaDAO caixaDAO = new CaixaDAO(); // Necessário para vincular a venda ao caixa

    // --- 1. SALVAR VENDA (Com Vínculo de Caixa) ---
    public int salvarVenda(Venda venda, List<ItemVenda> itens, List<Pagamento> pagamentos) throws SQLException {

        // SQL atualizado com caixa_id
        String sqlVenda = "INSERT INTO vendas (data_hora, valor_total, forma_pagamento, usuario_id, caixa_id) VALUES (?, ?, ?, ?, ?)";

        String sqlItem = "INSERT INTO itens_venda (venda_id, produto_id, quantidade, valor_unitario, total_item) VALUES (?, ?, ?, ?, ?)";
        String sqlPagamento = "INSERT INTO pagamentos_venda (venda_id, tipo, valor) VALUES (?, ?, ?)";
        String sqlUpdateEstoque = "UPDATE produtos SET estoque = estoque - ? WHERE id = ?";

        Connection conn = null;
        int vendaIdGerado = 0;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Inicia a transação

            // 0. Descobrir qual Caixa está aberto
            Caixa caixaAberto = caixaDAO.buscarCaixaAberto();
            int caixaId = (caixaAberto != null) ? caixaAberto.getId() : 0; // Se não tiver caixa, salva como 0

            // A. Salvar Cabeçalho da Venda
            try (PreparedStatement pstmtVenda = conn.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                pstmtVenda.setString(1, venda.getDataHora().toString());
                pstmtVenda.setDouble(2, venda.getValorTotal());
                pstmtVenda.setString(3, venda.getFormaPagamento());
                pstmtVenda.setInt(4, 1); // Usuário Admin (Fixo)
                pstmtVenda.setInt(5, caixaId); // <--- Vínculo com a Sessão

                pstmtVenda.executeUpdate();

                try (ResultSet generatedKeys = pstmtVenda.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        vendaIdGerado = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Falha ao obter o ID da venda.");
                    }
                }
            }

            // B. Salvar Itens
            try (PreparedStatement pstmtItem = conn.prepareStatement(sqlItem)) {
                for (ItemVenda item : itens) {
                    pstmtItem.setInt(1, vendaIdGerado);
                    pstmtItem.setInt(2, item.getProduto().getId());
                    pstmtItem.setDouble(3, item.getQuantidade());
                    pstmtItem.setDouble(4, item.getPrecoUnitario());
                    pstmtItem.setDouble(5, item.getTotalItem());
                    pstmtItem.addBatch();
                }
                pstmtItem.executeBatch();
            }

            // C. Salvar Pagamentos
            try (PreparedStatement pstmtPagamento = conn.prepareStatement(sqlPagamento)) {
                for (Pagamento pagamento : pagamentos) {
                    pstmtPagamento.setInt(1, vendaIdGerado);
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

            conn.commit(); // Confirma tudo

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return vendaIdGerado;
    }

    // --- 2. AUXILIARES DO PDV ---
    public int getProximoIdVenda() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 AS proximo_id FROM vendas";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt("proximo_id");
        } catch (SQLException e) { e.printStackTrace(); }
        return 1;
    }

    // --- 3. RELATÓRIOS (Cards de Totais) ---
    public double buscarTotalPorTipo(java.time.LocalDate data, String tipoPagamento) {
        String sql = """
            SELECT SUM(p.valor) as total 
            FROM pagamentos_venda p
            INNER JOIN vendas v ON p.venda_id = v.id
            WHERE date(v.data_hora) = date(?) AND p.tipo = ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.toString());
            pstmt.setString(2, tipoPagamento);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    // --- 4. RELATÓRIOS (Tabela Analítica Detalhada) ---
    public List<Venda> buscarVendasDetalhadasPorData(java.time.LocalDate data) {
        String sql = """
            SELECT 
                v.id, v.data_hora, v.valor_total, v.forma_pagamento,
                SUM(CASE WHEN p.tipo = 'DINHEIRO' THEN p.valor ELSE 0 END) as val_dinheiro,
                SUM(CASE WHEN p.tipo = 'DÉBITO'   THEN p.valor ELSE 0 END) as val_debito,
                SUM(CASE WHEN p.tipo = 'CRÉDITO'  THEN p.valor ELSE 0 END) as val_credito,
                SUM(CASE WHEN p.tipo = 'PIX'      THEN p.valor ELSE 0 END) as val_pix
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
                Venda v = new Venda();
                v.setId(rs.getInt("id"));
                v.setDataHora(java.time.LocalDateTime.parse(rs.getString("data_hora")));
                v.setValorTotal(rs.getDouble("valor_total"));
                v.setFormaPagamento(rs.getString("forma_pagamento"));

                v.setValorDinheiro(rs.getDouble("val_dinheiro"));
                v.setValorDebito(rs.getDouble("val_debito"));
                v.setValorCredito(rs.getDouble("val_credito"));
                v.setValorPix(rs.getDouble("val_pix"));

                lista.add(v);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    // --- 5. DETALHES PARA O RECIBO ---
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
    // Método novo: Busca vendas detalhadas de um CAIXA ESPECÍFICO
    public List<Venda> buscarVendasDetalhadasPorCaixa(int caixaId) {
        String sql = """
            SELECT 
                v.id, v.data_hora, v.valor_total, v.forma_pagamento,
                SUM(CASE WHEN p.tipo = 'DINHEIRO' THEN p.valor ELSE 0 END) as val_dinheiro,
                SUM(CASE WHEN p.tipo = 'DÉBITO'   THEN p.valor ELSE 0 END) as val_debito,
                SUM(CASE WHEN p.tipo = 'CRÉDITO'  THEN p.valor ELSE 0 END) as val_credito,
                SUM(CASE WHEN p.tipo = 'PIX'      THEN p.valor ELSE 0 END) as val_pix
            FROM vendas v
            LEFT JOIN pagamentos_venda p ON v.id = p.venda_id
            WHERE v.caixa_id = ? 
            GROUP BY v.id
            ORDER BY v.data_hora DESC
        """;

        List<Venda> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, caixaId); // Filtra pelo ID do caixa clicado
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Venda v = new Venda();
                v.setId(rs.getInt("id"));
                v.setDataHora(java.time.LocalDateTime.parse(rs.getString("data_hora")));
                v.setValorTotal(rs.getDouble("valor_total"));
                v.setFormaPagamento(rs.getString("forma_pagamento"));

                v.setValorDinheiro(rs.getDouble("val_dinheiro"));
                v.setValorDebito(rs.getDouble("val_debito"));
                v.setValorCredito(rs.getDouble("val_credito"));
                v.setValorPix(rs.getDouble("val_pix"));

                lista.add(v);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
}