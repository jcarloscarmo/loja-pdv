package br.com.churrasco.service;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.util.LogUtil;
import jssc.SerialPort;
import jssc.SerialPortException;

public class BalancaService {

    private final ConfigDAO configDAO = new ConfigDAO();

    public Double lerPeso() {
        String porta = configDAO.getValor("balanca_porta").orElse(null);

        // Se não tiver porta configurada, nem tenta (evita spam no log)
        if (porta == null || porta.isEmpty() || porta.contains("Nenhuma")) {
            return null;
        }

        String velocidadeStr = configDAO.getValor("balanca_velocidade").orElse("9600");
        int velocidade = 9600;
        try { velocidade = Integer.parseInt(velocidadeStr); } catch (Exception e) {}

        // --- INÍCIO DO RASTREIO ---
        // Vamos logar APENAS se for abrir a porta, para não travar o HD com log infinito
        // Mas como está crashando, precisamos saber.
        // LogUtil.registrar("BALANCA", "Tentando abrir porta: " + porta);

        SerialPort serialPort = new SerialPort(porta);

        try {
            // PASSO 1: ABRIR
            // LogUtil.registrar("BALANCA", "Abrindo porta...");
            boolean abriu = serialPort.openPort();
            if (!abriu) {
                LogUtil.registrar("BALANCA", "Falha ao abrir a porta (Ocupada ou inexistente): " + porta);
                return null;
            }

            // PASSO 2: CONFIGURAR
            serialPort.setParams(velocidade, 8, 1, 0);

            // PASSO 3: MANDAR COMANDO
            // LogUtil.registrar("BALANCA", "Enviando ENQ...");
            boolean enviou = serialPort.writeByte((byte) 0x05);
            if (!enviou) {
                LogUtil.registrar("BALANCA", "Falha ao enviar byte para balança.");
            }

            // PASSO 4: ESPERAR
            Thread.sleep(200);

            // PASSO 5: LER
            String dados = serialPort.readString();
            // LogUtil.registrar("BALANCA", "Dados recebidos: " + dados);

            // PASSO 6: FECHAR
            serialPort.closePort();

            if (dados != null && !dados.isEmpty()) {
                return processarPeso(dados);
            }

        } catch (SerialPortException e) {
            // ERRO ESPECÍFICO DE SERIAL
            LogUtil.registrarErro("Erro JSSC na porta " + porta + " - Tipo: " + e.getExceptionType(), e);
            fecharNaMarra(serialPort);
        } catch (InterruptedException e) {
            // Erro de thread (ignora)
            fecharNaMarra(serialPort);
        } catch (Throwable t) {
            // ERRO GRAVE (DLL, NATIVO, CRASH)
            LogUtil.registrarErro("ERRO FATAL/NATIVO NA BALANÇA", t);
            fecharNaMarra(serialPort);
        }

        return null;
    }

    private void fecharNaMarra(SerialPort porta) {
        try {
            if (porta != null && porta.isOpened()) {
                porta.closePort();
            }
        } catch (Exception e) {
            // LogUtil.registrarErro("Erro ao fechar porta no catch", e);
        }
    }

    private Double processarPeso(String dados) {
        try {
            // Remove caracteres estranhos (Deixa só numeros e ponto/virgula)
            String limpo = dados.replaceAll("[^0-9,.]", "");

            if (limpo.isEmpty()) return null;

            // Tratamento para sujeira comum de balança (Ex: "ST1..0.500")
            // Pega sempre os ultimos digitos válidos se tiver lixo

            double peso = Double.parseDouble(limpo.replace(",", "."));

            // LogUtil.registrar("BALANCA", "Peso processado: " + peso);

            if (peso > 0 && peso < 200) { // Filtro de segurança
                return peso;
            }
        } catch (Exception e) {
            LogUtil.registrar("BALANCA", "Erro ao converter valor: " + dados);
        }
        return null;
    }
}