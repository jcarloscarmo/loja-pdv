package br.com.churrasco.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SplashController implements Initializable {

    @FXML private ProgressBar barraProgresso;
    @FXML private Label lblStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Roda o carregamento em uma Thread separada
        new Thread(() -> {
            try {
                // Simula carregamento (0% a 100%)
                for (int i = 0; i <= 100; i++) {
                    double progresso = i / 100.0;

                    // Atualiza a tela (sempre usar Platform.runLater para mexer na tela de outra Thread)
                    final String msg = gerarMensagem(i);
                    Platform.runLater(() -> {
                        barraProgresso.setProgress(progresso);
                        lblStatus.setText(msg);
                    });

                    Thread.sleep(30); // Velocidade do carregamento (30ms * 100 = 3 segundos)
                }

                // Quando terminar, abre a próxima tela
                Platform.runLater(this::abrirProximaTela);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String gerarMensagem(int i) {
        if (i < 20) return "Conectando ao banco de dados...";
        if (i < 50) return "Carregando configurações...";
        if (i < 80) return "Verificando permissões...";
        return "Iniciando sistema...";
    }

    private void abrirProximaTela() {
        try {
            // Fecha a Splash Screen
            Stage stageAtual = (Stage) barraProgresso.getScene().getWindow();
            stageAtual.close();

            // Abre o Menu (Na próxima etapa mudaremos para o Login)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Menu.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("PDV Churrascaria - Sistema de Gestão");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}