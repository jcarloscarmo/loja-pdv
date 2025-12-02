package br.com.churrasco.controller;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.service.ImpressoraService;
import br.com.churrasco.util.Navegacao;
import com.fazecast.jSerialComm.SerialPort;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConfiguracoesController {

    // Campos Gerais
    @FXML private TextField txtEmpresaNome;
    @FXML private TextField txtCnpj;
    @FXML private TextField txtEndereco;
    @FXML private TextField txtTelefone;
    @FXML private TextField txtRodape;

    // CheckBoxes
    @FXML private CheckBox chkPrintCnpj;
    @FXML private CheckBox chkPrintEndereco;
    @FXML private CheckBox chkPrintTelefone;
    @FXML private CheckBox chkPrintDataHora;

    // Preview
    @FXML private TextArea txtPreview;

    // Hardware
    @FXML private ComboBox<String> comboImpressoras;
    @FXML private ComboBox<String> comboPortas;
    @FXML private ComboBox<String> comboVelocidade;
    @FXML private Label lblStatus;

    private final ConfigDAO configDAO = new ConfigDAO();

    @FXML
    public void initialize() {
        carregarListasHardware();
        carregarDadosSalvos();
        configurarListenersPreview(); // Ativa a atualização em tempo real
        atualizarPreview(); // Gera o primeiro preview
    }

    // --- LÓGICA DO PREVIEW EM TEMPO REAL ---

    private void configurarListenersPreview() {
        // Adiciona ouvinte em TODOS os campos para atualizar o preview ao digitar/clicar
        txtEmpresaNome.textProperty().addListener((o, old, newV) -> atualizarPreview());
        txtCnpj.textProperty().addListener((o, old, newV) -> atualizarPreview());
        txtEndereco.textProperty().addListener((o, old, newV) -> atualizarPreview());
        txtTelefone.textProperty().addListener((o, old, newV) -> atualizarPreview());
        txtRodape.textProperty().addListener((o, old, newV) -> atualizarPreview());

        chkPrintCnpj.selectedProperty().addListener((o, old, newV) -> atualizarPreview());
        chkPrintEndereco.selectedProperty().addListener((o, old, newV) -> atualizarPreview());
        chkPrintTelefone.selectedProperty().addListener((o, old, newV) -> atualizarPreview());
        chkPrintDataHora.selectedProperty().addListener((o, old, newV) -> atualizarPreview());
    }

    private void atualizarPreview() {
        StringBuilder sb = new StringBuilder();
        int larguraPapel = 32; // Simula 58mm (aprox 32 chars)

        // Cabeçalho Centralizado
        sb.append(centralizar(txtEmpresaNome.getText(), larguraPapel)).append("\n");
        sb.append(centralizar("--------------------------------", larguraPapel)).append("\n");

        sb.append(centralizar("Recibo #001", larguraPapel)).append("\n");

        if (chkPrintDataHora.isSelected()) {
            String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            sb.append(centralizar(dataHora, larguraPapel)).append("\n");
        }

        sb.append(centralizar("--------------------------------", larguraPapel)).append("\n");

        // Dados da Empresa (Opcionais)
        if (chkPrintCnpj.isSelected() && !txtCnpj.getText().isEmpty()) {
            sb.append(centralizar("CNPJ: " + txtCnpj.getText(), larguraPapel)).append("\n");
        }

        if (chkPrintEndereco.isSelected() && !txtEndereco.getText().isEmpty()) {
            // Quebra de linha simples se for muito longo (simulação básica)
            String end = txtEndereco.getText();
            if (end.length() > larguraPapel) {
                sb.append(centralizar(end.substring(0, larguraPapel), larguraPapel)).append("\n");
                sb.append(centralizar(end.substring(larguraPapel), larguraPapel)).append("\n");
            } else {
                sb.append(centralizar(end, larguraPapel)).append("\n");
            }
        }

        if (chkPrintTelefone.isSelected() && !txtTelefone.getText().isEmpty()) {
            sb.append(centralizar("Tel: " + txtTelefone.getText(), larguraPapel)).append("\n");
        }

        // Corpo do Cupom (Exemplo)
        sb.append(centralizar("--------------------------------", larguraPapel)).append("\n");
        sb.append("ITEM                            \n");
        sb.append("QTD x UNIT                 TOTAL\n");
        sb.append("--------------------------------\n");
        sb.append("1 PICANHA IMP.                  \n");
        sb.append("0,500kg x R$120,00      R$ 60,00\n");
        sb.append("................................\n");
        sb.append("2 COCA COLA                     \n");
        sb.append("2 un x R$5,00           R$ 10,00\n");
        sb.append("................................\n");
        sb.append("--------------------------------\n");

        // Totais
        sb.append("TOTAL:                  R$ 70,00\n");
        sb.append("--------------------------------\n");
        sb.append(centralizar("DINHEIRO: R$ 100,00", larguraPapel)).append("\n");
        sb.append("   TROCO:               R$ 30,00\n\n");

        // Rodapé
        sb.append(centralizar("Obrigado pela preferencia!", larguraPapel)).append("\n");
        sb.append(centralizar(txtRodape.getText(), larguraPapel)).append("\n");
        sb.append("\n\n\n"); // Espaço para corte

        txtPreview.setText(sb.toString());
    }

    // Helper para centralizar texto (imita a impressora)
    private String centralizar(String texto, int largura) {
        if (texto == null) return "";
        if (texto.length() >= largura) return texto; // Não cabe, não centraliza
        int padding = (largura - texto.length()) / 2;
        return " ".repeat(padding) + texto;
    }

    // --- RESTANTE DA LÓGICA DE CARREGAMENTO E SALVAMENTO ---

    private void carregarListasHardware() {
        try {
            comboImpressoras.getItems().clear();
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService service : services) comboImpressoras.getItems().add(service.getName());
        } catch (Exception e) { System.out.println("Erro impressoras: " + e.getMessage()); }

        try {
            comboPortas.getItems().clear();
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort port : ports) comboPortas.getItems().add(port.getSystemPortName());
        } catch (Exception e) { System.out.println("Erro COM: " + e.getMessage()); }

        comboVelocidade.getItems().clear();
        comboVelocidade.getItems().addAll("2400", "4800", "9600", "19200");
    }

    private void carregarDadosSalvos() {
        txtEmpresaNome.setText(configDAO.getValor("empresa_nome").orElse(""));
        txtCnpj.setText(configDAO.getValor("empresa_cnpj").orElse(""));
        txtEndereco.setText(configDAO.getValor("empresa_endereco").orElse(""));
        txtTelefone.setText(configDAO.getValor("empresa_telefone").orElse(""));
        txtRodape.setText(configDAO.getValor("rodape_cupom").orElse(""));

        chkPrintCnpj.setSelected(Boolean.parseBoolean(configDAO.getValor("print_cnpj").orElse("true")));
        chkPrintEndereco.setSelected(Boolean.parseBoolean(configDAO.getValor("print_endereco").orElse("true")));
        chkPrintTelefone.setSelected(Boolean.parseBoolean(configDAO.getValor("print_telefone").orElse("true")));
        chkPrintDataHora.setSelected(Boolean.parseBoolean(configDAO.getValor("print_datahora").orElse("true")));

        String imp = configDAO.getValor("impressora_nome").orElse("");
        if (!imp.isEmpty()) comboImpressoras.setValue(imp);

        String porta = configDAO.getValor("balanca_porta").orElse("");
        if (!porta.isEmpty()) comboPortas.setValue(porta);

        String vel = configDAO.getValor("balanca_velocidade").orElse("");
        if (!vel.isEmpty()) comboVelocidade.setValue(vel);
        else comboVelocidade.setValue("2400");

        // Força atualização do preview com os dados carregados
        atualizarPreview();
    }

    @FXML
    public void salvarTudo() {
        try {
            configDAO.salvar("empresa_nome", txtEmpresaNome.getText());
            configDAO.salvar("empresa_cnpj", txtCnpj.getText());
            configDAO.salvar("empresa_endereco", txtEndereco.getText());
            configDAO.salvar("empresa_telefone", txtTelefone.getText());
            configDAO.salvar("rodape_cupom", txtRodape.getText());

            configDAO.salvar("print_cnpj", String.valueOf(chkPrintCnpj.isSelected()));
            configDAO.salvar("print_endereco", String.valueOf(chkPrintEndereco.isSelected()));
            configDAO.salvar("print_telefone", String.valueOf(chkPrintTelefone.isSelected()));
            configDAO.salvar("print_datahora", String.valueOf(chkPrintDataHora.isSelected()));

            if (comboImpressoras.getValue() != null) configDAO.salvar("impressora_nome", comboImpressoras.getValue());
            if (comboPortas.getValue() != null) configDAO.salvar("balanca_porta", comboPortas.getValue());
            if (comboVelocidade.getValue() != null) configDAO.salvar("balanca_velocidade", comboVelocidade.getValue());

            lblStatus.setText("Configurações salvas!");
            mostrarAlerta("Sucesso", "Dados salvos com sucesso.");

        } catch (Exception e) {
            mostrarAlerta("Erro", "Falha ao salvar: " + e.getMessage());
        }
    }

    @FXML
    public void testarImpressora() {
        salvarTudo();
        try {
            ImpressoraService service = new ImpressoraService();
            service.imprimirTeste();
            lblStatus.setText("Teste enviado!");
        } catch (Exception e) {
            mostrarAlerta("Erro Impressora", e.getMessage());
        }
    }

    @FXML
    public void voltarMenu(ActionEvent event) {
        Navegacao.trocarTela(event, "/br/com/churrasco/view/Menu.fxml", "Menu Principal");
    }

    private void mostrarAlerta(String titulo, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}