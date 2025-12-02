package br.com.churrasco.controller;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.model.Caixa;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import br.com.churrasco.model.Venda;
import br.com.churrasco.util.ConfigKeys;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ImpressoraService {

    private final ConfigDAO configDAO;

    public ImpressoraService() {
        this.configDAO = new ConfigDAO();
    }

    /**
     * Tenta obter o PrintService configurado ou o padrão do sistema.
     */
    private PrintService getPrintService() {
        String nomeImpressora = configDAO.getValor(ConfigKeys.IMPRESSORA_NOME).orElse(null);

        if (nomeImpressora != null && !nomeImpressora.isEmpty()) {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService ps : services) {
                if (ps.getName().equalsIgnoreCase(nomeImpressora)) {
                    return ps;
                }
            }
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    // --- IMPRESSÃO DE VENDA (CUPOM) ---
    public void imprimirCupom(Venda venda, List<ItemVenda> itens, List<Pagamento> pagamentos) {
        PrintService printService = getPrintService();
        if (printService == null) {
            System.err.println("Impressora não encontrada.");
            return;
        }

        try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
             EscPos escpos = new EscPos(printerOutputStream)) {

            Style title = new Style().setFontSize(Style.FontSize._2, Style.FontSize._2).setJustification(EscPosConst.Justification.Center);
            Style subtitle = new Style().setJustification(EscPosConst.Justification.Center);
            Style bold = new Style().setBold(true);

            String nomeEmpresa = configDAO.getValor(ConfigKeys.EMPRESA_NOME).orElse("CHURRASCARIA");

            escpos.writeLF(title, nomeEmpresa);
            escpos.writeLF(subtitle, "Recibo de Venda");
            escpos.writeLF("------------------------------------------------");
            escpos.writeLF("Venda: " + venda.getId() + " - " + venda.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            escpos.writeLF("------------------------------------------------");

            escpos.writeLF(bold, "ITEM  QTD    VL.UN     TOTAL");
            for (ItemVenda item : itens) {
                String nome = item.getNomeProduto().length() > 15 ? item.getNomeProduto().substring(0, 15) : item.getNomeProduto();
                escpos.writeLF(String.format("%-15s %.3f x %.2f = %.2f", nome, item.getQuantidade(), item.getPrecoUnitario(), item.getTotalItem()));
            }
            escpos.writeLF("------------------------------------------------");
            escpos.writeLF(title, String.format("TOTAL: R$ %.2f", venda.getValorTotal()));
            escpos.writeLF("------------------------------------------------");

            for (Pagamento p : pagamentos) {
                escpos.writeLF(String.format("%-15s R$ %.2f", p.getTipo(), p.getValor()));
            }

            escpos.feed(4);
            escpos.cut(EscPos.CutMode.FULL);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- NOVO MÉTODO: IMPRESSÃO DE FECHAMENTO DE CAIXA ---
    // Assinatura deve bater com o que o CaixaController espera
    public void imprimirRelatorioFechamento(Caixa caixa, double totalVendas, double totalDinheiroSistema, double totalOutros, double diferenca) {
        PrintService printService = getPrintService();
        if (printService == null) {
            System.err.println("Impressora não encontrada para relatório.");
            return;
        }

        try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
             EscPos escpos = new EscPos(printerOutputStream)) {

            Style title = new Style().setFontSize(Style.FontSize._2, Style.FontSize._1).setJustification(EscPosConst.Justification.Center).setBold(true);
            Style center = new Style().setJustification(EscPosConst.Justification.Center);
            Style bold = new Style().setBold(true);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            escpos.writeLF(title, "FECHAMENTO DE CAIXA");
            escpos.writeLF(center, "ID Caixa: " + caixa.getId());
            escpos.writeLF("------------------------------------------------");

            if (caixa.getDataAbertura() != null)
                escpos.writeLF("Abertura:   " + caixa.getDataAbertura().format(fmt));

            // Assume que dataFechamento é agora
            escpos.writeLF("Fechamento: " + java.time.LocalDateTime.now().format(fmt));

            escpos.writeLF("------------------------------------------------");
            escpos.writeLF(bold, "RESUMO FINANCEIRO");
            escpos.writeLF(String.format("Fundo de Troco (+):   R$ %8.2f", caixa.getSaldoInicial()));
            escpos.writeLF(String.format("Total Vendas   (+):   R$ %8.2f", totalVendas));
            escpos.writeLF(String.format("Sangrias       (-):   R$ %8.2f", 0.00)); // Implementar se houver
            escpos.writeLF("------------------------------------------------");

            double saldoEsperado = caixa.getSaldoInicial() + totalDinheiroSistema; // Simplificação: Saldo em gaveta costuma ser troco + dinheiro vendas

            escpos.writeLF(bold, "CONFERENCIA DE GAVETA (Dinheiro)");
            escpos.writeLF(String.format("Sistema (Esp.):       R$ %8.2f", saldoEsperado));
            escpos.writeLF(String.format("Informado (Real):     R$ %8.2f", caixa.getSaldoFinal()));
            escpos.writeLF(String.format("DIFERENCA:            R$ %8.2f", diferenca));

            escpos.writeLF("------------------------------------------------");
            escpos.writeLF(bold, "DETALHAMENTO VENDAS");
            escpos.writeLF(String.format("Dinheiro:             R$ %8.2f", totalDinheiroSistema));
            escpos.writeLF(String.format("Cartao/Pix/Outros:    R$ %8.2f", totalOutros));

            escpos.feed(2);
            escpos.writeLF(center, "_______________________________");
            escpos.writeLF(center, "Assinatura do Operador");

            escpos.feed(4);
            escpos.cut(EscPos.CutMode.FULL);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro ao imprimir relatorio: " + e.getMessage());
        }
    }
}