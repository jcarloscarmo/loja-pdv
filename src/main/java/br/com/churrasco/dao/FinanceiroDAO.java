package br.com.churrasco.dao;

import br.com.churrasco.model.Despesa;
import br.com.churrasco.model.FluxoCaixaItem;
import br.com.churrasco.model.Venda;
import br.com.churrasco.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinanceiroDAO {

    // --- 1. CRUD DE DESPESAS ---
    public void salvarDespesa(Despesa d) {
        String sql = "INSERT INTO despesas (descricao, valor, data_pagamento, categoria, observacao) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, d.getDescricao());
            pstmt.setDouble(2, d.getValor());
            pstmt.setString(3, d.getDataPagamento().toString());
            pstmt.setString(4, d.getCategoria());
            pstmt.setString(5, d.getObservacao());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void excluirDespesa(int id) {
        String sql = "DELETE FROM despesas WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Despesa> buscarDespesasPorMes(LocalDate dataRef) {
        List<Despesa> lista = new ArrayList<>();
        String sql = "SELECT * FROM despesas WHERE strftime('%m', data_pagamento) = ? AND strftime('%Y', data_pagamento) = ? ORDER BY data_pagamento DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.format("%02d", dataRef.getMonthValue()));
            pstmt.setString(2, String.valueOf(dataRef.getYear()));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                lista.add(new Despesa(
                        rs.getInt("id"),
                        rs.getString("descricao"),
                        rs.getDouble("valor"),
                        LocalDate.parse(rs.getString("data_pagamento")),
                        rs.getString("categoria"),
                        rs.getString("observacao")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // --- 2. RELATÓRIO FLUXO DE CAIXA (TABELA DIA A DIA) ---
    public List<FluxoCaixaItem> buscarFluxoMensal(LocalDate dataRef) {
        List<FluxoCaixaItem> fluxo = new ArrayList<>();
        Map<Integer, Double> mapReceitas = new HashMap<>();
        Map<Integer, Double> mapDespesas = new HashMap<>();

        String mesStr = String.format("%02d", dataRef.getMonthValue());
        String anoStr = String.valueOf(dataRef.getYear());

        try (Connection conn = DatabaseConnection.getConnection()) {

            // A. Busca Receitas Agrupadas por Dia
            String sqlVendas = "SELECT strftime('%d', data_hora) as dia, SUM(valor_total) as total FROM vendas WHERE strftime('%m', data_hora) = ? AND strftime('%Y', data_hora) = ? GROUP BY dia";
            try (PreparedStatement ps = conn.prepareStatement(sqlVendas)) {
                ps.setString(1, mesStr);
                ps.setString(2, anoStr);
                ResultSet rs = ps.executeQuery();
                while(rs.next()) {
                    mapReceitas.put(rs.getInt("dia"), rs.getDouble("total"));
                }
            }

            // B. Busca Despesas Agrupadas por Dia
            String sqlDesp = "SELECT strftime('%d', data_pagamento) as dia, SUM(valor) as total FROM despesas WHERE strftime('%m', data_pagamento) = ? AND strftime('%Y', data_pagamento) = ? GROUP BY dia";
            try (PreparedStatement ps = conn.prepareStatement(sqlDesp)) {
                ps.setString(1, mesStr);
                ps.setString(2, anoStr);
                ResultSet rs = ps.executeQuery();
                while(rs.next()) {
                    mapDespesas.put(rs.getInt("dia"), rs.getDouble("total"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // C. Compila o Relatório dia a dia
        int diasNoMes = dataRef.lengthOfMonth();
        double acumulado = 0.0;

        for (int dia = 1; dia <= diasNoMes; dia++) {
            double rec = mapReceitas.getOrDefault(dia, 0.0);
            double desp = mapDespesas.getOrDefault(dia, 0.0);
            double saldoDia = rec - desp;
            acumulado += saldoDia;

            fluxo.add(new FluxoCaixaItem(
                    LocalDate.of(dataRef.getYear(), dataRef.getMonth(), dia),
                    rec,
                    desp,
                    saldoDia,
                    acumulado
            ));
        }

        return fluxo;
    }

    // --- 3. TOTAIS PARA O DRE (GRÁFICOS) ---
    public Map<String, Double> buscarTotaisDRE(LocalDate dataRef) {
        Map<String, Double> totais = new HashMap<>();
        String mesStr = String.format("%02d", dataRef.getMonthValue());
        String anoStr = String.valueOf(dataRef.getYear());

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Receita Bruta
            String sqlRec = "SELECT SUM(valor_total) FROM vendas WHERE strftime('%m', data_hora) = ? AND strftime('%Y', data_hora) = ?";
            try(PreparedStatement ps = conn.prepareStatement(sqlRec)) {
                ps.setString(1, mesStr);
                ps.setString(2, anoStr);
                ResultSet rs = ps.executeQuery();
                totais.put("receita", rs.next() ? rs.getDouble(1) : 0.0);
            }

            // 2. Custo Mercadoria (CMV)
            String sqlCMV = "SELECT SUM(i.custo_unitario * i.quantidade) FROM itens_venda i JOIN vendas v ON i.venda_id = v.id WHERE strftime('%m', v.data_hora) = ? AND strftime('%Y', v.data_hora) = ?";
            try(PreparedStatement ps = conn.prepareStatement(sqlCMV)) {
                ps.setString(1, mesStr);
                ps.setString(2, anoStr);
                ResultSet rs = ps.executeQuery();
                totais.put("custo_prod", rs.next() ? rs.getDouble(1) : 0.0);
            }

            // 3. Despesas Operacionais
            String sqlDesp = "SELECT SUM(valor) FROM despesas WHERE strftime('%m', data_pagamento) = ? AND strftime('%Y', data_pagamento) = ?";
            try(PreparedStatement ps = conn.prepareStatement(sqlDesp)) {
                ps.setString(1, mesStr);
                ps.setString(2, anoStr);
                ResultSet rs = ps.executeQuery();
                totais.put("despesas", rs.next() ? rs.getDouble(1) : 0.0);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        double lucro = totais.getOrDefault("receita", 0.0)
                - totais.getOrDefault("custo_prod", 0.0)
                - totais.getOrDefault("despesas", 0.0);

        totais.put("lucro_liquido", lucro);

        return totais;
    }

    // --- 4. DETALHAMENTO DO DIA (NOVO) ---
    public List<Despesa> buscarDespesasDetalhadasPorDia(LocalDate data) {
        List<Despesa> lista = new ArrayList<>();
        String sql = "SELECT * FROM despesas WHERE date(data_pagamento) = date(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                lista.add(new Despesa(
                        rs.getInt("id"),
                        rs.getString("descricao"),
                        rs.getDouble("valor"),
                        LocalDate.parse(rs.getString("data_pagamento")),
                        rs.getString("categoria"),
                        rs.getString("observacao")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public List<Venda> buscarVendasDetalhadasPorDia(LocalDate data) {
        List<Venda> lista = new ArrayList<>();
        // Traz apenas o resumo básico de cada venda do dia
        String sql = "SELECT id, data_hora, valor_total, forma_pagamento FROM vendas WHERE date(data_hora) = date(?) ORDER BY data_hora DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Venda v = new Venda();
                v.setId(rs.getInt("id"));
                v.setDataHora(LocalDateTime.parse(rs.getString("data_hora")));
                v.setValorTotal(rs.getDouble("valor_total"));
                v.setFormaPagamento(rs.getString("forma_pagamento"));
                lista.add(v);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
}