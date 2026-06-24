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

    public static void createShutdownBackup(Path databasePath) throws SQLException, IOException {
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
