package br.com.churrasco.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public final class DatabaseBackupService {

    private static final String BACKUP_DIR_NAME = "backups";
    private static final String PRE_UPDATE_PREFIX = "pre-update-";
    private static final String SHUTDOWN_PREFIX = "shutdown-";
    private static final String BACKUP_EXTENSION = ".db";
    private static final String VERSION_PROPERTY = "pdvchurrasco.app.version";
    private static final String BUILD_INFO_RESOURCE = "build-info.properties";
    private static final int MAX_SHUTDOWN_BACKUPS = 10;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static volatile boolean shutdownHookRegistered = false;
    private static volatile boolean backupConcluidoViaUI = false;

    private DatabaseBackupService() {
    }

    public static void registerShutdownBackupHook() {
        if (shutdownHookRegistered) {
            return;
        }

        synchronized (DatabaseBackupService.class) {
            if (shutdownHookRegistered) {
                return;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    createShutdownBackup(DatabaseConnection.getDatabasePath());
                } catch (Exception e) {
                    LogUtil.registrarErro("Erro ao criar backup no encerramento", e);
                }
            }, "database-shutdown-backup"));

            shutdownHookRegistered = true;
        }
    }

    public static void createPreUpdateBackupIfNeeded(Connection conn, Path databasePath) throws SQLException {
        if (databasePath == null || !Files.exists(databasePath)) {
            return;
        }

        try {
            Path backupDir = ensureBackupDirectory(databasePath);
            String version = resolveVersion();
            Path backupPath = backupDir.resolve(PRE_UPDATE_PREFIX + sanitizeVersion(version) + BACKUP_EXTENSION);

            if (Files.exists(backupPath)) {
                return;
            }

            createBackup(conn, backupPath);
            cleanupOldPreUpdateBackups(backupDir, backupPath);
            LogUtil.registrar("SISTEMA", "Backup pre-update criado em " + backupPath);
        } catch (IOException e) {
            throw new SQLException("Falha ao criar backup pre-update", e);
        }
    }

    public static void createBackupViaUI(Path databasePath) throws SQLException, IOException {
        createShutdownBackup(databasePath, true);
        backupConcluidoViaUI = true;
    }

    public static List<Path> listarBackups(Path databasePath) throws IOException {
        Path backupDir = ensureBackupDirectory(databasePath);
        if (!Files.exists(backupDir)) return new ArrayList<>();

        List<Path> backups = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir, SHUTDOWN_PREFIX + "*" + BACKUP_EXTENSION)) {
            for (Path backup : stream) {
                backups.add(backup);
            }
        }

        // Os mais recentes primeiro
        backups.sort(Comparator.comparing(Path::getFileName).reversed());
        return backups;
    }

    public static void restaurarBackup(Path backupFile, Path databasePath) throws IOException {
        if (backupFile == null || !Files.exists(backupFile)) {
            throw new IOException("Arquivo de backup não encontrado.");
        }

        // 1. Tenta restaurar via comando nativo do SQLite JDBC (evita file lock do Windows)
        boolean restauradoViaSql = false;
        try (java.sql.Connection conn = DatabaseConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            
            // O comando 'restore from' é uma extensão do driver sqlite-jdbc que substitui o banco online!
            stmt.executeUpdate("restore from '" + backupFile.toAbsolutePath().toString().replace("\\", "/") + "'");
            restauradoViaSql = true;
            LogUtil.registrar("SISTEMA", "Restore realizado via SQL (sqlite-jdbc).");
            
        } catch (Exception e) {
            LogUtil.registrarErro("Restore SQL falhou, tentando sobrescrita manual", e);
        }

        // 2. Fallback: Se o driver não suportar ou der erro, tentamos sobrescrever na marra
        if (!restauradoViaSql) {
            // Força o Garbage Collector pra tentar limpar Conexões perdidas que causam o Lock no Windows
            System.gc();
            try { Thread.sleep(500); } catch (InterruptedException ignore) {}

            Files.copy(backupFile, databasePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            Path walFile = databasePath.resolveSibling(databasePath.getFileName().toString() + "-wal");
            Path shmFile = databasePath.resolveSibling(databasePath.getFileName().toString() + "-shm");

            Files.deleteIfExists(walFile);
            Files.deleteIfExists(shmFile);
        }
    }

    public static void createShutdownBackup(Path databasePath) throws SQLException, IOException {
        createShutdownBackup(databasePath, false);
    }

    private static void createShutdownBackup(Path databasePath, boolean force) throws SQLException, IOException {
        if (!force && backupConcluidoViaUI) {
            return;
        }

        if (databasePath == null || !Files.exists(databasePath)) {
            return;
        }

        Path backupDir = ensureBackupDirectory(databasePath);
        Path backupPath = backupDir.resolve(SHUTDOWN_PREFIX + LocalDateTime.now().format(TIMESTAMP_FORMAT) + BACKUP_EXTENSION);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath())) {
            createBackup(conn, backupPath);
        }

        cleanupOldShutdownBackups(backupDir);
        LogUtil.registrar("SISTEMA", "Backup de encerramento criado em " + backupPath);
    }

    private static void createBackup(Connection conn, Path backupPath) throws SQLException, IOException {
        Files.deleteIfExists(backupPath);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("VACUUM INTO '" + escapeSqlLiteral(backupPath.toAbsolutePath().toString()) + "'");
        }

        // 1. Verificação de tamanho (não pode ser vazio)
        if (Files.size(backupPath) == 0) {
            Files.deleteIfExists(backupPath);
            throw new SQLException("Falha no backup: O arquivo gerado possui 0 bytes.");
        }

        // 2. Verificação de integridade estrutural do banco gerado
        try (Connection connBackup = DriverManager.getConnection("jdbc:sqlite:" + backupPath.toAbsolutePath());
             Statement stmtBackup = connBackup.createStatement();
             java.sql.ResultSet rs = stmtBackup.executeQuery("PRAGMA integrity_check;")) {
            
            if (rs.next()) {
                String result = rs.getString(1);
                if (!"ok".equalsIgnoreCase(result)) {
                    Files.deleteIfExists(backupPath);
                    throw new SQLException("Falha na integridade do backup: " + result);
                }
            } else {
                throw new SQLException("Falha ao verificar integridade: Nenhuma resposta retornada.");
            }
        }
    }

    private static Path ensureBackupDirectory(Path databasePath) throws IOException {
        Path backupDir = databasePath.getParent().resolve(BACKUP_DIR_NAME);
        Files.createDirectories(backupDir);
        return backupDir;
    }

    private static void cleanupOldPreUpdateBackups(Path backupDir, Path currentBackupPath) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir, PRE_UPDATE_PREFIX + "*" + BACKUP_EXTENSION)) {
            for (Path backup : stream) {
                if (!backup.equals(currentBackupPath)) {
                    Files.deleteIfExists(backup);
                }
            }
        }
    }

    private static void cleanupOldShutdownBackups(Path backupDir) throws IOException {
        List<Path> shutdownBackups = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir, SHUTDOWN_PREFIX + "*" + BACKUP_EXTENSION)) {
            for (Path backup : stream) {
                shutdownBackups.add(backup);
            }
        }

        shutdownBackups.sort(Comparator.comparing(Path::getFileName).reversed());
        for (int i = MAX_SHUTDOWN_BACKUPS; i < shutdownBackups.size(); i++) {
            Files.deleteIfExists(shutdownBackups.get(i));
        }
    }

    private static String resolveVersion() {
        String version = System.getProperty(VERSION_PROPERTY);
        if (version != null && !version.isBlank()) {
            return version.trim();
        }

        try (InputStream input = DatabaseBackupService.class.getClassLoader().getResourceAsStream(BUILD_INFO_RESOURCE)) {
            if (input == null) {
                return "sem-versao";
            }

            Properties properties = new Properties();
            properties.load(input);
            String fileVersion = properties.getProperty("app.version");
            return (fileVersion == null || fileVersion.isBlank()) ? "sem-versao" : fileVersion.trim();
        } catch (IOException e) {
            LogUtil.registrarErro("Erro ao resolver versao para backup", e);
            return "sem-versao";
        }
    }

    private static String sanitizeVersion(String version) {
        return version.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }
}
