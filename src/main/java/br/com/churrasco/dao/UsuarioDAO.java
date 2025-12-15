package br.com.churrasco.dao;

import br.com.churrasco.model.Usuario;
import br.com.churrasco.util.DatabaseConnection;
import br.com.churrasco.util.Sessao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    // 1. LISTAR NOMES (Para o Login e Combobox do Admin)
    public List<String> listarNomes() {
        List<String> nomes = new ArrayList<>();
        String sql = "SELECT nome FROM usuarios ORDER BY nome";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                nomes.add(rs.getString("nome"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nomes;
    }

    // 2. AUTENTICAÇÃO PADRÃO
    public Usuario autenticar(String nome, String senha) {
        String sql = "SELECT * FROM usuarios WHERE nome = ? AND senha = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            stmt.setString(2, senha);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Usuario(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("senha"),
                        rs.getString("perfil")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- AQUI ESTÁ A MUDANÇA PARA DESCOBRIR O ERRO ---
    public boolean validarAdmin(String nome, String senha) {
        System.out.println("--- INICIANDO VALIDAÇÃO DE ADMIN ---");
        System.out.println("Tentando validar usuário: " + nome);

        Usuario u = autenticar(nome, senha);

        if (u == null) {
            System.out.println("ERRO: Usuário não encontrado ou senha incorreta.");
            return false;
        }

        System.out.println("SUCESSO: Senha correta.");
        System.out.println("Perfil carregado do banco: " + u.getPerfil());

        boolean ehAdmin = u.isAdmin();
        System.out.println("O sistema considera Admin? " + (ehAdmin ? "SIM" : "NÃO"));

        if (!ehAdmin) {
            System.out.println("MOTIVO DA RECUSA: O perfil '" + u.getPerfil() + "' não está na lista de permitidos (ADMIN, DONO, ADMINISTRADOR).");
        }
        System.out.println("------------------------------------");

        return ehAdmin;
    }
    // -----------------------------------------------------

    // 3. SALVAR NOVO
    public void salvar(Usuario u) throws Exception {
        String sql = "INSERT INTO usuarios (nome, senha, perfil) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, u.getNome());
            stmt.setString(2, u.getSenha());
            stmt.setString(3, u.getPerfil());
            stmt.executeUpdate();
        }
    }

    // 4. ATUALIZAR
    public void atualizar(Usuario u) throws Exception {
        String sql = "UPDATE usuarios SET nome = ?, senha = ?, perfil = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, u.getNome());
            stmt.setString(2, u.getSenha());
            stmt.setString(3, u.getPerfil());
            stmt.setInt(4, u.getId());
            stmt.executeUpdate();
        }
    }

    // 5. EXCLUIR
    public void excluir(Integer id) throws Exception {
        Usuario logado = Sessao.getUsuario();
        if (logado != null && logado.getId().equals(id)) {
            throw new Exception("Você não pode excluir o seu próprio usuário enquanto está logado.\nCrie outro ADM, logue com ele e depois exclua este.");
        }

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement stmtUpdate = conn.prepareStatement("UPDATE vendas SET usuario_id = NULL WHERE usuario_id = ?")) {
                stmtUpdate.setInt(1, id);
                stmtUpdate.executeUpdate();
            } catch (Exception e) {}

            try (PreparedStatement stmtDelete = conn.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
                stmtDelete.setInt(1, id);
                int afetados = stmtDelete.executeUpdate();
                if (afetados == 0) throw new Exception("Usuário não encontrado.");
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            if (e.getMessage().contains("FOREIGN KEY")) throw new Exception("Erro de vínculo. Tente renomear o usuário.");
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    // 6. LISTAR TODOS
    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Usuario(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("senha"),
                        rs.getString("perfil")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}