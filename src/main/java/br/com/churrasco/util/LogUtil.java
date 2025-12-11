package br.com.churrasco.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogUtil {

    private static final String ARQUIVO_LOG = "sistema.log";

    // Método existente (mantém igual)
    public static void registrar(String usuario, String acao) {
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String mensagem = String.format("[%s] - %s - %s", dataHora, usuario, acao);
        System.out.println("LOG: " + mensagem);
        escreverNoArquivo(mensagem);
    }

    // --- NOVO MÉTODO PARA ERROS TÉCNICOS ---
    public static void registrarErro(String contexto, Throwable erro) {
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        try (FileWriter fw = new FileWriter(ARQUIVO_LOG, true);
             PrintWriter pw = new PrintWriter(fw)) {

            pw.println("--------------------------------------------------");
            pw.println("ERRO CRÍTICO [" + dataHora + "]");
            pw.println("Contexto: " + contexto);
            pw.println("Mensagem: " + erro.getMessage());
            pw.println("--- Detalhes Técnicos (Stacktrace) ---");
            erro.printStackTrace(pw); // Isso escreve o erro completo no arquivo
            pw.println("--------------------------------------------------");
            pw.println(); // Linha em branco

        } catch (IOException e) {
            System.err.println("Erro fatal: Não conseguiu gravar o log de erro.");
            e.printStackTrace();
        }
    }

    // Auxiliar para evitar repetição
    private static void escreverNoArquivo(String msg) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO_LOG, true))) {
            writer.write(msg);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}