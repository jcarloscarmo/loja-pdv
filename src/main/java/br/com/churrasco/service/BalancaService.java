package br.com.churrasco.service;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.util.ConfigKeys;
import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import java.io.InputStream;

public class BalancaService {

    private final ConfigDAO configDAO;
    private SerialPort serialPort;

    @Getter
    private boolean conectada = false;

    public BalancaService() {
        this.configDAO = new ConfigDAO();
    }

    /**
     * Busca a porta no banco e abre a conexão.
     * Deve ser chamado no initialize() do PDVController ou ao abrir o sistema.
     */
    public void conectar() throws Exception {
        String portaNome = configDAO.getValor(ConfigKeys.BALANCA_PORTA)
                .orElseThrow(() -> new Exception("Porta da balança não configurada."));

        // Se já estiver aberta na mesma porta, não faz nada
        if (conectada && serialPort != null && serialPort.getSystemPortName().equals(portaNome)) {
            return;
        }

        desconectar(); // Fecha anterior se houver

        serialPort = SerialPort.getCommPort(portaNome);
        serialPort.setBaudRate(9600); // Toledo geralmente é 9600 ou 4800
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);
        // Timeout é crucial para o read() não travar a UI se a balança não responder
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 200, 0);

        if (serialPort.openPort()) {
            this.conectada = true;
        } else {
            throw new Exception("Não foi possível abrir a porta " + portaNome);
        }
    }

    public void desconectar() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
        this.conectada = false;
    }

    /**
     * Lê o peso da porta que JÁ está aberta.
     * Não recebe argumentos.
     */
    public Double lerPeso() throws Exception {
        if (!conectada || serialPort == null) {
            conectar(); // Tenta reconectar automaticamente (fail-safe)
        }

        // Exemplo simplificado para Toledo (Protocolo P03 envia string tipo "000.560")
        // O ideal é enviar o comando de solicitação (ENQ - 0x05) se a balança não enviar continuo

        // 1. Limpar buffer de entrada (para não ler peso velho)
        while (serialPort.bytesAvailable() > 0) {
            byte[] lixo = new byte[serialPort.bytesAvailable()];
            serialPort.readBytes(lixo, lixo.length);
        }

        // 2. Escrever comando (se necessário). Toledo P03 geralmente envia com Ctrl+E (0x05)
        // serialPort.writeBytes(new byte[]{0x05}, 1);

        // 3. Ler resposta
        InputStream in = serialPort.getInputStream();
        byte[] buffer = new byte[30]; // Tamanho suficiente para o frame

        // Lê os bytes disponíveis (aguarda até 200ms pelo timeout definido acima)
        int len = in.read(buffer);

        if (len > 0) {
            String dados = new String(buffer, 0, len);
            // Aqui entra o parser específico.
            // Exemplo Toledo: STX + PESO + ETX.
            // Vamos assumir que recebemos uma string limpa ou precisamos filtrar digitos

            return interpretarStringToledo(dados);
        }

        return 0.0;
    }

    private Double interpretarStringToledo(String dados) {
        try {
            // Remove tudo que não é número ou ponto
            // Toledo geralmente manda algo como: " 001.230 "
            String limpa = dados.replaceAll("[^0-9.]", "");
            if (limpa.isEmpty()) return 0.0;
            return Double.parseDouble(limpa);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}