package br.com.churrasco.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:pdv.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        verificarEAtualizarTabelas(conn);
        return conn;
    }

    private static void verificarEAtualizarTabelas(Connection conn) {
        try (Statement stmt = conn.createStatement()) {

            // USUÁRIOS
            stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT, senha TEXT, perfil TEXT)");
            stmt.execute("INSERT OR IGNORE INTO usuarios (id, nome, senha, perfil) VALUES (1, 'Admin', '1234', 'ADMIN')");

            // PRODUTOS
            stmt.execute("CREATE TABLE IF NOT EXISTS produtos (id INTEGER PRIMARY KEY AUTOINCREMENT, codigo TEXT UNIQUE, nome TEXT, preco_custo REAL, preco_venda REAL, unidade TEXT, estoque REAL)");

            // CAIXAS (SESSÕES)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS caixas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id INTEGER,
                    data_abertura TEXT,
                    data_fechamento TEXT,
                    saldo_inicial REAL,
                    saldo_final REAL,
                    saldo_informado REAL,
                    diferenca REAL,  -- Garante que essa coluna exista no create
                    status TEXT,
                    FOREIGN KEY(usuario_id) REFERENCES usuarios(id)
                );
            """);

            // --- MIGRATION DE EMERGÊNCIA (CORREÇÃO DO ERRO) ---
            try { stmt.execute("ALTER TABLE caixas ADD COLUMN diferenca REAL"); } catch (SQLException e) {}
            // --------------------------------------------------

            // VENDAS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vendas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id INTEGER,
                    caixa_id INTEGER,
                    data_hora TEXT,
                    valor_total REAL,
                    forma_pagamento TEXT,
                    FOREIGN KEY(usuario_id) REFERENCES usuarios(id),
                    FOREIGN KEY(caixa_id) REFERENCES caixas(id)
                );
            """);
            // Migrations vendas
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN usuario_id INTEGER DEFAULT 1"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN forma_pagamento TEXT DEFAULT 'MISTO'"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN caixa_id INTEGER"); } catch (SQLException e) {}

            // ITENS E PAGAMENTOS
            stmt.execute("CREATE TABLE IF NOT EXISTS itens_venda (id INTEGER PRIMARY KEY AUTOINCREMENT, venda_id INTEGER, produto_id INTEGER, quantidade REAL, valor_unitario REAL, total_item REAL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS pagamentos_venda (id INTEGER PRIMARY KEY AUTOINCREMENT, venda_id INTEGER, tipo TEXT, valor REAL)");

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar banco: " + e.getMessage());
        }
    }
}