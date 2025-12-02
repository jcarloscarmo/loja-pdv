package br.com.churrasco.dao;

import br.com.churrasco.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ConfigDAO {

    // Busca o valor de uma configuração
    public Optional<String> getValor(String chave) {
        String sql = "SELECT valor FROM configuracoes WHERE chave = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, chave);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.ofNullable(rs.getString("valor"));
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Use um Logger real em produção
        }
        return Optional.empty();
    }

    // Salva ou Atualiza (Upsert simplificado para SQLite)
    public void salvar(String chave, String valor) {
        String sql = "INSERT OR REPLACE INTO configuracoes (chave, valor) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, chave);
            stmt.setString(2, valor);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao salvar configuração: " + chave);
        }
    }
}