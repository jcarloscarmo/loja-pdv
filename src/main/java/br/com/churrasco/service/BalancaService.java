package br.com.churrasco.service;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.util.LogUtil;
import com.fazecast.jSerialComm.SerialPort;

public class BalancaService {

    private final ConfigDAO configDAO = new ConfigDAO();

    public Double lerPeso() {
        String portaNome = configDAO.getValor("balanca_porta").orElse(null);

        if (portaNome == null || portaNome.isEmpty() || portaNome.contains("Nenhuma")) {
            return null;
        }

        int velocidade = 9600;
        try {
            String velStr = configDAO.getValor("balanca_velocidade").orElse("9600");
            velocidade = Integer.parseInt(velStr);
        } catch (Exception e) {}

        SerialPort comPort = SerialPort.getCommPort(portaNome);

        try {
            // 1. Tenta Abrir
            boolean abriu = comPort.openPort();
            if (!abriu) return null;

            // 2. Configura
            comPort.setComPortParameters(velocidade, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);

            // 3. Timeout (Leitura rápida)
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);

            // 4. Limpa lixo
            while (comPort.bytesAvailable() > 0) {
                byte[] lixo = new byte[comPort.bytesAvailable()];
                comPort.readBytes(lixo, lixo.length);
            }

            // 5. Envia comando ENQ (0x05)
            byte[] comando = new byte[]{0x05};
            comPort.writeBytes(comando, 1);

            // 6. Lê a resposta
            byte[] buffer = new byte[32];
            int bytesLidos = comPort.readBytes(buffer, buffer.length);

            // 7. Fecha
            comPort.closePort();

            // 8. Processa
            if (bytesLidos > 0) {
                String dados = new String(buffer, 0, bytesLidos);
                return processarPeso(dados);
            }

        } catch (Exception e) {
            LogUtil.registrarErro("Erro balança", e);
            if (comPort.isOpen()) comPort.closePort();
        }

        return null;
    }

    private Double processarPeso(String dados) {
        try {
            // Remove tudo que não é número, ponto ou vírgula
            String limpo = dados.replaceAll("[^0-9,.]", "");

            if (limpo.isEmpty()) return null;

            // Troca vírgula por ponto para o Java entender
            double peso = Double.parseDouble(limpo.replace(",", "."));

            // --- CORREÇÃO INTELIGENTE (O Pulo do Gato) ---
            // Se a balança mandou "66" (achando que é gramas) ou "00066" (sem ponto),
            // o Java leu como 66.0 KG.
            // Ninguém come 60kg de carne. Se passar de 50, com certeza são GRAMAS.
            if (peso > 50) {
                peso = peso / 1000.0;
            }

            // Filtro de segurança final (entre 0g e 50kg)
            if (peso > 0 && peso < 50) {
                return peso;
            }
        } catch (Exception e) {
            // Ignora erro de conversão
        }
        return null;
    }
}