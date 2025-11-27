package br.com.churrasco.controller;

import br.com.churrasco.dao.CaixaDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

import java.io.IOException;

public class MenuController {

    @FXML
    public void abrirPDV(ActionEvent event) {
        CaixaDAO caixaDAO = new CaixaDAO();

        // 1. Verifica se JÁ existe um caixa aberto no banco
        if (caixaDAO.buscarCaixaAberto() == null) {
            // 2. Se não tem, força a abertura da tela de Suprimento Inicial
            boolean caixaFoiAberto = abrirTelaAberturaCaixa();

            // Se o usuário fechar a janelinha sem abrir o caixa, cancela a entrada no PDV
            if (!caixaFoiAberto) {
                return;
            }
        }

        // 3. Se já estava aberto ou acabou de abrir com sucesso, entra no PDV
        navegar(event, "/br/com/churrasco/view/PDV.fxml", "Caixa - PDV");
    }

    @FXML
    public void abrirProdutos(ActionEvent event) {
        navegar(event, "/br/com/churrasco/view/Produtos.fxml", "Gerenciamento de Produtos");
    }

    @FXML
    public void abrirRelatorios(ActionEvent event) {
        navegar(event, "/br/com/churrasco/view/Relatorios.fxml", "Relatórios e Fechamento");
    }

    // --- LÓGICA DO MODAL DE ABERTURA ---
    private boolean abrirTelaAberturaCaixa() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/AberturaCaixa.fxml"));
            Parent root = loader.load();

            CaixaController controller = loader.getController();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Abertura de Caixa - Fundo de Troco");
            stage.initModality(Modality.APPLICATION_MODAL); // Bloqueia o menu atrás
            stage.showAndWait(); // Espera o usuário decidir

            return controller.isConfirmado(); // Retorna TRUE se ele clicou em "Abrir Caixa"

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Método genérico para trocar de tela
    private void navegar(ActionEvent event, String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Pega o palco (Stage) atual através do botão clicado
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle(titulo);
            stage.setMaximized(true); // Garante tela cheia
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erro ao abrir tela: " + fxmlPath);
        }
    }
    @FXML
    public void abrirHistoricoCaixas(ActionEvent event) {
        navegar(event, "/br/com/churrasco/view/HistoricoCaixas.fxml", "Auditoria de Caixas");
    }
}