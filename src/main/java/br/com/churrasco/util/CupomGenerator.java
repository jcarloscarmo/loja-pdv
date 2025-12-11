package br.com.churrasco.util;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CupomGenerator {

    public static String gerarTexto(List<ItemVenda> itens, List<Pagamento> pagamentos, double totalLiquido) {
        ConfigDAO config = new ConfigDAO();
        StringBuilder sb = new StringBuilder();

        // --- CABEÇALHO ---
        sb.append(config.getValor("empresa_nome").orElse("CHURRASCARIA")).append("\n");
        if (Boolean.parseBoolean(config.getValor("print_datahora").orElse("true"))) {
            sb.append("Data: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        }
        sb.append("--------------------------------\n");

        // --- ITENS ---
        double subtotal = 0.0;
        for (ItemVenda item : itens) {
            String nome = item.getNomeProduto();
            if (nome.length() > 20) nome = nome.substring(0, 20);

            String qtd = "KG".equals(item.getProduto().getUnidade()) ?
                    String.format("%.3fkg", item.getQuantidade()) :
                    String.format("%.0f un", item.getQuantidade());

            sb.append(String.format("%-20s %8s\n", nome, String.format("R$%.2f", item.getTotalItem())));
            sb.append(String.format("   %s x R$%.2f\n", qtd, item.getPrecoUnitario()));

            subtotal += item.getTotalItem();
        }

        // --- TOTAIS E DESCONTO ---
        sb.append("--------------------------------\n");

        // Calcula se houve desconto (Diferença entre a soma dos itens e o total a pagar)
        double desconto = subtotal - totalLiquido;

        if (desconto > 0.01) {
            sb.append(String.format("SUBTOTAL:           R$ %6.2f\n", subtotal));
            sb.append(String.format("DESCONTO (-):       R$ %6.2f\n", desconto));
        }

        sb.append(String.format("TOTAL A PAGAR:      R$ %6.2f\n", totalLiquido));
        sb.append("--------------------------------\n");

        // --- PAGAMENTOS ---
        sb.append("PAGAMENTOS:\n");
        for (Pagamento p : pagamentos) {
            sb.append(String.format("%-20s R$ %6.2f\n", p.getTipo(), p.getValor()));
        }

        sb.append("\n\n");
        sb.append(config.getValor("rodape_cupom").orElse("Obrigado pela preferencia!"));

        return sb.toString();
    }
}