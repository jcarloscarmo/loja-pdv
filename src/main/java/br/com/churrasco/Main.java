package br.com.churrasco;

import br.com.churrasco.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle; // Importante para tirar a borda da janela

public class Main extends Application {

    public static void main(String[] args) {
        // Inicializa Banco antes de tudo
        try {
            DatabaseConnection.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Agora carrega o SPLASH primeiro
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Login.fxml"));
        Parent root = loader.load();

        // Remove a barra de título da janela (fica só o quadrado colorido)
        primaryStage.initStyle(StageStyle.UNDECORATED);

        primaryStage.setScene(new Scene(root));
        primaryStage.centerOnScreen(); // Centraliza no monitor
        primaryStage.show();
    }
}