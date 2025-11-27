package br.com.churrasco.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ConfirmacaoController {

    @FXML private VBox root;
    @FXML private TextArea txtResumo;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar; // Novo ID injetado
    @FXML private Label lblPergunta;  // Novo ID injetado

    private boolean confirmado = false;

    @FXML
    public void initialize() {
        root.setOnKeyPressed(event -> {
            KeyCode tecla = event.getCode();
            if (tecla == KeyCode.DIGIT0 || tecla == KeyCode.NUMPAD0 || tecla == KeyCode.ESCAPE) {
                event.consume();
                cancelar();
            }
        });
        txtResumo.setFocusTraversable(false);
    }

    public void setTextoCupom(String texto) {
        txtResumo.setText(texto);
        txtResumo.setScrollTop(Double.MAX_VALUE);
        Platform.runLater(() -> btnConfirmar.requestFocus());
    }

    // --- NOVO MÉTODO PARA MODO VISUALIZAÇÃO ---
    public void ativarModoLeitura() {
        // Esconde o botão de cancelar (remove do layout também)
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);

        // Muda o texto da pergunta
        lblPergunta.setText("Histórico de Venda Finalizada");

        // Muda o botão de confirmar para apenas "FECHAR"
        btnConfirmar.setText("FECHAR (ENTER)");
        btnConfirmar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
    }

    @FXML
    public void confirmar() {
        // No modo leitura, confirmar é apenas fechar, mas mantém true (sem efeito colateral)
        this.confirmado = true;
        fechar();
    }

    @FXML
    public void cancelar() {
        this.confirmado = false;
        fechar();
    }

    private void fechar() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
    }

    public boolean isConfirmado() {
        return confirmado;
    }
}