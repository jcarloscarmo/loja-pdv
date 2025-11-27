package br.com.churrasco.controller;

import br.com.churrasco.model.Pagamento;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PagamentoController {

    @FXML private VBox rootPane;
    @FXML private Label lblTotal, lblFalta, lblTroco, lblMetodoSelecionado;
    @FXML private TextField txtValor;
    @FXML private Button btnConcluir, btnAddPagamento;

    @FXML private Button btnDinheiro, btnDebito, btnCredito, btnPix;

    @FXML private TableView<Pagamento> tabelaPagamentos;
    @FXML private TableColumn<Pagamento, String> colTipoPagamento;
    @FXML private TableColumn<Pagamento, Double> colValorPagamento;

    private double valorTotalVenda;
    private double totalPago = 0.0;
    private boolean confirmado = false;
    private String metodoAtual = null;

    private ObservableList<Pagamento> listaPagamentos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colTipoPagamento.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colValorPagamento.setCellValueFactory(new PropertyValueFactory<>("valor"));
        tabelaPagamentos.setItems(listaPagamentos);

        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::processarTeclaGlobal);

        // Configura a máscara que formata o dinheiro enquanto digita
        configurarMascaraMonetaria();

        txtValor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) confirmarPagamentoAtual();
            else if (event.getCode() == KeyCode.ESCAPE) resetarParaEstadoDeEspera();
        });

        btnConcluir.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) confirmarEFechar();
        });

        rootPane.setFocusTraversable(true);
        resetarParaEstadoDeEspera();
    }

    // --- MÁSCARA "ESTILO ATM" ---
    private void configurarMascaraMonetaria() {
        txtValor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            // 1. Remove tudo que não for número (ex: tira R$, pontos e virgulas)
            String digitos = newValue.replaceAll("[^0-9]", "");

            // 2. Remove zeros à esquerda desnecessários
            if (digitos.isEmpty()) digitos = "0";

            // 3. Transforma em valor decimal (divide por 100)
            long valorLong = Long.parseLong(digitos);
            double valorDecimal = valorLong / 100.0;

            // 4. Formata visualmente (Ex: 12.5 -> "12,50")
            String formatado = String.format("%.2f", valorDecimal);

            // 5. Atualiza o texto apenas se mudou (para evitar loop infinito)
            if (!newValue.equals(formatado)) {
                txtValor.setText(formatado);
                txtValor.positionCaret(formatado.length()); // Joga o cursor pro final
            }
        });
    }

    private void processarTeclaGlobal(KeyEvent event) {
        if (txtValor.isFocused() && !txtValor.isDisabled()) return;

        KeyCode code = event.getCode();

        if (code == KeyCode.DIGIT1 || code == KeyCode.NUMPAD1) {
            event.consume();
            selecionarDinheiro();
        } else if (code == KeyCode.DIGIT2 || code == KeyCode.NUMPAD2) {
            event.consume();
            selecionarDebito();
        } else if (code == KeyCode.DIGIT3 || code == KeyCode.NUMPAD3) {
            event.consume();
            selecionarCredito();
        } else if (code == KeyCode.DIGIT4 || code == KeyCode.NUMPAD4) {
            event.consume();
            selecionarPix();
        } else if (code == KeyCode.ESCAPE) {
            ((Stage) rootPane.getScene().getWindow()).close();
        }
    }

    private void resetarParaEstadoDeEspera() {
        this.metodoAtual = null;

        String styleCinza = "-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-font-weight: bold;";
        btnDinheiro.setStyle(styleCinza);
        btnDebito.setStyle(styleCinza);
        btnCredito.setStyle(styleCinza);
        btnPix.setStyle(styleCinza);

        lblMetodoSelecionado.setText("SELECIONE UMA FORMA (1-4)");
        lblMetodoSelecionado.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");

        txtValor.setText("");
        txtValor.setPromptText("");
        txtValor.setDisable(true);
        btnAddPagamento.setDisable(true);

        rootPane.requestFocus();
    }

    private void ativarModoDigitacao(String tipo, Button btnAtivo) {
        this.metodoAtual = tipo;

        btnAtivo.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        lblMetodoSelecionado.setText("PAGANDO COM: " + tipo);
        lblMetodoSelecionado.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        txtValor.setDisable(false);
        btnAddPagamento.setDisable(false);

        double falta = valorTotalVenda - totalPago;

        // Limpa e foca
        txtValor.setText("");
        // Aqui setamos o texto vazio para obrigar o usuário a digitar ou ver o "0,00" sendo formado
        // Se quiser que já venha preenchido com o valor total, use:
        // txtValor.setText(String.format("%.2f", falta));

        if (falta > 0) txtValor.setPromptText(String.format("%.2f", falta));

        Platform.runLater(() -> txtValor.requestFocus());
    }

    @FXML public void selecionarDinheiro() { if (verificarSeFaltaPagar()) ativarModoDigitacao("DINHEIRO", btnDinheiro); }
    @FXML public void selecionarDebito()   { if (verificarSeFaltaPagar()) ativarModoDigitacao("DÉBITO", btnDebito); }
    @FXML public void selecionarCredito()  { if (verificarSeFaltaPagar()) ativarModoDigitacao("CRÉDITO", btnCredito); }
    @FXML public void selecionarPix()      { if (verificarSeFaltaPagar()) ativarModoDigitacao("PIX", btnPix); }

    private boolean verificarSeFaltaPagar() {
        if (totalPago >= (valorTotalVenda - 0.01)) {
            btnConcluir.requestFocus();
            return false;
        }
        return true;
    }

    @FXML
    public void confirmarPagamentoAtual() {
        if (metodoAtual == null) return;

        try {
            String texto = txtValor.getText().replace(",", ".");
            double valor = 0.0;

            // Se o campo estiver vazio ou zerado, assume o restante da dívida (atalho do Enter)
            if (texto.isEmpty() || texto.equals("0.00")) {
                valor = valorTotalVenda - totalPago;
            } else {
                valor = Double.parseDouble(texto);
            }

            if (valor <= 0) return;

            double falta = valorTotalVenda - totalPago;

            boolean isCartao = metodoAtual.equals("DÉBITO") || metodoAtual.equals("CRÉDITO");
            if (isCartao && valor > (falta + 0.01)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Cartão não aceita troco. Máximo: " + String.format("%.2f", falta));
                alert.showAndWait();
                // Ao voltar do erro, limpa para o usuário digitar de novo
                txtValor.setText("");
                Platform.runLater(() -> txtValor.requestFocus());
                return;
            }

            listaPagamentos.add(new Pagamento(metodoAtual, valor));
            totalPago += valor;

            atualizarTotalizadores();

            if (totalPago >= (valorTotalVenda - 0.01)) {
                txtValor.setText("");
                txtValor.setDisable(true);
                btnAddPagamento.setDisable(true);
                btnConcluir.setDisable(false);
                btnConcluir.requestFocus();
            } else {
                resetarParaEstadoDeEspera();
            }

        } catch (NumberFormatException e) {
            System.out.println("Valor inválido");
        }
    }

    @FXML
    public void confirmarEFechar() {
        this.confirmado = true;
        ((Stage) txtValor.getScene().getWindow()).close();
    }

    public void setValorTotal(double total) {
        this.valorTotalVenda = total;
        atualizarTotalizadores();
        resetarParaEstadoDeEspera();
    }

    private void atualizarTotalizadores() {
        lblTotal.setText(String.format("R$ %.2f", valorTotalVenda));
        double falta = valorTotalVenda - totalPago;

        if (falta > 0.01) {
            lblFalta.setText(String.format("R$ %.2f", falta));
            lblTroco.setText("R$ 0,00");
            btnConcluir.setDisable(true);
        } else {
            lblFalta.setText("R$ 0,00");
            lblTroco.setText(String.format("R$ %.2f", Math.abs(falta)));
            btnConcluir.setDisable(false);
        }
    }

    public boolean isConfirmado() { return confirmado; }
    public ObservableList<Pagamento> getPagamentosRealizados() { return listaPagamentos; }
}