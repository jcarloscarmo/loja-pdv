package br.com.churrasco.util;

import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CupomGenerator {

    public static String gerarTexto(List<ItemVenda> itens, List<Pagamento> pagamentos, double total) {
        StringBuilder sb = new StringBuilder();
        sb.append("CHURRASCARIA DO MESTRE\n");
        sb.append("Data: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        sb.append("--------------------------------\n");

        for (ItemVenda item : itens) {
            String nome = item.getNomeProduto();
            if (nome.length() > 20) nome = nome.substring(0, 20);

            String qtd;
            if ("KG".equals(item.getProduto().getUnidade())) qtd = String.format("%.3fkg", item.getQuantidade());
            else qtd = String.format("%.0f un", item.getQuantidade());

            sb.append(String.format("%-20s %8s\n", nome, String.format("R$%.2f", item.getTotalItem())));
            sb.append(String.format("   %s x R$%.2f\n", qtd, item.getPrecoUnitario()));
        }

        sb.append("--------------------------------\n");
        sb.append(String.format("TOTAL:              R$ %6.2f\n", total));
        sb.append("--------------------------------\n");
        sb.append("PAGAMENTOS:\n");

        for (Pagamento p : pagamentos) {
            sb.append(String.format("%-20s R$ %6.2f\n", p.getTipo(), p.getValor()));
        }

        sb.append("\n\nOBRIGADO PELA PREFERENCIA!");
        return sb.toString();
    }
}