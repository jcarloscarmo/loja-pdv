package br.com.churrasco.dao;

import br.com.churrasco.model.Caixa;
import br.com.churrasco.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;

public class CaixaDAO {

    // Verifica se existe algum caixa com status 'ABERTO'
    public Caixa buscarCaixaAberto() {
        String sql = "SELECT * FROM caixas WHERE status = 'ABERTO' ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                Caixa c = new Caixa();
                c.setId(rs.getInt("id"));
                c.setSaldoInicial(rs.getDouble("saldo_inicial"));
                c.setDataAbertura(LocalDateTime.parse(rs.getString("data_abertura")));
                c.setStatus(rs.getString("status"));
                return c;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void abrirCaixa(double saldoInicial) throws SQLException {
        String sql = "INSERT INTO caixas (usuario_id, data_abertura, saldo_inicial, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, 1); // Usuário Admin fixo por enquanto
            pstmt.setString(2, LocalDateTime.now().toString());
            pstmt.setDouble(3, saldoInicial);
            pstmt.setString(4, "ABERTO");
            pstmt.executeUpdate();
        }
    }

    public void fecharCaixa(int id, double saldoSistema, double saldoInformado, double diferenca) throws SQLException {
        String sql = "UPDATE caixas SET data_fechamento = ?, saldo_final = ?, saldo_informado = ?, diferenca = ?, status = 'FECHADO' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, LocalDateTime.now().toString());
            pstmt.setDouble(2, saldoSistema);
            pstmt.setDouble(3, saldoInformado);
            pstmt.setDouble(4, diferenca);
            pstmt.setInt(5, id);
            pstmt.executeUpdate();
        }
    }

    // Calcula quanto deveria ter em DINHEIRO (Saldo Inicial + Vendas em Dinheiro - Sangrias)
    // Nota: Por enquanto não temos sangria, então é Saldo Inicial + Vendas Dinheiro
    public double calcularSaldoDinheiroSistema(int caixaId) {
        String sql = """
            SELECT SUM(p.valor) as total 
            FROM pagamentos_venda p
            JOIN vendas v ON p.venda_id = v.id
            WHERE v.caixa_id = ? AND p.tipo = 'DINHEIRO'
        """;

        double totalVendasDinheiro = 0.0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, caixaId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) totalVendasDinheiro = rs.getDouble("total");
        } catch (SQLException e) { e.printStackTrace(); }

        return totalVendasDinheiro;
    }

    // Método para listar o histórico completo
    public java.util.List<Caixa> listarHistorico() {
        String sql = "SELECT * FROM caixas ORDER BY id DESC"; // Do mais novo pro mais velho
        java.util.List<Caixa> lista = new java.util.ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Caixa c = new Caixa();
                c.setId(rs.getInt("id"));
                c.setUsuarioId(rs.getInt("usuario_id"));

                // Tratamento de datas (pode ser null se o caixa ainda estiver aberto)
                String dtAbertura = rs.getString("data_abertura");
                if(dtAbertura != null) c.setDataAbertura(LocalDateTime.parse(dtAbertura));

                String dtFechamento = rs.getString("data_fechamento");
                if(dtFechamento != null) c.setDataFechamento(LocalDateTime.parse(dtFechamento));

                c.setSaldoInicial(rs.getDouble("saldo_inicial"));
                c.setSaldoFinal(rs.getDouble("saldo_final"));
                c.setSaldoInformado(rs.getDouble("saldo_informado"));
                c.setDiferenca(rs.getDouble("diferenca"));
                c.setStatus(rs.getString("status"));

                lista.add(c);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
}