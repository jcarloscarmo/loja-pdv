package br.com.churrasco.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:pdv.db";

    // Flag para garantir que só criamos as tabelas 1 vez por execução
    private static boolean tabelasVerificadas = false;

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);

        // Só entra aqui na primeira vez que o sistema rodar (lá no Main ou Splash)
        if (!tabelasVerificadas) {
            try (Statement stmt = conn.createStatement()) {
                // Ativa o modo WAL
                stmt.execute("PRAGMA journal_mode=WAL;");
                // Sincronismo NORMAL é mais rápido e seguro o suficiente
                stmt.execute("PRAGMA synchronous=NORMAL;");
            }
            verificarEAtualizarTabelas(conn);
            tabelasVerificadas = true; // Trava para não rodar mais
        }

        return conn;
    }

    private static void verificarEAtualizarTabelas(Connection conn) {
        try (Statement stmt = conn.createStatement()) {

            // 1. USUÁRIOS
            stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT UNIQUE, senha TEXT, perfil TEXT)");
            // Inserts com REPLACE para garantir que existam
            stmt.execute("INSERT OR REPLACE INTO usuarios (id, nome, senha, perfil) VALUES (1, 'Tião', 'fuka2010', 'DONO')");
            stmt.execute("INSERT OR REPLACE INTO usuarios (id, nome, senha, perfil) VALUES (2, 'Clara', '2025', 'ATENDENTE')");

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
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN usuario_id INTEGER DEFAULT 1"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN forma_pagamento TEXT DEFAULT 'MISTO'"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN caixa_id INTEGER"); } catch (SQLException e) {}

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
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('balanca_porta', 'COM5')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('balanca_velocidade', '2400')");

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

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar banco: " + e.getMessage());
        }
    }
}