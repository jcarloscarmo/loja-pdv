package br.com.churrasco.dao;

import br.com.churrasco.model.Encomenda;
import br.com.churrasco.model.ItemEncomenda;
import br.com.churrasco.model.Produto;
import br.com.churrasco.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EncomendaDAO {

    // --- SALVAR NOVA ENCOMENDA ---
    public int salvar(Encomenda encomenda, List<ItemEncomenda> itens) {
        String sqlEncomenda = "INSERT INTO encomendas (nome_cliente, data_retirada, valor_total, status) VALUES (?, ?, ?, ?)";
        String sqlItem = "INSERT INTO itens_encomenda (encomenda_id, produto_id, quantidade, valor_unitario, total_item) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        int idGerado = 0;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Transação

            // 1. Salva o Cabeçalho
            try (PreparedStatement pstmt = conn.prepareStatement(sqlEncomenda, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, encomenda.getNomeCliente());
                pstmt.setString(2, encomenda.getDataRetirada().toString());
                pstmt.setDouble(3, encomenda.getValorTotal());
                pstmt.setString(4, "PENDENTE");
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) idGerado = rs.getInt(1);
                }
            }

            // 2. Salva os Itens
            try (PreparedStatement pstmt = conn.prepareStatement(sqlItem)) {
                for (ItemEncomenda item : itens) {
                    pstmt.setInt(1, idGerado);
                    pstmt.setInt(2, item.getProduto().getId());
                    pstmt.setDouble(3, item.getQuantidade());
                    pstmt.setDouble(4, item.getValorUnitario());
                    pstmt.setDouble(5, item.getTotalItem());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit();

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); /* conn.close(); */ } } catch (SQLException e) { e.printStackTrace(); }
        }
        return idGerado;
    }

    // --- LISTAR PENDENTES (Para a faixa de cards) ---
    public List<Encomenda> listarPendentes() {
        String sql = "SELECT * FROM encomendas WHERE status = 'PENDENTE' ORDER BY data_retirada ASC";
        List<Encomenda> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Encomenda e = new Encomenda();
                e.setId(rs.getInt("id"));
                e.setNomeCliente(rs.getString("nome_cliente"));
                e.setDataRetirada(LocalDateTime.parse(rs.getString("data_retirada")));
                e.setValorTotal(rs.getDouble("valor_total"));
                e.setStatus(rs.getString("status"));
                lista.add(e);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    // --- BUSCAR ITENS (Para carregar no PDV) ---
    public List<ItemEncomenda> buscarItens(int encomendaId) {
        String sql = "SELECT i.*, p.codigo, p.nome, p.unidade, p.preco_venda FROM itens_encomenda i JOIN produtos p ON i.produto_id = p.id WHERE i.encomenda_id = ?";
        List<ItemEncomenda> itens = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, encomendaId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Produto p = new Produto();
                p.setId(rs.getInt("produto_id"));
                p.setCodigo(rs.getString("codigo"));
                p.setNome(rs.getString("nome"));
                p.setUnidade(rs.getString("unidade"));
                p.setPrecoVenda(rs.getDouble("preco_venda")); // Preço atual

                ItemEncomenda item = new ItemEncomenda();
                item.setId(rs.getInt("id"));
                item.setEncomendaId(encomendaId);
                item.setProduto(p);
                item.setQuantidade(rs.getDouble("quantidade"));
                item.setValorUnitario(rs.getDouble("valor_unitario")); // Preço acordado na encomenda
                // O totalItem é recalculado no setQuantidade ou construtor, mas podemos forçar se quisermos manter o histórico exato
                // item.setTotalItem(rs.getDouble("total_item"));

                // Recalcula para garantir consistência
                item.setQuantidade(item.getQuantidade());

                itens.add(item);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return itens;
    }

    // --- FINALIZAR OU CANCELAR ---
    public void atualizarStatus(int id, String novoStatus) {
        String sql = "UPDATE encomendas SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, novoStatus);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}