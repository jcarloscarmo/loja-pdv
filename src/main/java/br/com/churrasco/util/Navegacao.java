package br.com.churrasco.util;

import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Navegacao {

    public static void trocarTela(Event event, String fxmlPath, String titulo) {
        try {
            // Carrega o arquivo visual
            Parent root = FXMLLoader.load(Navegacao.class.getResource(fxmlPath));

            // Pega a Janela (Stage) e a Cena (Scene) atuais através do botão clicado
            Node node = (Node) event.getSource();
            Stage stage = (Stage) node.getScene().getWindow();
            Scene scene = node.getScene();

            // O TRUQUE: Troca apenas o conteúdo de dentro, mantém a janela igual
            scene.setRoot(root);

            // Garante o título
            stage.setTitle(titulo);

            // Reforça que deve estar maximizada (caso algo tenha mudado)
            if (!stage.isMaximized()) {
                stage.setMaximized(true);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erro ao navegar para: " + fxmlPath);
        }
    }
}