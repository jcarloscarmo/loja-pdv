package br.com.churrasco.util; // VOLTAMOS PARA UTIL PARA NÃO QUEBRAR O RESTO

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String APP_DIR_NAME = "PDVChurrasco";
    private static final String DB_FILE_NAME = "pdv.db";
    private static final Path DATABASE_PATH = inicializarCaminhoBanco();
    private static final String URL = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();
    private static boolean tabelasVerificadas = false;

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);

        if (!tabelasVerificadas) {
            try (Statement stmt = conn.createStatement()) {
                // Otimização
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA synchronous=NORMAL;");
            }
            verificarEAtualizarTabelas(conn);
            tabelasVerificadas = true;
        }

        return conn;
    }

    public static Path getDatabasePath() {
        return DATABASE_PATH;
    }

    private static Path inicializarCaminhoBanco() {
        Path destino = obterDiretorioPersistente().resolve(DB_FILE_NAME);

        try {
            Files.createDirectories(destino.getParent());
            migrarBancoLegadoSeNecessario(destino);
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel preparar o banco de dados em " + destino, e);
        }

        return destino;
    }

    private static Path obterDiretorioPersistente() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, APP_DIR_NAME);
        }

        return Paths.get(System.getProperty("user.home"), "." + APP_DIR_NAME);
    }

    private static void migrarBancoLegadoSeNecessario(Path destino) throws IOException {
        if (Files.exists(destino)) {
            return;
        }

        for (Path diretorioLegado : listarDiretoriosLegados()) {
            Path bancoLegado = diretorioLegado.resolve(DB_FILE_NAME).toAbsolutePath().normalize();
            if (!Files.exists(bancoLegado) || bancoLegado.equals(destino.toAbsolutePath().normalize())) {
                continue;
            }

            Files.copy(bancoLegado, destino, StandardCopyOption.REPLACE_EXISTING);
            copiarSeExistir(diretorioLegado.resolve(DB_FILE_NAME + "-wal").toAbsolutePath().normalize(), destino.resolveSibling(DB_FILE_NAME + "-wal"));
            copiarSeExistir(diretorioLegado.resolve(DB_FILE_NAME + "-shm").toAbsolutePath().normalize(), destino.resolveSibling(DB_FILE_NAME + "-shm"));
            break;
        }
    }

    private static Set<Path> listarDiretoriosLegados() {
        Set<Path> diretorios = new LinkedHashSet<>();
        diretorios.add(Paths.get("").toAbsolutePath().normalize());

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            diretorios.add(Paths.get(userDir).toAbsolutePath().normalize());
        }

        try {
            Path origemCodigo = Paths.get(DatabaseConnection.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path pastaCodigo = Files.isDirectory(origemCodigo) ? origemCodigo : origemCodigo.getParent();
            if (pastaCodigo != null) {
                diretorios.add(pastaCodigo.toAbsolutePath().normalize());
                if (pastaCodigo.getParent() != null) {
                    diretorios.add(pastaCodigo.getParent().toAbsolutePath().normalize());
                }
            }
        } catch (URISyntaxException | NullPointerException ignored) {
        }

        return diretorios;
    }

    private static void copiarSeExistir(Path origem, Path destino) throws IOException {
        if (Files.exists(origem)) {
            Files.copy(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void verificarEAtualizarTabelas(Connection conn) {
        try (Statement stmt = conn.createStatement()) {

            // 1. USUÁRIOS
            stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT UNIQUE, senha TEXT, perfil TEXT)");

            // No primeiro acesso, garante um admin padrao sem senha.
            ResultSet rsUser = stmt.executeQuery("SELECT COUNT(*) AS total FROM usuarios");
            if (rsUser.next() && rsUser.getInt("total") == 0) {
                stmt.execute("INSERT INTO usuarios (nome, senha, perfil) VALUES ('ADMIN', '', 'ADMIN')");
            }
            rsUser.close();

            // 2. PRODUTOS
            stmt.execute("CREATE TABLE IF NOT EXISTS produtos (id INTEGER PRIMARY KEY AUTOINCREMENT, codigo TEXT UNIQUE, nome TEXT, preco_custo REAL, preco_venda REAL, unidade TEXT, estoque REAL, exibir_no_pdv INTEGER DEFAULT 0, agrupar_em_proteina INTEGER DEFAULT 0)");
            try { stmt.execute("ALTER TABLE produtos ADD COLUMN exibir_no_pdv INTEGER DEFAULT 0"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE produtos ADD COLUMN agrupar_em_proteina INTEGER DEFAULT 0"); } catch (SQLException e) {}

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
                  desconto_manual REAL DEFAULT 0.0,
                  desconto_promocional REAL DEFAULT 0.0,
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
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN desconto_manual REAL DEFAULT 0.0"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE vendas ADD COLUMN desconto_promocional REAL DEFAULT 0.0"); } catch (SQLException e) {}

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
            stmt.execute("INSERT OR IGNORE INTO configuracoes (chave, valor) VALUES ('impressora_nome', 'Generic / Text Only')");

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

            // 11. PROMOCOES
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS promocoes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL,
                    preco_combo REAL NOT NULL,
                    data_inicio TEXT,
                    data_fim TEXT,
                    ativo INTEGER NOT NULL DEFAULT 1
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS promocao_itens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    promocao_id INTEGER NOT NULL,
                    produto_id INTEGER NOT NULL,
                    quantidade INTEGER NOT NULL,
                    FOREIGN KEY(promocao_id) REFERENCES promocoes(id),
                    FOREIGN KEY(produto_id) REFERENCES produtos(id)
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS venda_promocoes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    venda_id INTEGER NOT NULL,
                    promocao_id INTEGER,
                    nome_promocao TEXT NOT NULL,
                    quantidade_aplicada INTEGER NOT NULL DEFAULT 1,
                    desconto_aplicado REAL NOT NULL DEFAULT 0.0,
                    FOREIGN KEY(venda_id) REFERENCES vendas(id),
                    FOREIGN KEY(promocao_id) REFERENCES promocoes(id)
                );
            """);

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar banco: " + e.getMessage());
        }
    }
}
