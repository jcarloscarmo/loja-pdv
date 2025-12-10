package br.com.churrasco;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class TesteBalancaReal {

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("⚖️ TESTE DE COMUNICAÇÃO - TOLEDO PRIX 3");
        System.out.println("=========================================");

        // 1. LISTAR PORTAS DISPONÍVEIS (Para garantir que o Java está vendo a COM3)
        System.out.println("\n🔍 Varrendo portas seriais...");
        SerialPort[] ports = SerialPort.getCommPorts();
        boolean achou = false;
        for (SerialPort p : ports) {
            System.out.println("   -> Encontrada: " + p.getSystemPortName() + " (" + p.getDescriptivePortName() + ")");
            if (p.getSystemPortName().equalsIgnoreCase("COM5")) {
                achou = true;
            }
        }

        if (!achou) {
            System.out.println("\n❌ ERRO CRÍTICO: A porta 'COM3' não foi listada pelo Java.");
            System.out.println("   Verifique se o cabo está conectado ou se o driver USB/Serial está instalado.");
            return;
        }

        // 2. CONFIGURAR CONEXÃO
        System.out.println("\n🔌 Conectando na COM3 a 4800 baud...");
        SerialPort comPort = SerialPort.getCommPort("COM5");
        comPort.setBaudRate(4800);
        comPort.setNumDataBits(8);
        comPort.setNumStopBits(1);
        comPort.setParity(SerialPort.NO_PARITY);

        // Timeout é vital: espera até 2 segundos para ler ou escrever
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 2000, 2000);

        if (!comPort.openPort()) {
            System.out.println("❌ FALHA: Não foi possível abrir a porta. Ela pode estar em uso por outro programa.");
            return;
        }
        System.out.println("✅ Porta aberta com sucesso!");

        // 3. LOOP DE TENTATIVAS
        try {
            OutputStream out = comPort.getOutputStream();
            InputStream in = comPort.getInputStream();

            // Vamos tentar ler 5 vezes
            for (int i = 1; i <= 5; i++) {
                System.out.println("\n--- Tentativa " + i + " de 5 ---");

                // COMANDO P03 (Toledo): Envia o byte 0x05 (ENQ)
                System.out.print("📡 Enviando comando ENQ (0x05)... ");
                out.write(0x05);
                out.flush();
                System.out.println("OK.");

                // Aguarda resposta
                Thread.sleep(200);

                // Lê resposta
                if (comPort.bytesAvailable() > 0 || true) { // Força tentativa de leitura devido ao modo Blocking
                    byte[] buffer = new byte[32];
                    int len = in.read(buffer);

                    if (len > 0) {
                        String recebido = new String(buffer, 0, len);
                        System.out.println("📥 RECEBIDO (RAW): " + Arrays.toString(Arrays.copyOf(buffer, len)));
                        System.out.println("⚖️ RECEBIDO (TEXTO): [" + recebido + "]");

                        // Tenta limpar e mostrar o peso
                        String pesoLimpo = recebido.replaceAll("[^0-9.]", "");
                        System.out.println("💡 Peso Identificado: " + pesoLimpo);

                        // Se leu com sucesso, podemos parar o teste
                        // break;
                    } else {
                        System.out.println("⚠️ Porta aberta, comando enviado, mas NADA voltou (Timeout).");
                    }
                }

                Thread.sleep(1000); // Espera 1 seg antes de tentar de novo
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (comPort.isOpen()) {
                comPort.closePort();
                System.out.println("\n🔒 Porta fechada.");
            }
        }
    }
}