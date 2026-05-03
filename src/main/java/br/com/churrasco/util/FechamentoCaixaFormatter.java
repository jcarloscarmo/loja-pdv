package br.com.churrasco.util;

import br.com.churrasco.model.Caixa;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class FechamentoCaixaFormatter {

    private static final int LARGURA = 32;
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private FechamentoCaixaFormatter() {
    }

    public static String gerarTexto(Caixa caixa, ResumoFechamento resumo) {
        return String.join("\n", gerarLinhas(caixa, resumo));
    }

    public static List<String> gerarLinhas(Caixa caixa, ResumoFechamento resumo) {
        List<String> linhas = new ArrayList<>();

        linhas.add(centralizar("FECHAMENTO DE CAIXA"));
        linhas.add(separador());
        linhas.add("Caixa ID: " + caixa.getId());
        if (caixa.getDataAbertura() != null) {
            linhas.add("Abertura: " + caixa.getDataAbertura().format(DATA_HORA));
        }
        linhas.add("Fechamento: " + LocalDateTime.now().format(DATA_HORA));
        linhas.add(separador());
        linhas.add("RESUMO DE VENDAS");
        linhas.add(alinhar("Dinheiro", moeda(resumo.totalDinheiro())));
        linhas.add(alinhar("Pix", moeda(resumo.totalPix())));
        linhas.add(alinhar("Debito", moeda(resumo.totalDebito())));
        linhas.add(alinhar("Credito", moeda(resumo.totalCredito())));
        if (resumo.totalDesconto() > 0.009d) {
            linhas.add(alinhar("Desconto", moeda(resumo.totalDesconto())));
        }
        linhas.add(alinhar("Total Liquido", moeda(resumo.totalLiquido())));
        linhas.add(separador());
        linhas.add("CONFERENCIA GAVETA");
        linhas.add(alinhar("Fundo troco", moeda(valor(caixa.getSaldoInicial()))));
        linhas.add(alinhar("Esperado", moeda(valor(caixa.getSaldoFinal()))));
        linhas.add(alinhar("Informado", moeda(valor(caixa.getSaldoInformado()))));
        linhas.add(alinhar("Diferenca", moeda(valor(caixa.getDiferenca())) + " " + statusDiferenca(valor(caixa.getDiferenca()))));
        linhas.add(separador());
        linhas.add(centralizar("Assinatura do Operador"));

        return linhas;
    }

    public record ResumoFechamento(
            double totalDinheiro,
            double totalPix,
            double totalDebito,
            double totalCredito,
            double totalDesconto,
            double totalLiquido
    ) {}

    private static String alinhar(String esquerda, String direita) {
        String e = esquerda == null ? "" : esquerda.trim();
        String d = direita == null ? "" : direita.trim();
        if (e.length() + d.length() + 1 >= LARGURA) {
            String combinado = e + " " + d;
            return combinado.length() <= LARGURA ? combinado : combinado.substring(0, LARGURA);
        }
        return e + " ".repeat(LARGURA - e.length() - d.length()) + d;
    }

    private static String separador() {
        return "-".repeat(LARGURA);
    }

    private static String centralizar(String texto) {
        String valor = texto == null ? "" : texto;
        if (valor.length() >= LARGURA) return valor.substring(0, LARGURA);
        return " ".repeat((LARGURA - valor.length()) / 2) + valor;
    }

    private static String moeda(double valor) {
        return String.format("R$ %.2f", valor);
    }

    private static String statusDiferenca(double diferenca) {
        if (Math.abs(diferenca) < 0.01d) return "(OK)";
        return diferenca > 0 ? "(SOBRA)" : "(FALTA)";
    }

    private static double valor(Double numero) {
        return numero != null ? numero : 0.0d;
    }
}
