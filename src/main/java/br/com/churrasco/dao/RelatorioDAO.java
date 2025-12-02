package br.com.churrasco.dao;

import br.com.churrasco.model.ItemRelatorio;
import br.com.churrasco.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RelatorioDAO {

    public List<ItemRelatorio> buscarVendasPorPeriodo(LocalDate inicio, LocalDate fim) {
        // SQL Poderoso: Soma quantidades e valores agrupando pelo ID do produto
        String sql = """
            SELECT 
                p.codigo, 
                p.nome, 
                p.unidade, 
                SUM(i.quantidade) as qtd_total, 
                SUM(i.total_item) as valor_total
            FROM itens_venda i
            JOIN vendas v ON i.venda_id = v.id
            JOIN produtos p ON i.produto_id = p.id
            WHERE date(v.data_hora) BETWEEN date(?) AND date(?)
            GROUP BY p.id
            ORDER BY valor_total DESC -- Ordena do que mais faturou para o que menos faturou
        """;

        List<ItemRelatorio> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, inicio.toString());
            pstmt.setString(2, fim.toString());

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                lista.add(new ItemRelatorio(
                        rs.getString("codigo"),
                        rs.getString("nome"),
                        rs.getString("unidade"),
                        rs.getDouble("qtd_total"),
                        rs.getDouble("valor_total")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}