package br.com.churrasco.dao;

import br.com.churrasco.model.Caixa;
import br.com.churrasco.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CaixaDAO {

    // --- BUSCA PADRÃO (SEGURA) ---
    public Caixa buscarCaixaAberto() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return buscarCaixaAberto(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Caixa buscarCaixaAberto(Connection conn) throws SQLException {
        String sql = "SELECT * FROM caixas WHERE status = 'ABERTO' ORDER BY id DESC LIMIT 1";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return mapearCaixa(rs);
            }
        }
        return null;
    }

    // --- NOVA ESTRATÉGIA: RECUPERAR ÚLTIMO CAIXA (SE O PADRÃO FALHAR) ---
    public Caixa buscarUltimoCaixaQualquerStatus() {
        String sql = "SELECT * FROM caixas ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return mapearCaixa(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // --- FECHAMENTO AUTOMÁTICO E CORRETIVO ---
    public void verificarEFecharCaixasAntigos() {
        Caixa aberto = buscarCaixaAberto();

        // Se achou um caixa aberto com data anterior a hoje -> FECHA
        if (aberto != null && aberto.getDataAbertura().toLocalDate().isBefore(LocalDate.now())) {
            System.out.println("Caixa antigo detectado. Fechando compulsoriamente...");
            forcarFechamento(aberto);
        }
        // Se NÃO achou caixa aberto, mas o último registro está travado como ABERTO -> FECHA TAMBÉM
        else if (aberto == null) {
            Caixa ultimo = buscarUltimoCaixaQualquerStatus();
            if (ultimo != null && "ABERTO".equalsIgnoreCase(ultimo.getStatus()) && ultimo.getDataAbertura().toLocalDate().isBefore(LocalDate.now())) {
                System.out.println("Caixa órfão detectado. Fechando compulsoriamente...");
                forcarFechamento(ultimo);
            }
        }
    }

    private void forcarFechamento(Caixa c) {
        try {
            double saldoDinheiro = calcularSaldoDinheiroSistema(c.getId());
            double saldoFinalEsperado = c.getSaldoInicial() + saldoDinheiro;
            fecharCaixa(c.getId(), saldoFinalEsperado, saldoFinalEsperado, 0.0);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- MÉTODOS CRUD ---
    public void abrirCaixa(double saldoInicial) throws SQLException {
        verificarEFecharCaixasAntigos(); // Garante limpeza antes de abrir
        String sql = "INSERT INTO caixas (usuario_id, data_abertura, saldo_inicial, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, 1);
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

    public double calcularSaldoDinheiroSistema(int caixaId) {
        String sql = "SELECT SUM(p.valor) as total FROM pagamentos_venda p JOIN vendas v ON p.venda_id = v.id WHERE v.caixa_id = ? AND p.tipo = 'DINHEIRO'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, caixaId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) { }
        return 0.0;
    }

    // Auxiliar para evitar repetição
    private Caixa mapearCaixa(ResultSet rs) throws SQLException {
        Caixa c = new Caixa();
        c.setId(rs.getInt("id"));
        c.setSaldoInicial(rs.getDouble("saldo_inicial"));
        String dtAbertura = rs.getString("data_abertura");
        if (dtAbertura != null) c.setDataAbertura(LocalDateTime.parse(dtAbertura));
        c.setStatus(rs.getString("status"));
        c.setSaldoFinal(rs.getDouble("saldo_final"));
        return c;
    }

    public List<Caixa> listarHistorico() {
        // (Mantém o código anterior de listagem se precisar)
        return new ArrayList<>();
    }
}