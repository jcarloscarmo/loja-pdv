package br.com.churrasco;

import br.com.churrasco.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        // Inicializa Banco
        try {
            DatabaseConnection.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Menu.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("PDV Churrascaria - Sistema de Gestão");
        primaryStage.setScene(new Scene(root));

        // COMEÇA MAXIMIZADO
        primaryStage.setMaximized(true);

        primaryStage.show();
    }
}