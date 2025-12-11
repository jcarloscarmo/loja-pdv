package br.com.churrasco.controller;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.dao.UsuarioDAO;
import br.com.churrasco.model.Usuario;
import br.com.churrasco.service.ImpressoraService;
import br.com.churrasco.util.Navegacao;
import com.fazecast.jSerialComm.SerialPort;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConfiguracoesController {

    // --- ABA 1: DADOS DA EMPRESA ---
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

    // --- ABA 2: HARDWARE ---
    @FXML private ComboBox<String> comboImpressoras;
    @FXML private ComboBox<String> comboPortas;
    @FXML private ComboBox<String> comboVelocidade;

    // --- ABA 3: USUÁRIOS ---
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
        carregarListasHardware();
        carregarDadosSalvos();

        // Configurações de Usuário
        configurarTabelaUsuarios();
        carregarListaUsuarios();
        configurarFormularioUsuarios();

        // ATIVA O PREVIEW EM TEMPO REAL
        configurarListenersPreview();
        atualizarPreview();
    }

    // =================================================================================
    //                             LÓGICA DO PREVIEW (SIMULAÇÃO)
    // =================================================================================

    private void configurarListenersPreview() {
        // Qualquer tecla digitada ou checkbox clicado chama o atualizarPreview()
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
        int largura = 32; // Largura aproximada de 58mm

        // 1. NOME DA EMPRESA (Sempre aparece)
        String nome = txtEmpresaNome.getText();
        if (nome.isEmpty()) nome = "NOME DA EMPRESA";
        sb.append(centralizar(nome, largura)).append("\n");

        // 2. DATA/HORA (Opcional)
        if (chkPrintDataHora.isSelected()) {
            String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            sb.append(centralizar(data, largura)).append("\n");
        }

        sb.append("--------------------------------\n");

        // 3. CNPJ (Opcional)
        if (chkPrintCnpj.isSelected() && !txtCnpj.getText().isEmpty()) {
            sb.append(centralizar("CNPJ: " + txtCnpj.getText(), largura)).append("\n");
        }

        // 4. ENDEREÇO (Opcional)
        if (chkPrintEndereco.isSelected() && !txtEndereco.getText().isEmpty()) {
            String end = txtEndereco.getText();
            // Quebra de linha simples para simular
            if (end.length() > largura) {
                sb.append(centralizar(end.substring(0, largura), largura)).append("\n");
                sb.append(centralizar(end.substring(largura), largura)).append("\n");
            } else {
                sb.append(centralizar(end, largura)).append("\n");
            }
        }

        // 5. TELEFONE (Opcional)
        if (chkPrintTelefone.isSelected() && !txtTelefone.getText().isEmpty()) {
            sb.append(centralizar("Tel: " + txtTelefone.getText(), largura)).append("\n");
        }

        sb.append("--------------------------------\n");

        // CORPO DE EXEMPLO (Fixo, só pra ilustrar)
        sb.append("ITEM             QTD/UN    TOTAL\n");
        sb.append("1. PICANHA IMP. (KG)            \n");
        sb.append("   0,500kg x 120,00        60,00\n");
        sb.append("--------------------------------\n");
        sb.append(formatarLinhaTotal("TOTAL R$", "60,00", largura)).append("\n");
        sb.append("\n");

        // 6. RODAPÉ (Personalizável)
        String rodape = txtRodape.getText();
        if (rodape.isEmpty()) rodape = "Volte Sempre!";
        sb.append(centralizar(rodape, largura)).append("\n");

        sb.append("\n\n"); // Espaço de corte

        txtPreview.setText(sb.toString());
    }

    private String centralizar(String texto, int largura) {
        if (texto.length() >= largura) return texto;
        int espacos = (largura - texto.length()) / 2;
        return " ".repeat(espacos) + texto;
    }

    private String formatarLinhaTotal(String label, String valor, int largura) {
        int espacos = largura - label.length() - valor.length();
        return label + " ".repeat(Math.max(0, espacos)) + valor;
    }

    // =================================================================================
    //                             SALVAR E CARREGAR
    // =================================================================================

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
        else comboVelocidade.setValue("9600");

        atualizarPreview(); // Garante que o preview mostre o que está no banco ao abrir
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

    // --- MÉTODOS DE HARDWARE E USUÁRIOS (MANTIDOS DO ANTERIOR) ---
    private void carregarListasHardware() {
        try {
            comboImpressoras.getItems().clear();
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService service : services) comboImpressoras.getItems().add(service.getName());
        } catch (Exception e) {}
        try {
            comboPortas.getItems().clear();
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort port : ports) comboPortas.getItems().add(port.getSystemPortName());
        } catch (Exception e) {}
        comboVelocidade.getItems().setAll("2400", "4800", "9600", "19200");
    }

    private void carregarListaUsuarios() {
        try { tabelaUsuarios.setItems(FXCollections.observableArrayList(usuarioDAO.listarTodos())); }
        catch (Exception e) {}
    }

    private void configuringTabelaUsuarios() { /* ... código anterior ... */ }
    // ... Mantenha salvarUsuario, excluirUsuario, limparFormUsuario, etc ...
    // Vou resumir para caber, mas você já tem a lógica de usuário completa na resposta anterior.
    // Se precisar que eu reenvie a parte de usuário, avise.

    // --- USUÁRIOS (RESUMIDO PARA NÃO REPETIR O ANTERIOR QUE ESTAVA CERTO) ---
    private void configurarTabelaUsuarios() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsuarioNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colPerfil.setCellValueFactory(new PropertyValueFactory<>("perfil"));
        tabelaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, old, novo) -> {
            if (novo != null) { usuarioEmEdicao = novo; txtUsuarioNome.setText(novo.getNome()); txtUsuarioSenha.setText(novo.getSenha()); comboPerfil.setValue(novo.getPerfil()); }
        });
    }
    private void configurarFormularioUsuarios() { comboPerfil.setItems(FXCollections.observableArrayList("DONO", "ATENDENTE")); comboPerfil.getSelectionModel().selectFirst(); }
    @FXML public void salvarUsuario() {
        if (txtUsuarioNome.getText().isEmpty() || txtUsuarioSenha.getText().isEmpty()) return;
        Usuario u = new Usuario(0, txtUsuarioNome.getText(), txtUsuarioSenha.getText(), comboPerfil.getValue());
        try {
            if (usuarioEmEdicao == null) usuarioDAO.salvar(u);
            else { u.setId(usuarioEmEdicao.getId()); usuarioDAO.atualizar(u); }
            limparFormUsuario(); carregarListaUsuarios(); lblStatus.setText("Usuário salvo!");
        } catch (Exception e) { mostrarAlerta("Erro", e.getMessage()); }
    }
    @FXML public void excluirUsuario() {
        Usuario u = tabelaUsuarios.getSelectionModel().getSelectedItem();
        if (u != null) { try { usuarioDAO.excluir(u.getId()); limparFormUsuario(); carregarListaUsuarios(); } catch(Exception e){ mostrarAlerta("Erro", e.getMessage()); } }
    }
    @FXML public void limparFormUsuario() { usuarioEmEdicao = null; txtUsuarioNome.clear(); txtUsuarioSenha.clear(); tabelaUsuarios.getSelectionModel().clearSelection(); }

    @FXML public void testarImpressora() { salvarTudo(); try { new ImpressoraService().imprimirTeste(); } catch(Exception e){} }
    @FXML public void voltarMenu(ActionEvent event) { Navegacao.trocarTela(event, "/br/com/churrasco/view/Menu.fxml", "Menu Principal"); }
    private void mostrarAlerta(String t, String m) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(m); a.showAndWait(); }
}