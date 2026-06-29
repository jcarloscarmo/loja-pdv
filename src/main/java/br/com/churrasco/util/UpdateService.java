package br.com.churrasco.util;

import javafx.concurrent.Task;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateService {

    private static final String GITHUB_REPO = "jcarloscarmo/loja-pdv";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    public static class UpdateInfo {
        public final String version;
        public final String downloadUrl;

        public UpdateInfo(String version, String downloadUrl) {
            this.version = version;
            this.downloadUrl = downloadUrl;
        }
    }

    public static Optional<UpdateInfo> checkUpdate() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String json = response.body();

                // Regex simples para não precisar importar bibliotecas JSON apenas para isso
                Pattern tagPattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"[vV]?([^\"]+)\"");
                Matcher tagMatcher = tagPattern.matcher(json);

                Pattern urlPattern = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.msi)\"");
                Matcher urlMatcher = urlPattern.matcher(json);

                if (tagMatcher.find() && urlMatcher.find()) {
                    String onlineVersion = tagMatcher.group(1);
                    String downloadUrl = urlMatcher.group(1);

                    String currentVersion = System.getProperty("pdvchurrasco.app.version", "1.0.0");
                    
                    // Tratamento simples: se for diferente, tem atualização.
                    if (!currentVersion.equals(onlineVersion)) {
                        return Optional.of(new UpdateInfo(onlineVersion, downloadUrl));
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.registrarErro("Falha ao checar atualização OTA", e);
        }
        return Optional.empty();
    }

    public static Task<Path> createDownloadTask(UpdateInfo updateInfo) {
        return new Task<>() {
            @Override
            protected Path call() throws Exception {
                updateMessage("Iniciando download da versão " + updateInfo.version + "...");
                
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(updateInfo.downloadUrl)).GET().build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Erro ao baixar o arquivo: HTTP " + response.statusCode());
                }

                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                Path tempFile = Files.createTempFile("pdvchurrasco-update-", ".msi");

                try (InputStream is = response.body();
                     java.io.OutputStream os = Files.newOutputStream(tempFile)) {

                    byte[] buffer = new byte[8192];
                    long totalBytesRead = 0;
                    int bytesRead;

                    while ((bytesRead = is.read(buffer)) != -1) {
                        if (isCancelled()) {
                            Files.deleteIfExists(tempFile);
                            return null;
                        }
                        os.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        if (contentLength > 0) {
                            updateProgress(totalBytesRead, contentLength);
                            updateMessage(String.format("Baixando: %.1f MB / %.1f MB", 
                                totalBytesRead / 1048576.0, contentLength / 1048576.0));
                        } else {
                            updateMessage("Baixando... " + (totalBytesRead / 1048576) + " MB");
                        }
                    }
                }

                updateMessage("Download concluído! Iniciando instalador...");
                return tempFile;
            }
        };
    }

    public static void installAndExit(Path msiPath) {
        try {
            LogUtil.registrar("OTA", "Iniciando instalação silenciosa: " + msiPath.toString());
            Runtime.getRuntime().exec(new String[]{"msiexec.exe", "/i", msiPath.toAbsolutePath().toString(), "/passive"});
            System.exit(0);
        } catch (Exception e) {
            LogUtil.registrarErro("Falha ao iniciar o instalador", e);
        }
    }
}
