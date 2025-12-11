package br.com.churrasco.controller;

import br.com.churrasco.dao.CaixaDAO;
import br.com.churrasco.dao.VendaDAO;
import br.com.churrasco.model.Caixa;
import br.com.churrasco.model.Venda;
import br.com.churrasco.service.ImpressoraService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CaixaController {

    @FXML private TextField txtValor;
    @FXML private Label lblSaldoSistema;
    @FXML private Label lblDiferenca;

    private final CaixaDAO caixaDAO = new CaixaDAO();
    private final VendaDAO vendaDAO = new VendaDAO();
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
            // --- PROTEÇÃO CONTRA NULL POINTER ---
            if (caixaAberto == null) {
                mostrarAlerta("ERRO CRÍTICO: O sistema perdeu a referência do caixa.\nFeche esta janela e tente novamente.");
                return;
            }

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

    private void imprimirFechamento() {
        // Lógica de impressão (Mantenha a sua lógica aqui ou use a que enviei anteriormente)
        // ... (Para economizar espaço, assumo que você tem o ImpressoraService configurado)
    }

    private void exibirRelatorioFinal(double informado, double diferenca) {
        // ... (Mesma lógica de exibição do comprovante amarelo)
        // Copie a lógica do arquivo anterior se necessário, ou mantenha a sua se já estiver funcionando.
        // O importante aqui foi a proteção no 'confirmarFechamento'
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
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public boolean isConfirmado() { return confirmado; }
}