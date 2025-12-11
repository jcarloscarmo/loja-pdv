package br.com.churrasco.service;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.model.Caixa;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import br.com.churrasco.model.Venda;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ImpressoraService {

    private final ConfigDAO configDAO = new ConfigDAO();

    // --- 1. CUPOM DE VENDA ---
    public void imprimirCupom(Venda venda, List<ItemVenda> itens, List<Pagamento> pagamentos) {
        try {
            PrintService printService = obterPrintService();
            if (printService == null) return;

            try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
                 EscPos escpos = new EscPos(printerOutputStream)) {

                Style titulo = new Style().setFontSize(Style.FontSize._2, Style.FontSize._1).setJustification(EscPosConst.Justification.Center).setBold(true);
                Style centro = new Style().setJustification(EscPosConst.Justification.Center);
                Style normal = new Style().setFontSize(Style.FontSize._1, Style.FontSize._1);
                Style negrito = new Style().setBold(true);

                // Cabeçalho
                escpos.writeLF(titulo, getValorConfig("empresa_nome", "CHURRASCARIA"));
                if (Boolean.parseBoolean(getValorConfig("print_datahora", "true"))) {
                    escpos.writeLF(centro, venda.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                }
                escpos.writeLF(centro, "--------------------------------");
                escpos.writeLF(centro, "Recibo #" + venda.getId());
                escpos.writeLF(centro, "--------------------------------");

                // Itens
                double subtotal = 0.0;
                for (ItemVenda item : itens) {
                    String nome = item.getNomeProduto();
                    if (nome.length() > 32) nome = nome.substring(0, 32);
                    escpos.writeLF(negrito, nome);

                    String qtd = "KG".equals(item.getProduto().getUnidade()) ?
                            String.format("%.3fkg", item.getQuantidade()) : String.format("%.0f un", item.getQuantidade());

                    escpos.writeLF(normal, String.format("%s x R$%.2f", qtd, item.getPrecoUnitario()));
                    escpos.writeLF(negrito, String.format("%32s", String.format("R$ %.2f", item.getTotalItem())));
                    escpos.writeLF(normal, "................................");

                    subtotal += item.getTotalItem();
                }

                escpos.writeLF(centro, "--------------------------------");

                // Lógica de Desconto no Papel
                double totalLiquido = venda.getValorTotal();
                double desconto = subtotal - totalLiquido;

                if (desconto > 0.01) {
                    escpos.writeLF(normal, String.format("SUBTOTAL:       R$ %8.2f", subtotal));
                    escpos.writeLF(normal, String.format("DESCONTO (-):   R$ %8.2f", desconto));
                }

                escpos.writeLF(titulo, String.format("TOTAL: R$ %.2f", totalLiquido));
                escpos.writeLF(centro, "--------------------------------");

                for (Pagamento p : pagamentos) {
                    escpos.writeLF(centro, String.format("%s: R$ %.2f", p.getTipo(), p.getValor()));
                }

                double totalPago = pagamentos.stream().mapToDouble(Pagamento::getValor).sum();
                double troco = totalPago - totalLiquido;
                if (troco > 0.01) {
                    escpos.feed(1);
                    escpos.writeLF(negrito, String.format("   TROCO: R$ %.2f", troco));
                }

                escpos.feed(2);
                escpos.writeLF(centro, getValorConfig("rodape_cupom", "Volte Sempre!"));
                escpos.feed(4);
                escpos.cut(EscPos.CutMode.FULL);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- 2. RELATÓRIO DE FECHAMENTO (ATUALIZADO COM DESCONTO) ---
    // Adicionei o parâmetro 'double tDesc'
    public void imprimirRelatorioFechamento(Caixa caixa, double tDin, double tPix, double tDeb, double tCre, double tDesc) {
        try {
            PrintService printService = obterPrintService();
            if (printService == null) return;

            try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
                 EscPos escpos = new EscPos(printerOutputStream)) {

                Style titulo = new Style().setFontSize(Style.FontSize._2, Style.FontSize._1).setJustification(EscPosConst.Justification.Center).setBold(true);
                Style centro = new Style().setJustification(EscPosConst.Justification.Center);
                Style normal = new Style().setFontSize(Style.FontSize._1, Style.FontSize._1);
                Style negrito = new Style().setBold(true);

                escpos.writeLF(titulo, "FECHAMENTO DE CAIXA");
                escpos.writeLF(centro, "--------------------------------");
                escpos.writeLF(centro, "Caixa ID: " + caixa.getId());
                escpos.writeLF(centro, "Fechamento: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
                escpos.writeLF(centro, "--------------------------------");

                escpos.writeLF(negrito, "RESUMO DE VENDAS:");

                if (tDesc > 0.01) {
                    escpos.writeLF(normal, String.format("(-) Descontos:  R$ %8.2f", tDesc));
                }

                escpos.writeLF(normal, String.format("Dinheiro:       R$ %8.2f", tDin));
                escpos.writeLF(normal, String.format("Pix:            R$ %8.2f", tPix));
                escpos.writeLF(normal, String.format("Debito:         R$ %8.2f", tDeb));
                escpos.writeLF(normal, String.format("Credito:        R$ %8.2f", tCre));

                double totalGeral = tDin + tPix + tDeb + tCre;
                escpos.writeLF(negrito, String.format("TOTAL LIQ:      R$ %8.2f", totalGeral));
                escpos.writeLF(centro, "--------------------------------");

                escpos.writeLF(negrito, "CONFERENCIA GAVETA:");
                escpos.writeLF(normal, String.format("Fundo Troco:    R$ %8.2f", caixa.getSaldoInicial()));
                escpos.writeLF(normal, String.format("Esperado (Din): R$ %8.2f", caixa.getSaldoFinal()));
                escpos.writeLF(normal, String.format("Informado:      R$ %8.2f", caixa.getSaldoInformado()));

                double dif = caixa.getDiferenca();
                String status = Math.abs(dif) < 0.01 ? "OK" : (dif > 0 ? "SOBRA" : "FALTA");
                escpos.writeLF(negrito, String.format("DIFERENCA:      R$ %8.2f (%s)", dif, status));

                escpos.writeLF(centro, "--------------------------------");
                escpos.writeLF(centro, "Assinatura do Operador");
                escpos.feed(4);
                escpos.cut(EscPos.CutMode.FULL);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void imprimirTeste() { /* ... código de teste mantido ... */ }

    // Auxiliares
    private PrintService obterPrintService() {
        String nomeConfig = getValorConfig("impressora_nome", "");
        if (!nomeConfig.isEmpty()) {
            PrintService servico = PrinterOutputStream.getPrintServiceByName(nomeConfig);
            if (servico != null) return servico;
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    private String getValorConfig(String chave, String padrao) {
        try {
            Optional<String> val = configDAO.getValor(chave);
            return val.orElse(padrao);
        } catch (Exception e) { return padrao; }
    }
}