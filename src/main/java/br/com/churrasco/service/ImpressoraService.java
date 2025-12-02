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
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ImpressoraService {

    private final ConfigDAO configDAO = new ConfigDAO();

    // --- CUPOM DE VENDA ---
    public void imprimirCupom(Venda venda, List<ItemVenda> itens, List<Pagamento> pagamentos) {
        try {
            // 1. Tenta pegar o serviço de impressão
            PrintService printService = obterPrintService();

            if (printService == null) {
                System.err.println(">>> ERRO: Nenhuma impressora encontrada (nem configurada, nem padrão).");
                return;
            }

            System.out.println(">>> Imprimindo na: " + printService.getName());

            try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
                 EscPos escpos = new EscPos(printerOutputStream)) {

                // Estilos
                Style titulo = new Style().setFontSize(Style.FontSize._2, Style.FontSize._1).setJustification(EscPosConst.Justification.Center).setBold(true);
                Style centro = new Style().setJustification(EscPosConst.Justification.Center);
                Style normal = new Style().setFontSize(Style.FontSize._1, Style.FontSize._1);
                Style negrito = new Style().setBold(true);

                // Busca nome da empresa (trata Optional caso o DAO esteja usando)
                String nomeEmpresa = getValorConfig("empresa_nome", "CHURRASCARIA");

                // Cabeçalho
                escpos.writeLF(titulo, nomeEmpresa);
                escpos.writeLF(centro, "--------------------------------");
                escpos.writeLF(centro, "Recibo #" + venda.getId());
                escpos.writeLF(centro, venda.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                escpos.writeLF(centro, "--------------------------------");

                // Itens
                for (ItemVenda item : itens) {
                    String nome = item.getNomeProduto();
                    if (nome.length() > 32) nome = nome.substring(0, 32);

                    escpos.writeLF(negrito, nome);

                    String qtd = "KG".equals(item.getProduto().getUnidade()) ?
                            String.format("%.3fkg", item.getQuantidade()) :
                            String.format("%.0f un", item.getQuantidade());

                    String linha = String.format("%s x R$%.2f", qtd, item.getPrecoUnitario());
                    String totalStr = String.format("R$ %.2f", item.getTotalItem());

                    escpos.writeLF(normal, linha);
                    escpos.writeLF(negrito, String.format("%32s", totalStr));
                    escpos.writeLF(normal, "................................");
                }

                escpos.writeLF(centro, "--------------------------------");
                escpos.writeLF(titulo, String.format("TOTAL: R$ %.2f", venda.getValorTotal()));
                escpos.writeLF(centro, "--------------------------------");

                // Pagamentos
                for (Pagamento p : pagamentos) {
                    escpos.writeLF(centro, String.format("%s: R$ %.2f", p.getTipo(), p.getValor()));
                }

                // Troco
                double totalPago = pagamentos.stream().mapToDouble(Pagamento::getValor).sum();
                double troco = totalPago - venda.getValorTotal();
                if (troco > 0.01) {
                    escpos.feed(1);
                    escpos.writeLF(negrito, String.format("   TROCO: R$ %.2f", troco));
                }

                escpos.feed(2);
                escpos.writeLF(centro, getValorConfig("rodape_cupom", "Volte Sempre!"));
                escpos.feed(4);
                escpos.cut(EscPos.CutMode.FULL);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Falha na impressão: " + e.getMessage());
        }
    }

    // --- RELATÓRIO DE FECHAMENTO ---
    public void imprimirRelatorioFechamento(Caixa caixa, double tDin, double tPix, double tDeb, double tCre) {
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

                String dtAbertura = caixa.getDataAbertura() != null ? caixa.getDataAbertura().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "--";
                escpos.writeLF(centro, "Abertura: " + dtAbertura);
                escpos.writeLF(centro, "Fechamento: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
                escpos.writeLF(centro, "--------------------------------");

                escpos.writeLF(negrito, "RESUMO DE VENDAS:");
                escpos.writeLF(normal, String.format("Dinheiro:       R$ %8.2f", tDin));
                escpos.writeLF(normal, String.format("Pix:            R$ %8.2f", tPix));
                escpos.writeLF(normal, String.format("Debito:         R$ %8.2f", tDeb));
                escpos.writeLF(normal, String.format("Credito:        R$ %8.2f", tCre));

                double totalGeral = tDin + tPix + tDeb + tCre;
                escpos.writeLF(negrito, String.format("TOTAL GERAL:    R$ %8.2f", totalGeral));
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- MÉTODO DE TESTE ---
    public void imprimirTeste() {
        try {
            PrintService printService = obterPrintService();
            if (printService == null) {
                System.out.println("Nenhuma impressora disponível para teste.");
                return;
            }

            try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
                 EscPos escpos = new EscPos(printerOutputStream)) {

                Style titulo = new Style().setFontSize(Style.FontSize._2, Style.FontSize._1).setJustification(EscPosConst.Justification.Center);
                escpos.writeLF(titulo, "TESTE DE IMPRESSAO");
                escpos.writeLF(titulo, "OK!");
                escpos.feed(3);
                escpos.cut(EscPos.CutMode.FULL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- HELPER: Busca Impressora de forma robusta ---
    private PrintService obterPrintService() {
        // 1. Tenta ler do banco
        String nomeConfig = getValorConfig("impressora_nome", "");

        if (!nomeConfig.isEmpty()) {
            // Tenta achar pelo nome exato
            PrintService servico = PrinterOutputStream.getPrintServiceByName(nomeConfig);
            if (servico != null) return servico;

            System.out.println(">>> Aviso: Impressora '" + nomeConfig + "' não encontrada. Tentando padrão...");
        }

        // 2. Se falhar ou não tiver config, pega a PADRÃO do sistema
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    // --- HELPER: Compatibilidade DAO (String ou Optional) ---
    private String getValorConfig(String chave, String padrao) {
        try {
            // Tenta tratar como se o DAO retornasse Optional (Java moderno)
            Object retorno = configDAO.getValor(chave);
            if (retorno instanceof Optional) {
                return ((Optional<?>) retorno).map(Object::toString).orElse(padrao);
            }
            // Se o DAO retornar String direta (Java legado/simples)
            if (retorno instanceof String) {
                String s = (String) retorno;
                return s.isEmpty() ? padrao : s;
            }
        } catch (Exception e) {
            System.out.println("Erro ao ler config: " + e.getMessage());
        }
        return padrao;
    }
}