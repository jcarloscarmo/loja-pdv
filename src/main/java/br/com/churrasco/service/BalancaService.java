package br.com.churrasco.service;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.io.OutputStream;

public class BalancaService {

    // Configurações Otimizadas (Toledo 9600)
    private static final String PORTA = "COM5";
    private static final int VELOCIDADE = 9600; // <--- Mais rápido

    private static final byte[] COMANDO_LEITURA = {0x05};

    public Double lerPeso() {
        SerialPort comPort = null;
        try {
            comPort = SerialPort.getCommPort(PORTA);
            comPort.setBaudRate(VELOCIDADE);
            comPort.setNumDataBits(8);
            comPort.setNumStopBits(1);
            comPort.setParity(SerialPort.NO_PARITY);

            // Timeout curto para não travar o sistema
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 500, 500);

            if (comPort.openPort()) {
                OutputStream out = comPort.getOutputStream();
                InputStream in = comPort.getInputStream();

                out.write(COMANDO_LEITURA);
                out.flush();

                // Espera reduzida para resposta quase instantânea
                Thread.sleep(50);

                if (comPort.bytesAvailable() > 0) {
                    byte[] buffer = new byte[32];
                    int len = in.read(buffer);

                    if (len > 0) {
                        String resposta = new String(buffer, 0, len);
                        String pesoLimpo = resposta.replaceAll("[^0-9]", "");

                        if (!pesoLimpo.isEmpty()) {
                            double valorBruto = Double.parseDouble(pesoLimpo);
                            return valorBruto / 1000.0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignora erro de comunicação para não travar a tela
        } finally {
            if (comPort != null && comPort.isOpen()) {
                comPort.closePort();
            }
        }
        return 0.0;
    }
}