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

            // 1. USUÁRIOS
            stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT, senha TEXT, perfil TEXT)");
            stmt.execute("INSERT OR IGNORE INTO usuarios (id, nome, senha, perfil) VALUES (1, 'Admin', '1234', 'ADMIN')");

            // 2. PRODUTOS
            stmt.execute("CREATE TABLE IF NOT EXISTS produtos (id INTEGER PRIMARY KEY AUTOINCREMENT, codigo TEXT UNIQUE, nome TEXT, preco_custo REAL, preco_venda REAL, unidade TEXT, estoque REAL)");

            // 3. CAIXAS (SESSÕES)
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
            // Migration para garantir coluna diferenca em bancos antigos
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
            // Migrations vendas
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN usuario_id INTEGER DEFAULT 1"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN forma_pagamento TEXT DEFAULT 'MISTO'"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN caixa_id INTEGER"); } catch (SQLException e) {}

            // 5. ITENS VENDA
            stmt.execute("CREATE TABLE IF NOT EXISTS itens_venda (id INTEGER PRIMARY KEY AUTOINCREMENT, venda_id INTEGER, produto_id INTEGER, quantidade REAL, valor_unitario REAL, total_item REAL)");

            // 6. PAGAMENTOS
            stmt.execute("CREATE TABLE IF NOT EXISTS pagamentos_venda (id INTEGER PRIMARY KEY AUTOINCREMENT, venda_id INTEGER, tipo TEXT, valor REAL)");

            // --- 7. CONFIGURAÇÕES (NOVA TABELA) ---
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS configuracoes (
                    chave TEXT PRIMARY KEY,
                    valor TEXT
                );
            """);

            // Seed (Valores Padrão)
            // Dados da Empresa
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('empresa_nome', 'CHURRASCARIA DO MESTRE')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('empresa_cnpj', '00.000.000/0001-99')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('empresa_endereco', 'Rua das Carnes, 123 - Centro')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('empresa_telefone', '(11) 99999-9999')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('rodape_cupom', 'Volte Sempre!')");

            // Flags de Visibilidade (O que imprimir?) - 'true' ou 'false'
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('print_cnpj', 'true')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('print_endereco', 'true')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('print_telefone', 'true')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('print_datahora', 'true')");

            // Hardware
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('impressora_nome', 'POS58MM')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('balanca_porta', 'COM5')");
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('balanca_velocidade', '2400')");

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar banco: " + e.getMessage());
        }
    }
}