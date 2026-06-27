package br.com.churrasco.controller;

import br.com.churrasco.dao.UsuarioDAO;
import br.com.churrasco.model.Usuario;
import br.com.churrasco.util.LogUtil;
import br.com.churrasco.util.Navegacao;
import br.com.churrasco.util.Sessao;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.util.List;

public class LoginController {

    @FXML private ComboBox<String> comboUsuario;
    @FXML private PasswordField txtSenha;

    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        List<String> nomes = usuarioDAO.listarNomes();
        comboUsuario.setItems(FXCollections.observableArrayList(nomes));
        if (!nomes.isEmpty()) {
            comboUsuario.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void fazerLogin(ActionEvent event) {
        String nome = comboUsuario.getValue();
        String senha = txtSenha.getText();

        if (nome == null || nome.isEmpty()) {
            mostrarAlerta("Selecione um usuário.");
            return;
        }

        Usuario usuario = usuarioDAO.autenticar(nome, senha);

        if (usuario != null) {
            Sessao.setUsuario(usuario);
            try { LogUtil.registrar(usuario.getNome(), "Realizou login no sistema."); } catch (Exception e) {}

            abrirMenuPrincipal(event);
        } else {
            mostrarAlerta("Usuario ou senha incorretos.");
        }
    }

    private void abrirMenuPrincipal(ActionEvent event) {
        try {
            Stage stageLogin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stageLogin.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Menu.fxml"));
            Parent root = loader.load();

            Stage stageMenu = new Stage();
            Navegacao.aplicarCena(stageMenu, root, "PDV Churrascaria - " + Sessao.getUsuario().getNome());
            
            // Intercepta o fechamento do Menu principal
            stageMenu.setOnCloseRequest(ev -> {
                ev.consume();
                executarBackupEFechar();
            });
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Erro ao abrir o Menu: " + e.getMessage());
        }
    }

    @FXML
    public void sair() {
        executarBackupEFechar();
    }

    public static void executarBackupEFechar() {
        javafx.stage.Stage loadingStage = new javafx.stage.Stage(javafx.stage.StageStyle.UNDECORATED);
        
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(20);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);
        vbox.setStyle("-fx-background-color: #2c3e50; -fx-padding: 30; -fx-border-color: #e67e22; -fx-border-width: 2;");
        
        javafx.scene.control.Label lblTitulo = new javafx.scene.control.Label("ENCERRANDO O SISTEMA");
        lblTitulo.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        javafx.scene.control.Label lblMsg = new javafx.scene.control.Label("Realizando backup de segurança...\nAguarde, por favor.");
        lblMsg.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        lblMsg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        javafx.scene.control.ProgressIndicator progress = new javafx.scene.control.ProgressIndicator();
        progress.setStyle("-fx-progress-color: #e67e22;");
        
        vbox.getChildren().addAll(lblTitulo, progress, lblMsg);
        
        javafx.scene.Scene scene = new javafx.scene.Scene(vbox);
        loadingStage.setScene(scene);
        loadingStage.sizeToScene();
        loadingStage.centerOnScreen();
        loadingStage.setAlwaysOnTop(true);
        loadingStage.show();

        javafx.concurrent.Task<Void> backupTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                br.com.churrasco.util.DatabaseBackupService.createBackupViaUI(br.com.churrasco.util.DatabaseConnection.getDatabasePath());
                Thread.sleep(1500); // Para dar tempo de visualizar a tela de carregamento caso o backup seja muito rápido
                return null;
            }
        };

        backupTask.setOnSucceeded(e -> {
            progress.setVisible(false);
            progress.setManaged(false);
            
            lblTitulo.setText("SUCESSO!");
            lblTitulo.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 18px; -fx-font-weight: bold;");
            vbox.setStyle("-fx-background-color: #2c3e50; -fx-padding: 30; -fx-border-color: #2ecc71; -fx-border-width: 2;");
            
            lblMsg.setText("Backup validado e salvo com sucesso.\nO sistema será encerrado.");
            
            javafx.scene.control.Button btnOk = new javafx.scene.control.Button("OK");
            btnOk.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 40;");
            btnOk.setOnAction(ev -> {
                loadingStage.close();
                Platform.exit();
            });
            
            vbox.getChildren().add(btnOk);
        });

        backupTask.setOnFailed(e -> {
            loadingStage.close();
            Throwable ex = backupTask.getException();
            
            Alert erro = new Alert(Alert.AlertType.ERROR, "Erro ao gerar ou validar backup:\n" + ex.getMessage() + "\n\nO sistema NÃO será fechado para que você possa verificar espaço em disco ou tentar novamente.", javafx.scene.control.ButtonType.OK);
            erro.setHeaderText("Falha no Backup");
            erro.showAndWait();
            
            br.com.churrasco.util.LogUtil.registrarErro("Erro no backup de fechamento", (Exception) ex);
        });

        new Thread(backupTask).start();
    }

    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
