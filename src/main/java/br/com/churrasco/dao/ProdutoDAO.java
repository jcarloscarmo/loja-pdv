package br.com.churrasco.dao;

import br.com.churrasco.model.Produto;
import br.com.churrasco.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    public void salvar(Produto p) {
        String sql = "INSERT INTO produtos (codigo, nome, preco_custo, preco_venda, unidade, estoque) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getCodigo());
            pstmt.setString(2, p.getNome());
            pstmt.setDouble(3, p.getPrecoCusto());
            pstmt.setDouble(4, p.getPrecoVenda());
            pstmt.setString(5, p.getUnidade());
            pstmt.setDouble(6, p.getEstoque());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<Produto> listarTodos() {
        // Ordena por código para ficar bonito na tela
        String sql = "SELECT * FROM produtos ORDER BY CAST(codigo AS UNSIGNED) ASC";
        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Produto produto = new Produto();
                produto.setId(rs.getInt("id"));
                produto.setCodigo(rs.getString("codigo"));
                produto.setNome(rs.getString("nome"));
                produto.setPrecoCusto(rs.getDouble("preco_custo"));
                produto.setPrecoVenda(rs.getDouble("preco_venda"));
                produto.setUnidade(rs.getString("unidade"));
                produto.setEstoque(rs.getDouble("estoque"));
                produtos.add(produto);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return produtos;
    }

    public Produto buscarPorCodigo(String codigo) {
        String sql = "SELECT * FROM produtos WHERE codigo = ?";
        Produto produto = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codigo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    produto = new Produto();
                    produto.setId(rs.getInt("id"));
                    produto.setCodigo(rs.getString("codigo"));
                    produto.setNome(rs.getString("nome"));
                    produto.setPrecoCusto(rs.getDouble("preco_custo"));
                    produto.setPrecoVenda(rs.getDouble("preco_venda"));
                    produto.setUnidade(rs.getString("unidade"));
                    produto.setEstoque(rs.getDouble("estoque"));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return produto;
    }

    public void deletar(Integer id) {
        String sql = "DELETE FROM produtos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void atualizar(Produto p) {
        String sql = "UPDATE produtos SET codigo = ?, nome = ?, preco_custo = ?, preco_venda = ?, unidade = ?, estoque = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getCodigo());
            pstmt.setString(2, p.getNome());
            pstmt.setDouble(3, p.getPrecoCusto());
            pstmt.setDouble(4, p.getPrecoVenda());
            pstmt.setString(5, p.getUnidade());
            pstmt.setDouble(6, p.getEstoque());
            pstmt.setInt(7, p.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erro ao atualizar: " + e.getMessage());
        }
    }

    // --- NOVO MÉTODO: GERA O PRÓXIMO CÓDIGO SEQUENCIAL ---
    public String buscarProximoCodigoDisponivel() {
        // Converte o código para número para achar o maior (evita que 10 venha antes de 2)
        String sql = "SELECT MAX(CAST(codigo AS UNSIGNED)) as max_cod FROM produtos";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                int max = rs.getInt("max_cod");
                return String.valueOf(max + 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "1"; // Se não tiver nada, começa do 1
    }
}