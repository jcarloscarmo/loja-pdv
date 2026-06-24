package br.com.churrasco.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ImpressaoComprovanteController {

    @FXML private VBox root;
    @FXML private Label lblMensagem;
    @FXML private Button btnImprimir;
    @FXML private Button btnNaoImprimir;

    private boolean imprimir;

    @FXML
    public void initialize() {
        root.setOnKeyPressed(event -> {
            KeyCode tecla = event.getCode();
            if (tecla == KeyCode.DIGIT1 || tecla == KeyCode.NUMPAD1) {
                event.consume();
                imprimir();
            } else if (tecla == KeyCode.DIGIT0 || tecla == KeyCode.NUMPAD0 || tecla == KeyCode.ESCAPE) {
                event.consume();
                naoImprimir();
            }
        });
    }

    public void setNumeroVenda(int numeroVenda) {
        lblMensagem.setText("Venda Nº " + numeroVenda + " realizada com sucesso!");
        Platform.runLater(() -> btnImprimir.requestFocus());
    }

    @FXML
    public void imprimir() {
        this.imprimir = true;
        fechar();
    }

    @FXML
    public void naoImprimir() {
        this.imprimir = false;
        fechar();
    }

    public boolean isImprimir() {
        return imprimir;
    }

    private void fechar() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
    }
}
