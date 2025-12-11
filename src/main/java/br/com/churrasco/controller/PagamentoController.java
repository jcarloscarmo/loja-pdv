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

    // Labels de Totais
    @FXML private Label lblSubTotal;      // Valor dos produtos
    @FXML private Label lblValorDesconto; // Quanto de desconto
    @FXML private Label lblTotalFinal;    // Subtotal - Desconto
    @FXML private Label lblFalta;
    @FXML private Label lblTroco;

    @FXML private Label lblMetodoSelecionado;
    @FXML private TextField txtValor;
    @FXML private Button btnConcluir, btnAddPagamento;

    // Botões de Ação
    @FXML private Button btnDinheiro, btnDebito, btnCredito, btnPix, btnDesconto;

    @FXML private TableView<Pagamento> tabelaPagamentos;
    @FXML private TableColumn<Pagamento, String> colTipoPagamento;
    @FXML private TableColumn<Pagamento, Double> colValorPagamento;

    // Estado da Tela
    private double valorVendaOriginal; // Valor bruto (soma dos itens)
    private double valorDesconto = 0.0;
    private double totalPago = 0.0;

    private boolean confirmado = false;
    private String modoAtual = null; // "PAGAMENTO" ou "DESCONTO"
    private String tipoPagamentoSelecionado = null;

    private ObservableList<Pagamento> listaPagamentos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colTipoPagamento.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colValorPagamento.setCellValueFactory(new PropertyValueFactory<>("valor"));
        tabelaPagamentos.setItems(listaPagamentos);

        // Ouve teclas globais (F1, NumPad, -, etc)
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::processarTeclaGlobal);

        configurarMascaraMonetaria();

        // Atalhos do Campo de Valor
        txtValor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) confirmarInputAtual();
            else if (event.getCode() == KeyCode.ESCAPE) resetarParaEstadoDeEspera();
        });

        // Atalhos do Botão Concluir
        btnConcluir.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) confirmarEFechar();
        });

        rootPane.setFocusTraversable(true);
        resetarParaEstadoDeEspera();
    }

    private void configurarMascaraMonetaria() {
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

    private void processarTeclaGlobal(KeyEvent event) {
        if (txtValor.isFocused() && !txtValor.isDisabled()) return;

        KeyCode code = event.getCode();

        if (code == KeyCode.DIGIT1 || code == KeyCode.NUMPAD1) { event.consume(); selecionarDinheiro(); }
        else if (code == KeyCode.DIGIT2 || code == KeyCode.NUMPAD2) { event.consume(); selecionarDebito(); }
        else if (code == KeyCode.DIGIT3 || code == KeyCode.NUMPAD3) { event.consume(); selecionarCredito(); }
        else if (code == KeyCode.DIGIT4 || code == KeyCode.NUMPAD4) { event.consume(); selecionarPix(); }
        else if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) { event.consume(); selecionarDesconto(); } // Atalho '-'
        else if (code == KeyCode.ESCAPE) { ((Stage) rootPane.getScene().getWindow()).close(); }
    }

    private void resetarParaEstadoDeEspera() {
        this.modoAtual = null;
        this.tipoPagamentoSelecionado = null;

        String styleCinza = "-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-font-weight: bold;";
        btnDinheiro.setStyle(styleCinza);
        btnDebito.setStyle(styleCinza);
        btnCredito.setStyle(styleCinza);
        btnPix.setStyle(styleCinza);

        // Botão de desconto vermelho para destacar
        btnDesconto.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        lblMetodoSelecionado.setText("SELECIONE (1-4) ou DESCONTO (-)");
        lblMetodoSelecionado.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");

        txtValor.setText("");
        txtValor.setPromptText("");
        txtValor.setDisable(true);
        btnAddPagamento.setDisable(true);

        rootPane.requestFocus();
    }

    // --- AÇÕES DOS BOTÕES ---
    @FXML public void selecionarDinheiro() { ativarDigitacao("PAGAMENTO", "DINHEIRO", btnDinheiro, "#27ae60"); }
    @FXML public void selecionarDebito()   { ativarDigitacao("PAGAMENTO", "DÉBITO", btnDebito, "#27ae60"); }
    @FXML public void selecionarCredito()  { ativarDigitacao("PAGAMENTO", "CRÉDITO", btnCredito, "#27ae60"); }
    @FXML public void selecionarPix()      { ativarDigitacao("PAGAMENTO", "PIX", btnPix, "#27ae60"); }
    @FXML public void selecionarDesconto() { ativarDigitacao("DESCONTO", "DESCONTO", btnDesconto, "#c0392b"); }

    private void ativarDigitacao(String modo, String tipo, Button btn, String corHex) {
        // Se já pagou tudo, não deixa selecionar pagamento (só deixa desconto pra corrigir)
        double totalComDesconto = valorVendaOriginal - valorDesconto;
        if (modo.equals("PAGAMENTO") && totalPago >= (totalComDesconto - 0.01)) {
            btnConcluir.requestFocus();
            return;
        }

        this.modoAtual = modo;
        this.tipoPagamentoSelecionado = tipo;

        // Visual
        btn.setStyle("-fx-background-color: " + corHex + "; -fx-text-fill: white; -fx-font-weight: bold;");
        lblMetodoSelecionado.setText(modo.equals("DESCONTO") ? "APLICAR DESCONTO:" : "PAGANDO COM: " + tipo);
        lblMetodoSelecionado.setStyle("-fx-text-fill: " + corHex + "; -fx-font-weight: bold;");

        txtValor.setDisable(false);
        btnAddPagamento.setDisable(false);
        txtValor.setText("");

        // Sugestão de valor (Prompt)
        if (modo.equals("PAGAMENTO")) {
            double falta = (valorVendaOriginal - valorDesconto) - totalPago;
            if (falta > 0) txtValor.setPromptText(String.format("%.2f", falta));
        } else {
            txtValor.setPromptText("0,00");
        }

        Platform.runLater(() -> txtValor.requestFocus());
    }

    // --- CONFIRMAÇÃO DO INPUT ---
    @FXML
    public void confirmarInputAtual() {
        if (modoAtual == null) return;

        try {
            String texto = txtValor.getText().replace(",", ".");
            double valorInput = 0.0;

            // Atalho do Enter vazio: Pega o restante automático
            if (texto.isEmpty() || texto.equals("0.00")) {
                if (modoAtual.equals("PAGAMENTO")) {
                    valorInput = (valorVendaOriginal - valorDesconto) - totalPago;
                }
            } else {
                valorInput = Double.parseDouble(texto);
            }

            if (valorInput <= 0) return;

            if (modoAtual.equals("DESCONTO")) {
                aplicarDesconto(valorInput);
            } else {
                processarPagamento(valorInput);
            }

        } catch (NumberFormatException e) { }
    }

    private void aplicarDesconto(double valor) {
        // Validação 1: Desconto maior que a venda
        if (valor > valorVendaOriginal) {
            mostrarAlerta("Desconto inválido", "O desconto não pode ser maior que o valor da venda.");
            return;
        }
        // Validação 2: Desconto impede pagamento já feito
        if ((valorVendaOriginal - valor) < totalPago) {
            mostrarAlerta("Atenção", "Já existem pagamentos lançados. O novo total não pode ser menor que o valor já pago.");
            return;
        }

        this.valorDesconto = valor; // Define o desconto
        atualizarTotalizadores();

        // CORREÇÃO DO FOCO: Se pagou tudo com o desconto, foca no Concluir
        double totalComDesconto = valorVendaOriginal - valorDesconto;
        if (totalPago >= (totalComDesconto - 0.01)) {
            txtValor.setText("");
            txtValor.setDisable(true);
            btnAddPagamento.setDisable(true);
            btnConcluir.setDisable(false);
            Platform.runLater(() -> btnConcluir.requestFocus());
        } else {
            resetarParaEstadoDeEspera();
        }
    }

    private void processarPagamento(double valor) {
        double totalComDesconto = valorVendaOriginal - valorDesconto;
        double falta = totalComDesconto - totalPago;

        // Validação de Cartão (não gera troco)
        boolean isCartao = tipoPagamentoSelecionado.equals("DÉBITO") || tipoPagamentoSelecionado.equals("CRÉDITO");
        if (isCartao && valor > (falta + 0.01)) {
            mostrarAlerta("Atenção", "Cartão não aceita troco. Máximo: R$ " + String.format("%.2f", falta));
            txtValor.setText("");
            return;
        }

        listaPagamentos.add(new Pagamento(tipoPagamentoSelecionado, valor));
        totalPago += valor;

        atualizarTotalizadores();

        // Se finalizou, foca no concluir
        if (totalPago >= (totalComDesconto - 0.01)) {
            txtValor.setText("");
            txtValor.setDisable(true);
            btnAddPagamento.setDisable(true);
            btnConcluir.setDisable(false);
            Platform.runLater(() -> btnConcluir.requestFocus());
        } else {
            resetarParaEstadoDeEspera();
        }
    }

    // --- INICIALIZAÇÃO EXTERNA ---
    public void setValorTotal(double total) {
        this.valorVendaOriginal = total;
        this.valorDesconto = 0.0;
        this.totalPago = 0.0;
        listaPagamentos.clear();
        atualizarTotalizadores();
        resetarParaEstadoDeEspera();
    }

    private void atualizarTotalizadores() {
        double totalFinal = valorVendaOriginal - valorDesconto;
        double falta = totalFinal - totalPago;

        lblSubTotal.setText(String.format("R$ %.2f", valorVendaOriginal));
        lblValorDesconto.setText(String.format("R$ %.2f", valorDesconto));
        lblTotalFinal.setText(String.format("R$ %.2f", totalFinal));

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

    @FXML public void confirmarEFechar() {
        this.confirmado = true;
        ((Stage) txtValor.getScene().getWindow()).close();
    }

    // Getters para o PDVController pegar os dados
    public boolean isConfirmado() { return confirmado; }
    public ObservableList<Pagamento> getPagamentosRealizados() { return listaPagamentos; }
    public double getValorDesconto() { return valorDesconto; }

    private void mostrarAlerta(String t, String c) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(t);
        alert.setHeaderText(null);
        alert.setContentText(c);
        alert.showAndWait();
    }
}