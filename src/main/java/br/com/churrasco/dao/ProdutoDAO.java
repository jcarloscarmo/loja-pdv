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
        String sql = "SELECT * FROM produtos";
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

    // Adicione este método na classe ProdutoDAO
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
            pstmt.setInt(7, p.getId()); // O ID é usado no WHERE para saber qual atualizar

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erro ao atualizar: " + e.getMessage());
        }
    }
}