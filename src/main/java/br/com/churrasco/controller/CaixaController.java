package br.com.churrasco.controller;

import br.com.churrasco.dao.CaixaDAO;
import br.com.churrasco.dao.UsuarioDAO; // Importante
import br.com.churrasco.dao.VendaDAO;
import br.com.churrasco.model.Caixa;
import br.com.churrasco.service.ImpressoraService;
import br.com.churrasco.util.Sessao; // Importante
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair; // Para o Dialog

import java.util.Optional;

public class CaixaController {

    @FXML private TextField txtValor;
    @FXML private Label lblSaldoSistema;
    @FXML private Label lblDiferenca;

    private final CaixaDAO caixaDAO = new CaixaDAO();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO(); // Instância do DAO de Usuário
    private final ImpressoraService impressoraService = new ImpressoraService();

    private boolean confirmado = false;
    private Caixa caixaAberto;

    @FXML
    public void initialize() {
        configurarMascaraDinheiro();
        // Tenta limpar sujeira antiga ao abrir
        caixaDAO.verificarEFecharCaixasAntigos();
    }

    // --- MASCARA ---
    private void configurarMascaraDinheiro() {
        if(txtValor == null) return;
        txtValor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            String digitos = newValue.replaceAll("[^0-9]", "");
            if (digitos.isEmpty()) digitos = "0";
            long valorLong = Long.parseLong(digitos);
            double valorDecimal = valorLong / 100.0;
            String formatado = String.format("%.2f", valorDecimal);
            if (!newValue.equals(formatado)) {
                txtValor.setText(formatado);
                txtValor.positionCaret(formatado.length());
            }
        });
    }

    @FXML
    public void confirmarAbertura() {
        try {
            double valor = lerValorMonetario();
            caixaDAO.abrirCaixa(valor);
            this.confirmado = true;
            fecharJanela();
        } catch (Exception e) {
            mostrarAlerta("Valor inválido: " + e.getMessage());
        }
    }

    // --- MÉTODO DE CARREGAMENTO BLINDADO ---
    public void carregarDadosFechamento() {
        // 1. Tenta busca padrão
        this.caixaAberto = caixaDAO.buscarCaixaAberto();

        // 2. PLANO B: Se não achou, pega o último registro do banco e vê se serve
        if (this.caixaAberto == null) {
            Caixa ultimo = caixaDAO.buscarUltimoCaixaQualquerStatus();
            if (ultimo != null && "ABERTO".equalsIgnoreCase(ultimo.getStatus())) {
                this.caixaAberto = ultimo; // Recuperou o caixa perdido
            }
        }

        // 3. Se ainda assim for null, significa que está tudo FECHADO.
        if (caixaAberto == null) {
            mostrarAlerta("Não há nenhum caixa com status ABERTO no sistema.");
            fecharJanela(); // Fecha a tela de fechamento pois não tem o que fechar
            return;
        }

        double vendasDinheiro = caixaDAO.calcularSaldoDinheiroSistema(caixaAberto.getId());
        double totalEsperadoNaGaveta = caixaAberto.getSaldoInicial() + vendasDinheiro;

        caixaAberto.setSaldoFinal(totalEsperadoNaGaveta);
        lblSaldoSistema.setText(String.format("R$ %.2f", totalEsperadoNaGaveta));

        txtValor.textProperty().addListener((obs, old, novo) -> atualizarDiferenca(totalEsperadoNaGaveta));
        Platform.runLater(() -> txtValor.requestFocus());
    }

    private void atualizarDiferenca(double esperado) {
        try {
            double informado = lerValorMonetario();
            double dif = informado - esperado;
            lblDiferenca.setText(String.format("Diferença: R$ %.2f", dif));
            if (dif < -0.01) lblDiferenca.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            else if (dif > 0.01) lblDiferenca.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            else lblDiferenca.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        } catch (Exception e) {}
    }

    @FXML
    public void confirmarFechamento() {
        try {
            // --- PROTEÇÃO 1: CAIXA VÁLIDO ---
            if (caixaAberto == null) {
                mostrarAlerta("ERRO CRÍTICO: O sistema perdeu a referência do caixa.\nFeche esta janela e tente novamente.");
                return;
            }

            // --- PROTEÇÃO 2: VERIFICAÇÃO DE ADMIN (NOVA LÓGICA) ---
            // Se o usuário atual NÃO for admin, pede senha de um admin
            if (!Sessao.getUsuario().isAdmin()) {
                boolean autorizado = solicitarAutorizacaoAdmin();
                if (!autorizado) {
                    return; // Se cancelou ou errou a senha, para aqui.
                }
            }

            // Se passou daqui, ou é Admin ou foi autorizado
            double informado = lerValorMonetario();
            double esperado = caixaAberto.getSaldoFinal();
            double diferenca = informado - esperado;

            caixaDAO.fecharCaixa(caixaAberto.getId(), esperado, informado, diferenca);

            caixaAberto.setSaldoInformado(informado);
            caixaAberto.setDiferenca(diferenca);

            Alert printAlert = new Alert(Alert.AlertType.CONFIRMATION, "Caixa Fechado! Deseja imprimir?", ButtonType.YES, ButtonType.NO);
            if (printAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                imprimirFechamento();
            }

            exibirRelatorioFinal(informado, diferenca);
            this.confirmado = true;
            fecharJanela();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Erro ao fechar: " + e.getMessage());
        }
    }

    // --- POPUP DE AUTORIZAÇÃO (NOVO) ---
    private boolean solicitarAutorizacaoAdmin() {
        // Cria o Dialog customizado
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Autorização de Gerente/Dono");
        dialog.setHeaderText("Apenas administradores podem fechar o caixa.\nPor favor, insira as credenciais.");

        // Botões
        ButtonType loginButtonType = new ButtonType("Autorizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Campos
        ComboBox<String> cmbAdmin = new ComboBox<>();
        cmbAdmin.setPromptText("Selecione o Admin");
        // Carrega usuários para facilitar
        cmbAdmin.getItems().addAll(usuarioDAO.listarNomes());

        PasswordField pwdSenha = new PasswordField();
        pwdSenha.setPromptText("Senha");

        grid.add(new Label("Administrador:"), 0, 0);
        grid.add(cmbAdmin, 1, 0);
        grid.add(new Label("Senha:"), 0, 1);
        grid.add(pwdSenha, 1, 1);

        // Habilita o botão apenas se tiver texto
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        pwdSenha.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty() || cmbAdmin.getValue() == null);
        });

        dialog.getDialogPane().setContent(grid);

        // Foca na senha
        Platform.runLater(cmbAdmin::requestFocus);

        // Converte o resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(cmbAdmin.getValue(), pwdSenha.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if (result.isPresent()) {
            String adminNome = result.get().getKey();
            String adminSenha = result.get().getValue();

            // Valida no DAO
            if (usuarioDAO.validarAdmin(adminNome, adminSenha)) {
                return true; // Sucesso
            } else {
                mostrarAlerta("Senha incorreta ou usuário não é Administrador!");
                return false;
            }
        }

        return false; // Cancelou
    }

    private void imprimirFechamento() {
        // Logica de impressão
    }

    private void exibirRelatorioFinal(double informado, double diferenca) {
        // Logica de exibição do comprovante amarelo (se tiver)
    }

    private double lerValorMonetario() {
        String limpo = txtValor.getText().replaceAll("[^0-9]", "");
        if(limpo.isEmpty()) limpo = "0";
        return Double.parseDouble(limpo) / 100.0;
    }

    private void fecharJanela() {
        if (txtValor.getScene() != null) ((Stage) txtValor.getScene().getWindow()).close();
    }

    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public boolean isConfirmado() { return confirmado; }
}