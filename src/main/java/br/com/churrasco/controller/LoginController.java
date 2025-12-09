package br.com.churrasco.controller;

import br.com.churrasco.dao.UsuarioDAO;
import br.com.churrasco.model.Usuario;
import br.com.churrasco.util.LogUtil;
import br.com.churrasco.util.Sessao;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox; // Importante
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.util.List;

public class LoginController {

    // Mudou de TextField para ComboBox
    @FXML private ComboBox<String> comboUsuario;
    @FXML private PasswordField txtSenha;

    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        // Carrega os nomes do banco e joga na lista
        List<String> nomes = usuarioDAO.listarNomes();
        comboUsuario.setItems(FXCollections.observableArrayList(nomes));

        // Seleciona o primeiro da lista pra facilitar (opcional)
        if (!nomes.isEmpty()) {
            comboUsuario.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void fazerLogin(ActionEvent event) {
        String nome = comboUsuario.getValue(); // Pega do Dropdown
        String senha = txtSenha.getText();

        if (nome == null || nome.isEmpty() || senha.isEmpty()) {
            mostrarAlerta("Selecione um usuário e digite a senha.");
            return;
        }

        Usuario usuario = usuarioDAO.autenticar(nome, senha);

        if (usuario != null) {
            Sessao.setUsuario(usuario);
            LogUtil.registrar(usuario.getNome(), "Realizou login no sistema.");
            abrirMenuPrincipal(event);
        } else {
            LogUtil.registrar(nome, "Tentativa de login falhou.");
            mostrarAlerta("Senha incorreta!");
        }
    }

    private void abrirMenuPrincipal(ActionEvent event) {
        try {
            Stage stageLogin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stageLogin.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Menu.fxml"));
            Parent root = loader.load();

            Stage stageMenu = new Stage();
            stageMenu.setTitle("PDV Churrascaria - " + Sessao.getUsuario().getNome());
            stageMenu.setScene(new Scene(root));
            stageMenu.setMaximized(true);
            stageMenu.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void sair() {
        System.exit(0);
    }

    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atenção");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}