package br.com.churrasco.controller;

import br.com.churrasco.model.Encomenda;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class NovaEncomendaController {

    @FXML private TextField txtCliente;
    @FXML private DatePicker dtRetirada;
    @FXML private TextField txtHora;

    private Encomenda encomendaCriada = null;
    private boolean confirmado = false;

    @FXML
    public void initialize() {
        // Valores Padrão: Hoje
        dtRetirada.setValue(LocalDate.now());

        // Hora Padrão: Agora + 30 min (Arredondado)
        LocalTime agora = LocalTime.now().plusMinutes(30);
        txtHora.setText(agora.format(DateTimeFormatter.ofPattern("HH:mm")));

        // --- CORREÇÃO DO ERRO DO ESC (NullPointerException) ---
        // O método initialize roda antes da janela (Scene) existir.
        // Adicionamos um ouvinte para esperar a cena ser criada.
        txtCliente.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancelar();
                    }
                });
            }
        });

        // Foco no nome do cliente (com leve atraso para garantir que a janela abriu)
        Platform.runLater(() -> txtCliente.requestFocus());
    }

    @FXML
    public void salvar() {
        try {
            if (txtCliente.getText().isEmpty()) {
                mostrarAlerta("Digite o nome do cliente.");
                return;
            }

            LocalDate data = dtRetirada.getValue();
            if (data == null) {
                mostrarAlerta("Selecione a data.");
                return;
            }

            // Tenta ler a hora (HH:mm)
            String horaTexto = txtHora.getText().trim();
            if (!horaTexto.matches("\\d{2}:\\d{2}")) {
                mostrarAlerta("Hora inválida. Use o formato HH:mm (Ex: 12:30)");
                return;
            }

            LocalTime hora = LocalTime.parse(horaTexto);
            LocalDateTime dataHoraRetirada = LocalDateTime.of(data, hora);

            // Cria o objeto para devolver
            this.encomendaCriada = new Encomenda(txtCliente.getText(), dataHoraRetirada);
            this.confirmado = true;

            fecharJanela();

        } catch (Exception e) {
            mostrarAlerta("Erro nos dados: " + e.getMessage());
        }
    }

    @FXML
    public void cancelar() {
        this.confirmado = false;
        this.encomendaCriada = null;
        fecharJanela();
    }

    private void fecharJanela() {
        // Verifica se a cena existe antes de tentar fechar (segurança extra)
        if (txtCliente.getScene() != null) {
            ((Stage) txtCliente.getScene().getWindow()).close();
        }
    }

    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public Encomenda getEncomendaCriada() {
        return encomendaCriada;
    }
}