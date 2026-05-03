package br.com.churrasco.service;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.model.Caixa;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import br.com.churrasco.model.Venda;
import br.com.churrasco.util.CupomGenerator;
import br.com.churrasco.util.FechamentoCaixaFormatter;
import br.com.churrasco.util.LogUtil;
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

                Style normal = new Style().setFontSize(Style.FontSize._1, Style.FontSize._1);
                for (String linha : CupomGenerator.gerarLinhasVenda(venda, itens, pagamentos)) {
                    escpos.writeLF(normal, linha);
                }
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
                Style normal = new Style().setFontSize(Style.FontSize._1, Style.FontSize._1);

                FechamentoCaixaFormatter.ResumoFechamento resumo = new FechamentoCaixaFormatter.ResumoFechamento(
                        tDin,
                        tPix,
                        tDeb,
                        tCre,
                        tDesc,
                        tDin + tPix + tDeb + tCre
                );

                for (String linha : FechamentoCaixaFormatter.gerarLinhas(caixa, resumo)) {
                    escpos.writeLF(normal, linha);
                }
                escpos.feed(4);
                escpos.cut(EscPos.CutMode.FULL);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void imprimirTeste() {
        try {
            PrintService printService = obterPrintService();
            if (printService == null) return;

            try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
                 EscPos escpos = new EscPos(printerOutputStream)) {
                Style normal = new Style().setFontSize(Style.FontSize._1, Style.FontSize._1);
                for (String linha : CupomGenerator.gerarLinhasPreview(CupomGenerator.carregarConfiguracao())) {
                    escpos.writeLF(normal, linha);
                }
                escpos.feed(4);
                escpos.cut(EscPos.CutMode.FULL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Auxiliares
    private PrintService obterPrintService() {
        String nomeConfig = getValorConfig("impressora_nome", "");
        if (!nomeConfig.isEmpty()) {
            PrintService servico = PrinterOutputStream.getPrintServiceByName(nomeConfig);
            if (servico != null) return servico;
            LogUtil.registrarErro("Impressora não encontrada: '" + nomeConfig + "'", null);
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
