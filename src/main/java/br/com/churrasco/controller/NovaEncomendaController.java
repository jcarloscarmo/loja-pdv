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
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;

public class NovaEncomendaController {

    @FXML private TextField txtCliente;
    @FXML private DatePicker dtRetirada;
    @FXML private TextField txtHora;

    private Encomenda encomendaCriada = null;
    private boolean confirmado = false;

    @FXML
    public void initialize() {
        dtRetirada.setValue(LocalDate.now());

        LocalTime agora = LocalTime.now().plusMinutes(30);
        txtHora.setText(agora.format(DateTimeFormatter.ofPattern("HH:mm")));

        txtCliente.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancelar();
                    }
                });
            }
        });

        txtCliente.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                txtHora.requestFocus();
                txtHora.selectAll();
            }
        });

        txtHora.textProperty().addListener((obs, oldValue, newValue) -> aplicarMascaraHora(newValue));

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

            String horaTexto = normalizarHora(txtHora.getText());
            if (horaTexto == null) {
                mostrarAlerta("Hora inválida. Use o formato HH:mm (Ex: 12:30)");
                return;
            }

            LocalTime hora;
            try {
                hora = LocalTime.parse(horaTexto, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e) {
                mostrarAlerta("Hora inválida. Use um horário entre 00:00 e 23:59.");
                return;
            }

            LocalDateTime dataHoraRetirada = LocalDateTime.of(data, hora);

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
        if (txtCliente.getScene() != null) {
            ((Stage) txtCliente.getScene().getWindow()).close();
        }
    }

    private void aplicarMascaraHora(String valorDigitado) {
        if (valorDigitado == null) {
            return;
        }

        String apenasDigitos = valorDigitado.replaceAll("[^0-9]", "");
        if (apenasDigitos.length() > 4) {
            apenasDigitos = apenasDigitos.substring(0, 4);
        }

        String formatado;
        if (apenasDigitos.length() <= 2) {
            formatado = apenasDigitos;
        } else {
            formatado = apenasDigitos.substring(0, 2) + ":" + apenasDigitos.substring(2);
        }

        if (!formatado.equals(valorDigitado)) {
            String textoFinal = formatado;
            Platform.runLater(() -> {
                txtHora.setText(textoFinal);
                txtHora.positionCaret(textoFinal.length());
            });
        }
    }

    private String normalizarHora(String textoHora) {
        if (textoHora == null) {
            return null;
        }

        String digitos = textoHora.replaceAll("[^0-9]", "");
        if (digitos.length() != 4) {
            return null;
        }

        return digitos.substring(0, 2) + ":" + digitos.substring(2);
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
