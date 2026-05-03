package br.com.churrasco.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class ShortcutVersionManager {

    private static final String APP_NAME = "PDVChurrasco";
    private static final String VERSION_PROPERTY = "pdvchurrasco.app.version";
    private static final String BUILD_INFO_RESOURCE = "build-info.properties";

    private ShortcutVersionManager() {
    }

    public static void syncDesktopShortcut() {
        if (!isWindows()) {
            return;
        }

        String version = resolveVersion();
        if (version == null || version.isBlank()) {
            return;
        }

        Path launcherPath = resolveLauncherPath();
        Path desktopDir = resolveDesktopDirectory();
        if (launcherPath == null || desktopDir == null) {
            return;
        }

        try {
            Files.createDirectories(desktopDir);
            deleteOldShortcuts(desktopDir, version);
            createVersionedShortcut(desktopDir, launcherPath, version);
        } catch (Exception e) {
            LogUtil.registrarErro("Erro ao sincronizar atalho versionado", e);
        }
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("win");
    }

    private static String resolveVersion() {
        String version = System.getProperty(VERSION_PROPERTY);
        if (version != null && !version.isBlank()) {
            return version.trim();
        }

        try (InputStream input = ShortcutVersionManager.class.getClassLoader().getResourceAsStream(BUILD_INFO_RESOURCE)) {
            if (input == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(input);
            String fileVersion = properties.getProperty("app.version");
            return fileVersion == null ? null : fileVersion.trim();
        } catch (IOException e) {
            LogUtil.registrarErro("Erro ao ler build-info.properties", e);
            return null;
        }
    }

    private static Path resolveLauncherPath() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            return Paths.get(appPath);
        }

        try {
            Path codeSource = Paths.get(ShortcutVersionManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(codeSource) && codeSource.toString().toLowerCase().endsWith(".jar")) {
                return codeSource;
            }
        } catch (Exception e) {
            LogUtil.registrarErro("Erro ao localizar executavel da aplicacao", e);
        }

        return null;
    }

    private static Path resolveDesktopDirectory() {
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile == null || userProfile.isBlank()) {
            return null;
        }

        return Paths.get(userProfile, "Desktop");
    }

    private static void deleteOldShortcuts(Path desktopDir, String currentVersion) throws IOException {
        String currentShortcutName = shortcutFileName(currentVersion);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(desktopDir, APP_NAME + "*.lnk")) {
            for (Path shortcut : stream) {
                String fileName = shortcut.getFileName().toString();
                if (!fileName.equalsIgnoreCase(currentShortcutName)) {
                    Files.deleteIfExists(shortcut);
                }
            }
        }
    }

    private static void createVersionedShortcut(Path desktopDir, Path launcherPath, String version) throws IOException, InterruptedException {
        Path shortcutPath = desktopDir.resolve(shortcutFileName(version));
        if (Files.exists(shortcutPath)) {
            return;
        }

        String launcher = escapeForPowerShell(launcherPath.toString());
        String workingDir = escapeForPowerShell(launcherPath.getParent().toString());
        String shortcut = escapeForPowerShell(shortcutPath.toString());
        String description = escapeForPowerShell(APP_NAME + " v" + version);

        String command = String.join("; ",
            "$ws = New-Object -ComObject WScript.Shell",
            "$sc = $ws.CreateShortcut('" + shortcut + "')",
            "$sc.TargetPath = '" + launcher + "'",
            "$sc.WorkingDirectory = '" + workingDir + "'",
            "$sc.Description = '" + description + "'",
            "$sc.IconLocation = '" + launcher + "'",
            "$sc.Save()"
        );

        Process process = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-Command", command
        ).start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("Falha ao criar atalho versionado: " + error);
        }
    }

    private static String shortcutFileName(String version) {
        return APP_NAME + " v" + version + ".lnk";
    }

    private static String escapeForPowerShell(String value) {
        return value.replace("'", "''");
    }
}
