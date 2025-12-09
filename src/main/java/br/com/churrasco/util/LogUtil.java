package br.com.churrasco.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogUtil {

    private static final String ARQUIVO_LOG = "sistema.log";

    public static void registrar(String usuario, String acao) {
        // Ex: [09/12/2025 14:30:00] - Tião - Abriu o Caixa
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String mensagem = String.format("[%s] - %s - %s", dataHora, usuario, acao);

        // Imprime no console (para você ver enquanto programa)
        System.out.println("LOG: " + mensagem);

        // Salva no arquivo
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO_LOG, true))) {
            writer.write(mensagem);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Erro ao gravar log: " + e.getMessage());
        }
    }
}