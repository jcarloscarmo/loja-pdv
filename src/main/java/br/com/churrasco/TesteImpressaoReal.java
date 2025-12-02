package br.com.churrasco;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javax.print.PrintService;

public class TesteImpressaoReal {
    public static void main(String[] args) {
        // O nome exato que você descobriu
        String nomeImpressora = "POS58MM";

        System.out.println("1. Tentando localizar: " + nomeImpressora);
        PrintService service = PrinterOutputStream.getPrintServiceByName(nomeImpressora);

        if (service == null) {
            System.out.println("❌ ERRO: Impressora não encontrada no Windows.");
            return;
        }
        System.out.println("✅ Impressora encontrada! Enviando dados...");

        try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(service);
             EscPos escpos = new EscPos(printerOutputStream)) {

            Style style = new Style().setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setJustification(EscPosConst.Justification.Center);

            escpos.writeLF(style, "TESTE DE IMPRESSAO");
            escpos.writeLF("--------------------------------");
            escpos.writeLF("Se voce esta lendo isso,");
            escpos.writeLF("o Java funcionou!");
            escpos.writeLF("--------------------------------");
            escpos.feed(4);
            escpos.cut(EscPos.CutMode.FULL);

            System.out.println("✅ Comando enviado para o Spooler.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}