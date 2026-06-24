package br.com.churrasco.dao;

import br.com.churrasco.model.Produto;
import br.com.churrasco.model.Promocao;
import br.com.churrasco.model.PromocaoAplicada;
import br.com.churrasco.model.PromocaoItem;
import br.com.churrasco.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PromocaoDAO {

    public List<Promocao> listarTodas() {
        String sql = """
            SELECT p.id, p.nome, p.preco_combo, p.data_inicio, p.data_fim, p.ativo,
                   pi.id as item_id, pi.quantidade as item_quantidade,
                   pr.id as produto_id, pr.codigo, pr.nome as produto_nome, pr.preco_venda, pr.preco_custo, pr.unidade, pr.estoque,
                   pr.exibir_no_pdv, pr.agrupar_em_proteina
            FROM promocoes p
            LEFT JOIN promocao_itens pi ON pi.promocao_id = p.id
            LEFT JOIN produtos pr ON pr.id = pi.produto_id
            ORDER BY p.nome, pi.id
        """;

        return carregarPromocoes(sql, null);
    }

    public List<Promocao> listarAtivasParaData(LocalDate data) {
        String sql = """
            SELECT p.id, p.nome, p.preco_combo, p.data_inicio, p.data_fim, p.ativo,
                   pi.id as item_id, pi.quantidade as item_quantidade,
                   pr.id as produto_id, pr.codigo, pr.nome as produto_nome, pr.preco_venda, pr.preco_custo, pr.unidade, pr.estoque,
                   pr.exibir_no_pdv, pr.agrupar_em_proteina
            FROM promocoes p
            JOIN promocao_itens pi ON pi.promocao_id = p.id
            JOIN produtos pr ON pr.id = pi.produto_id
            WHERE p.ativo = 1
              AND (p.data_inicio IS NULL OR date(p.data_inicio) <= date(?))
              AND (p.data_fim IS NULL OR date(p.data_fim) >= date(?))
            ORDER BY p.nome, pi.id
        """;

        return carregarPromocoes(sql, data);
    }

    public void salvar(Promocao promocao) throws SQLException {
        String sqlPromocao = "INSERT INTO promocoes (nome, preco_combo, data_inicio, data_fim, ativo) VALUES (?, ?, ?, ?, ?)";
        String sqlItem = "INSERT INTO promocao_itens (promocao_id, produto_id, quantidade) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int promocaoId;
                try (PreparedStatement pstmt = conn.prepareStatement(sqlPromocao, Statement.RETURN_GENERATED_KEYS)) {
                    preencherPromocaoStatement(promocao, pstmt);
                    pstmt.executeUpdate();
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        rs.next();
                        promocaoId = rs.getInt(1);
                    }
                }

                salvarItens(conn, promocaoId, promocao.getItens(), sqlItem);
                conn.commit();
                promocao.setId(promocaoId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void atualizar(Promocao promocao) throws SQLException {
        String sqlPromocao = "UPDATE promocoes SET nome = ?, preco_combo = ?, data_inicio = ?, data_fim = ?, ativo = ? WHERE id = ?";
        String sqlDeleteItens = "DELETE FROM promocao_itens WHERE promocao_id = ?";
        String sqlItem = "INSERT INTO promocao_itens (promocao_id, produto_id, quantidade) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlPromocao)) {
                    preencherPromocaoStatement(promocao, pstmt);
                    pstmt.setInt(6, promocao.getId());
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDeleteItens)) {
                    pstmtDelete.setInt(1, promocao.getId());
                    pstmtDelete.executeUpdate();
                }

                salvarItens(conn, promocao.getId(), promocao.getItens(), sqlItem);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void excluir(int promocaoId) throws SQLException {
        String sqlDeleteItens = "DELETE FROM promocao_itens WHERE promocao_id = ?";
        String sqlDeletePromocao = "DELETE FROM promocoes WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteItens)) {
                    pstmt.setInt(1, promocaoId);
                    pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = conn.prepareStatement(sqlDeletePromocao)) {
                    pstmt.setInt(1, promocaoId);
                    pstmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void salvarPromocoesAplicadas(Connection conn, int vendaId, List<PromocaoAplicada> promocoesAplicadas) throws SQLException {
        String sql = "INSERT INTO venda_promocoes (venda_id, promocao_id, nome_promocao, quantidade_aplicada, desconto_aplicado) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (PromocaoAplicada aplicada : promocoesAplicadas) {
                pstmt.setInt(1, vendaId);
                if (aplicada.getPromocaoId() != null) {
                    pstmt.setInt(2, aplicada.getPromocaoId());
                } else {
                    pstmt.setNull(2, java.sql.Types.INTEGER);
                }
                pstmt.setString(3, aplicada.getNomePromocao());
                pstmt.setInt(4, aplicada.getQuantidadeAplicada() != null ? aplicada.getQuantidadeAplicada() : 0);
                pstmt.setDouble(5, aplicada.getDescontoAplicado() != null ? aplicada.getDescontoAplicado() : 0.0);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public List<PromocaoAplicada> buscarPromocoesPorVenda(int vendaId) {
        List<PromocaoAplicada> lista = new ArrayList<>();
        String sql = "SELECT promocao_id, nome_promocao, quantidade_aplicada, desconto_aplicado FROM venda_promocoes WHERE venda_id = ? ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vendaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PromocaoAplicada(
                            rs.getInt("promocao_id"),
                            rs.getString("nome_promocao"),
                            rs.getInt("quantidade_aplicada"),
                            rs.getDouble("desconto_aplicado"),
                            null,
                            null
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private void preencherPromocaoStatement(Promocao promocao, PreparedStatement pstmt) throws SQLException {
        pstmt.setString(1, promocao.getNome());
        pstmt.setDouble(2, promocao.getPrecoCombo() != null ? promocao.getPrecoCombo() : 0.0);
        if (promocao.getDataInicio() != null) pstmt.setString(3, promocao.getDataInicio().toString()); else pstmt.setNull(3, java.sql.Types.VARCHAR);
        if (promocao.getDataFim() != null) pstmt.setString(4, promocao.getDataFim().toString()); else pstmt.setNull(4, java.sql.Types.VARCHAR);
        pstmt.setBoolean(5, promocao.isAtivo());
    }

    private void salvarItens(Connection conn, int promocaoId, List<PromocaoItem> itens, String sqlItem) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sqlItem)) {
            for (PromocaoItem item : itens) {
                pstmt.setInt(1, promocaoId);
                pstmt.setInt(2, item.getProduto().getId());
                pstmt.setInt(3, item.getQuantidade());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private List<Promocao> carregarPromocoes(String sql, LocalDate data) {
        Map<Integer, Promocao> promocoes = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (data != null) {
                pstmt.setString(1, data.toString());
                pstmt.setString(2, data.toString());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int promocaoId = rs.getInt("id");
                    Promocao promocao = promocoes.computeIfAbsent(promocaoId, id -> mapearPromocaoBase(rs));

                    int produtoId = rs.getInt("produto_id");
                    if (!rs.wasNull()) {
                        promocao.getItens().add(mapearItem(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(promocoes.values());
    }

    private Promocao mapearPromocaoBase(ResultSet rs) {
        try {
            String dataInicio = rs.getString("data_inicio");
            String dataFim = rs.getString("data_fim");
            return new Promocao(
                    rs.getInt("id"),
                    rs.getString("nome"),
                    rs.getDouble("preco_combo"),
                    parseDataPromocao(dataInicio),
                    parseDataPromocao(dataFim),
                    rs.getBoolean("ativo")
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private LocalDate parseDataPromocao(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String texto = valor.trim();
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException ignored) {
        }

        if (texto.matches("\\d+")) {
            long epochMillis = Long.parseLong(texto);
            return Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .plusDays(1);
        }

        throw new IllegalArgumentException("Data de promoção em formato inválido: " + valor);
    }

    private PromocaoItem mapearItem(ResultSet rs) throws SQLException {
        Produto produto = new Produto();
        produto.setId(rs.getInt("produto_id"));
        produto.setCodigo(rs.getString("codigo"));
        produto.setNome(rs.getString("produto_nome"));
        produto.setPrecoVenda(rs.getDouble("preco_venda"));
        produto.setPrecoCusto(rs.getDouble("preco_custo"));
        produto.setUnidade(rs.getString("unidade"));
        produto.setEstoque(rs.getDouble("estoque"));
        produto.setExibirNoPdv(rs.getBoolean("exibir_no_pdv"));
        produto.setAgruparEmProteina(rs.getBoolean("agrupar_em_proteina"));

        return new PromocaoItem(
                rs.getInt("item_id"),
                rs.getInt("id"),
                produto,
                rs.getInt("item_quantidade")
        );
    }
}
