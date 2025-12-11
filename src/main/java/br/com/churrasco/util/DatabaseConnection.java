package br.com.churrasco.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:pdv.db";
    private static boolean tabelasVerificadas = false;

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);

        if (!tabelasVerificadas) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA synchronous=NORMAL;");
            }
            verificarEAtualizarTabelas(conn);
            tabelasVerificadas = true;
        }

        return conn;
    }

    private static void verificarEAtualizarTabelas(Connection conn) {
        try (Statement stmt = conn.createStatement()) {

            // 1. USUÁRIOS
            stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT UNIQUE, senha TEXT, perfil TEXT)");

            // --- AQUI ESTÁ A MÁGICA: GARANTE O ADMIN SEM SENHA ---
            // Usamos INSERT OR REPLACE para garantir que ele exista com ID 1
            stmt.execute("INSERT OR REPLACE INTO usuarios (id, nome, senha, perfil) VALUES (1, 'ADMIN', '', 'ADMIN')");

            // 2. PRODUTOS
            stmt.execute("CREATE TABLE IF NOT EXISTS produtos (id INTEGER PRIMARY KEY AUTOINCREMENT, codigo TEXT UNIQUE, nome TEXT, preco_custo REAL, preco_venda REAL, unidade TEXT, estoque REAL)");

            // 3. CAIXAS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS caixas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id INTEGER,
                    data_abertura TEXT,
                    data_fechamento TEXT,
                    saldo_inicial REAL,
                    saldo_final REAL,
                    saldo_informado REAL,
                    diferenca REAL,
                    status TEXT,
                    FOREIGN KEY(usuario_id) REFERENCES usuarios(id)
                );
            """);
            try { stmt.execute("ALTER TABLE caixas ADD COLUMN diferenca REAL"); } catch (SQLException e) {}

            // 4. VENDAS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vendas (
                  id INTEGER PRIMARY KEY,
                  data_hora TEXT,
                  valor_total REAL,
                  desconto REAL DEFAULT 0.0,
                  forma_pagamento TEXT,
                  usuario_id INTEGER,
                  caixa_id INTEGER,
                  FOREIGN KEY(usuario_id) REFERENCES usuarios(id),
                  FOREIGN KEY(caixa_id) REFERENCES caixas(id)
                );
            """);
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN usuario_id INTEGER DEFAULT 1"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN forma_pagamento TEXT DEFAULT 'MISTO'"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN caixa_id INTEGER"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN desconto REAL DEFAULT 0.0"); } catch (SQLException e) {}

            // 5. ITENS VENDA
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS itens_venda (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, 
                    venda_id INTEGER, 
                    produto_id INTEGER, 
                    quantidade REAL, 
                    valor_unitario REAL, 
                    custo_unitario REAL,
                    total_item REAL
                );
            """);
            try { stmt.execute("ALTER TABLE itens_venda ADD COLUMN custo_unitario REAL DEFAULT 0"); } catch (SQLException e) {}

            // 6. PAGAMENTOS
            stmt.execute("CREATE TABLE IF NOT EXISTS pagamentos_venda (id INTEGER PRIMARY KEY AUTOINCREMENT, venda_id INTEGER, tipo TEXT, valor REAL)");

            // 7. CONFIGURAÇÕES
            stmt.execute("CREATE TABLE IF NOT EXISTS configuracoes (chave TEXT PRIMARY KEY, valor TEXT)");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('empresa_nome', 'CHURRASCARIA DO MESTRE')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('impressora_nome', 'POS58MM')");

            // 8. ENCOMENDAS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS encomendas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome_cliente TEXT,
                    data_retirada TEXT,
                    valor_total REAL,
                    status TEXT
                );
            """);

            // 9. ITENS DA ENCOMENDA
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS itens_encomenda (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    encomenda_id INTEGER,
                    produto_id INTEGER,
                    quantidade REAL,
                    valor_unitario REAL,
                    total_item REAL,
                    FOREIGN KEY(encomenda_id) REFERENCES encomendas(id),
                    FOREIGN KEY(produto_id) REFERENCES produtos(id)
                );
            """);

            // 10. DESPESAS
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS despesas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    descricao TEXT NOT NULL,
                    valor REAL NOT NULL,
                    data_pagamento TEXT NOT NULL,
                    categoria TEXT NOT NULL,
                    observacao TEXT
                );
            """);

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar banco: " + e.getMessage());
        }
    }
}