package br.com.churrasco.controller;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.dao.UsuarioDAO;
import br.com.churrasco.model.Usuario;
import br.com.churrasco.service.ImpressoraService;
import br.com.churrasco.util.LogUtil;
// IMPORTS NOVOS DO JSSC
import jssc.SerialPortList;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConfiguracoesController {

    @FXML private TextField txtEmpresaNome;
    @FXML private TextField txtCnpj;
    @FXML private TextField txtEndereco;
    @FXML private TextField txtTelefone;
    @FXML private TextField txtRodape;
    @FXML private CheckBox chkPrintCnpj;
    @FXML private CheckBox chkPrintEndereco;
    @FXML private CheckBox chkPrintTelefone;
    @FXML private CheckBox chkPrintDataHora;
    @FXML private TextArea txtPreview;

    @FXML private ComboBox<String> comboImpressoras;
    @FXML private ComboBox<String> comboPortas;
    @FXML private ComboBox<String> comboVelocidade;

    @FXML private TextField txtUsuarioNome;
    @FXML private PasswordField txtUsuarioSenha;
    @FXML private ComboBox<String> comboPerfil;
    @FXML private TableView<Usuario> tabelaUsuarios;
    @FXML private TableColumn<Usuario, Integer> colId;
    @FXML private TableColumn<Usuario, String> colUsuarioNome;
    @FXML private TableColumn<Usuario, String> colPerfil;

    @FXML private Label lblStatus;

    private final ConfigDAO configDAO = new ConfigDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private Usuario usuarioEmEdicao = null;

    @FXML
    public void initialize() {
        LogUtil.registrar("SISTEMA", "Iniciando carregamento da tela de Configurações...");

        try { carregarListasHardware(); }
        catch (Throwable e) { LogUtil.registrarErro("Erro fatal ao carregar listas de Hardware", e); }

        try { carregarDadosSalvos(); }
        catch (Exception e) { LogUtil.registrarErro("Erro ao carregar dados do banco", e); }

        try { configurarTabelaUsuarios(); carregarListaUsuarios(); configurarFormularioUsuarios(); }
        catch (Exception e) { LogUtil.registrarErro("Erro ao carregar aba de Usuários", e); }

        try { configurarListenersPreview(); atualizarPreview(); }
        catch (Exception e) { LogUtil.registrarErro("Erro ao iniciar Preview", e); }

        LogUtil.registrar("SISTEMA", "Tela de Configurações aberta.");
    }

    private void carregarListasHardware() {
        // 1. IMPRESSORAS
        try {
            comboImpressoras.getItems().clear();
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            if (services != null) {
                for (PrintService service : services) comboImpressoras.getItems().add(service.getName());
            }
            if (comboImpressoras.getItems().isEmpty()) comboImpressoras.getItems().add("Nenhuma impressora detectada");
        } catch (Throwable e) {
            LogUtil.registrarErro("Falha crítica impressora", e);
            comboImpressoras.getItems().add("Erro driver Impressora");
        }

        // 2. PORTAS SERIAIS (AGORA COM JSSC - MUITO MAIS ESTÁVEL)
        try {
            comboPortas.getItems().clear();
            // JSSC é simples assim: retorna Strings direto
            String[] portNames = SerialPortList.getPortNames();

            if (portNames != null && portNames.length > 0) {
                for (String portName : portNames) {
                    comboPortas.getItems().add(portName);
                }
            } else {
                comboPortas.getItems().add("Nenhuma porta COM encontrada");
            }
        } catch (Throwable e) {
            LogUtil.registrarErro("Falha crítica JSSC Serial", e);
            comboPortas.getItems().add("Erro driver Serial");
        }

        comboVelocidade.getItems().setAll("2400", "4800", "9600", "19200", "38400", "115200");
    }

    // --- MÉTODOS MANTIDOS IDÊNTICOS (PREVIEW, SALVAR, USUÁRIOS) ---
    // (Copiei a lógica exata do seu arquivo anterior para economizar espaço aqui,
    //  mas usando JSSC na listagem acima).

    private void configurarListenersPreview() {
        if (txtEmpresaNome == null) return;
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
        try {
            StringBuilder sb = new StringBuilder();
            int largura = 32;
            String nome = txtEmpresaNome.getText(); if (nome == null || nome.isEmpty()) nome = "NOME DA EMPRESA";
            sb.append(centralizar(nome, largura)).append("\n");
            if (chkPrintDataHora.isSelected()) {
                String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                sb.append(centralizar(data, largura)).append("\n");
            }
            sb.append("--------------------------------\n");
            if (chkPrintCnpj.isSelected() && !txtCnpj.getText().isEmpty()) sb.append(centralizar("CNPJ: " + txtCnpj.getText(), largura)).append("\n");
            if (chkPrintEndereco.isSelected() && !txtEndereco.getText().isEmpty()) sb.append(centralizar(txtEndereco.getText(), largura)).append("\n"); // Simplificado
            if (chkPrintTelefone.isSelected() && !txtTelefone.getText().isEmpty()) sb.append(centralizar("Tel: " + txtTelefone.getText(), largura)).append("\n");
            sb.append("--------------------------------\nITEM             QTD/UN    TOTAL\n1. PICANHA IMP. (KG)            \n   0,500kg x 120,00        60,00\n--------------------------------\nTOTAL R$                   60,00\n\n");
            String rodape = txtRodape.getText(); if (rodape == null || rodape.isEmpty()) rodape = "Volte Sempre!";
            sb.append(centralizar(rodape, largura)).append("\n\n\n");
            txtPreview.setText(sb.toString());
        } catch (Exception e) {}
    }

    private String centralizar(String texto, int largura) {
        if (texto.length() >= largura) return texto.substring(0, largura);
        int espacos = (largura - texto.length()) / 2;
        return " ".repeat(Math.max(0, espacos)) + texto;
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
        if (!imp.isEmpty()) {
            if (comboImpressoras.getItems().contains(imp)) comboImpressoras.setValue(imp);
            else comboImpressoras.setValue(imp + " (Não detectada)");
        }
        String porta = configDAO.getValor("balanca_porta").orElse("");
        if (!porta.isEmpty()) {
            if (comboPortas.getItems().contains(porta)) comboPortas.setValue(porta);
            else { comboPortas.getItems().add(porta); comboPortas.setValue(porta); }
        }
        String vel = configDAO.getValor("balanca_velocidade").orElse("");
        if (!vel.isEmpty()) comboVelocidade.setValue(vel); else comboVelocidade.setValue("9600");
    }

    @FXML public void salvarTudo() {
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
            if (comboImpressoras.getValue() != null) configDAO.salvar("impressora_nome", comboImpressoras.getValue().replace(" (Não detectada)", ""));
            if (comboPortas.getValue() != null) configDAO.salvar("balanca_porta", comboPortas.getValue());
            if (comboVelocidade.getValue() != null) configDAO.salvar("balanca_velocidade", comboVelocidade.getValue());
            lblStatus.setText("Configurações salvas!");
            mostrarAlerta("Sucesso", "Dados salvos com sucesso.");
        } catch (Exception e) {
            LogUtil.registrarErro("Erro ao salvar", e);
            mostrarAlerta("Erro", "Falha ao salvar: " + e.getMessage());
        }
    }

    // --- USUÁRIOS (Idêntico ao anterior) ---
    private void configurarTabelaUsuarios() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsuarioNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colPerfil.setCellValueFactory(new PropertyValueFactory<>("perfil"));
        tabelaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, old, novo) -> {
            if (novo != null) { usuarioEmEdicao = novo; txtUsuarioNome.setText(novo.getNome()); txtUsuarioSenha.setText(novo.getSenha()); comboPerfil.setValue(novo.getPerfil()); }
        });
    }
    private void carregarListaUsuarios() { try { tabelaUsuarios.setItems(FXCollections.observableArrayList(usuarioDAO.listarTodos())); } catch (Exception e) { LogUtil.registrarErro("Erro listar users", e); } }
    private void configurarFormularioUsuarios() { comboPerfil.setItems(FXCollections.observableArrayList("DONO", "ATENDENTE")); comboPerfil.getSelectionModel().selectFirst(); }
    @FXML public void salvarUsuario() { if (txtUsuarioNome.getText().isEmpty()) return; Usuario u = new Usuario(0, txtUsuarioNome.getText(), txtUsuarioSenha.getText(), comboPerfil.getValue()); try { if (usuarioEmEdicao == null) usuarioDAO.salvar(u); else { u.setId(usuarioEmEdicao.getId()); usuarioDAO.atualizar(u); } limparFormUsuario(); carregarListaUsuarios(); lblStatus.setText("Usuário salvo!"); } catch (Exception e) { LogUtil.registrarErro("Erro salvar user", e); mostrarAlerta("Erro", e.getMessage()); } }
    @FXML public void excluirUsuario() { Usuario u = tabelaUsuarios.getSelectionModel().getSelectedItem(); if (u != null) { try { usuarioDAO.excluir(u.getId()); limparFormUsuario(); carregarListaUsuarios(); } catch(Exception e){ LogUtil.registrarErro("Erro excluir user", e); mostrarAlerta("Erro", e.getMessage()); } } }
    @FXML public void limparFormUsuario() { usuarioEmEdicao = null; txtUsuarioNome.clear(); txtUsuarioSenha.clear(); tabelaUsuarios.getSelectionModel().clearSelection(); }
    @FXML public void testarImpressora() { salvarTudo(); try { new ImpressoraService().imprimirTeste(); } catch(Exception e){ LogUtil.registrarErro("Erro teste print", e); mostrarAlerta("Erro Impressão", "Falha: " + e.getMessage()); } }
    @FXML public void voltarMenu(ActionEvent event) { try { Parent root = FXMLLoader.load(getClass().getResource("/br/com/churrasco/view/Menu.fxml")); Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); stage.setScene(new Scene(root)); stage.setMaximized(true); } catch(Exception e) { LogUtil.registrarErro("Erro menu", e); } }
    private void mostrarAlerta(String t, String m) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(m); a.showAndWait(); }
}