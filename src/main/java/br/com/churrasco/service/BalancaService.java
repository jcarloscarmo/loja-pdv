package br.com.churrasco.service;

import br.com.churrasco.dao.ConfigDAO; // Importe o DAO
import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.io.OutputStream;

public class BalancaService {

    // REMOVI AS CONSTANTES FIXAS (PORTA/VELOCIDADE)

    private static final byte[] COMANDO_LEITURA = {0x05};
    private final ConfigDAO configDAO = new ConfigDAO(); // Instancia o DAO

    public Double lerPeso() {
        // BUSCA DO BANCO DE DADOS AGORA!
        // Se não achar, usa COM5 e 9600 como padrão
        String porta = configDAO.getValor("balanca_porta").orElse("COM5");
        int velocidade = Integer.parseInt(configDAO.getValor("balanca_velocidade").orElse("9600"));

        SerialPort comPort = null;
        try {
            comPort = SerialPort.getCommPort(porta); // Usa a variável
            comPort.setBaudRate(velocidade);         // Usa a variável
            comPort.setNumDataBits(8);
            comPort.setNumStopBits(1);
            comPort.setParity(SerialPort.NO_PARITY);

            // ... (Restante do código igual) ...

            // Timeout curto
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 500, 500);

            if (comPort.openPort()) {
                OutputStream out = comPort.getOutputStream();
                InputStream in = comPort.getInputStream();

                out.write(COMANDO_LEITURA);
                out.flush();
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
            // ...
        } finally {
            if (comPort != null && comPort.isOpen()) {
                comPort.closePort();
            }
        }
        return 0.0;
    }
}