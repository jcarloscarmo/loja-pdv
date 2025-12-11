package br.com.churrasco.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MenuController {

    @FXML
    private Label lblUsuarioLogado;

    @FXML
    public void initialize() {
        // Aqui você pode recuperar quem logou e mostrar no label
        // Ex: lblUsuarioLogado.setText("Olá, " + Sessao.usuario.getNome());
    }

    // --- NOVO MÉTODO: LOGOUT / TROCAR USUÁRIO ---
    @FXML
    public void fazerLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Trocar de Usuário");
        alert.setHeaderText(null);
        alert.setContentText("Deseja realmente sair e voltar para a tela de login?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                // Carrega a tela de Login (assumindo que o arquivo é Login.fxml)
                Parent root = FXMLLoader.load(getClass().getResource("/br/com/churrasco/view/Login.fxml"));

                // Pega a janela atual (Menu)
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                // Troca a cena para o Login
                stage.setScene(new Scene(root));
                stage.setTitle("Login - Tiãozinho's Grill");

                // Se o menu estava maximizado, o login geralmente é menor e centralizado
                stage.setMaximized(false);
                stage.centerOnScreen();

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Erro ao voltar para o Login. Verifique se Login.fxml existe.");
            }
        }
    }

    // --- MÉTODOS DE NAVEGAÇÃO EXISTENTES ---

    @FXML
    public void abrirFinanceiro(ActionEvent event) {
        navegar(event, "/br/com/churrasco/view/FluxoCaixa.fxml", "Fluxo de Caixa & DRE");
    }

    @FXML
    public void abrirPDV(ActionEvent event) {
        navegar(event, "/br/com/churrasco/view/PDV.fxml", "Frente de Caixa");
    }

    @FXML
    public void abrirProdutos(ActionEvent event) {
        navegar(event, "/br/com/churrasco/view/Produtos.fxml", "Gerenciamento de Produtos");
    }

    @FXML
    public void abrirRelatorios(ActionEvent event) {
        navegar(event, "/br/com/churrasco/view/Relatorios.fxml", "Relatórios de Fechamento");
    }

    @FXML
    public void abrirRankingProdutos(ActionEvent event) {
        navegar(event, "/br/com/churrasco/view/RelatorioProdutos.fxml", "Ranking de Vendas");
    }

    @FXML
    public void abrirHistoricoCaixas(ActionEvent event) {
        navegar(event, "/br/com/churrasco/view/HistoricoCaixas.fxml", "Auditoria de Caixas");
    }

    @FXML
    public void abrirConfiguracoes(ActionEvent event) {
        navegar(event, "/br/com/churrasco/view/Configuracoes.fxml", "Configurações");
    }

    private void navegar(ActionEvent event, String fxmlPath, String titulo) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Tiãozinho's Grill - " + titulo);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            System.err.println("Erro ao carregar a tela: " + fxmlPath);
            e.printStackTrace();
        }
    }
}