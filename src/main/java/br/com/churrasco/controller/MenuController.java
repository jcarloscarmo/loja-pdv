package br.com.churrasco.controller;

import br.com.churrasco.util.LogUtil;
import br.com.churrasco.util.Sessao;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MenuController {

    @FXML private Label lblUsuarioLogado;

    @FXML
    public void initialize() {
        if (Sessao.getUsuario() != null) {
            lblUsuarioLogado.setText("Usuário: " + Sessao.getUsuario().getNome());
        }
    }

    @FXML
    public void fazerLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Trocar de Usuário");
        alert.setHeaderText(null);
        alert.setContentText("Deseja realmente sair?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                Sessao.logout(); // Limpa sessão
                Parent root = FXMLLoader.load(getClass().getResource("/br/com/churrasco/view/Login.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Login");
                stage.centerOnScreen();
            } catch (Exception e) {
                mostrarErroDetalhado("Erro ao fazer logout", e);
            }
        }
    }

    // --- NAVEGAÇÃO ---
    @FXML public void abrirFinanceiro(ActionEvent event) { navegar(event, "/br/com/churrasco/view/FluxoCaixa.fxml", "Fluxo de Caixa"); }
    @FXML public void abrirPDV(ActionEvent event) { navegar(event, "/br/com/churrasco/view/PDV.fxml", "Frente de Caixa"); }
    @FXML public void abrirProdutos(ActionEvent event) { navegar(event, "/br/com/churrasco/view/Produtos.fxml", "Produtos"); }
    @FXML public void abrirRelatorios(ActionEvent event) { navegar(event, "/br/com/churrasco/view/Relatorios.fxml", "Relatórios"); }
    @FXML public void abrirRankingProdutos(ActionEvent event) { navegar(event, "/br/com/churrasco/view/RelatorioProdutos.fxml", "Ranking"); }
    @FXML public void abrirHistoricoCaixas(ActionEvent event) { navegar(event, "/br/com/churrasco/view/HistoricoCaixas.fxml", "Auditoria"); }
    @FXML public void abrirConfiguracoes(ActionEvent event) { navegar(event, "/br/com/churrasco/view/Configuracoes.fxml", "Configurações"); }

    private void navegar(ActionEvent event, String fxmlPath, String titulo) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Tiãozinho's Grill - " + titulo);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();

            // GRAVA O ERRO NO ARQUIVO
            LogUtil.registrarErro("Falha ao abrir tela: " + titulo, e);

            // Mostra na tela também
            mostrarErroDetalhado("Não foi possível abrir: " + titulo, e);
        }
    }

    private void mostrarErroDetalhado(String cabecalho, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro de Sistema");
        alert.setHeaderText(cabecalho);

        // Extrai o texto técnico do erro
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("O erro técnico foi:");
        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }
}