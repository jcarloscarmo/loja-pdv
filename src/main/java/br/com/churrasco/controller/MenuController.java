package br.com.churrasco.controller;

import br.com.churrasco.dao.CaixaDAO;
import br.com.churrasco.model.Caixa;
import br.com.churrasco.util.Navegacao;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MenuController {

    @FXML
    public void abrirPDV(ActionEvent event) {
        CaixaDAO caixaDAO = new CaixaDAO();
        Caixa caixaAberto = caixaDAO.buscarCaixaAberto();

        // 1. Verifica se tem caixa aberto
        if (caixaAberto != null) {
            java.time.LocalDate dataCaixa = caixaAberto.getDataAbertura().toLocalDate();
            java.time.LocalDate hoje = java.time.LocalDate.now();

            // SEGURANÇA: Se o caixa aberto for de ONTEM (ou antes), obriga fechar
            if (dataCaixa.isBefore(hoje)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Bloqueio de Segurança");
                alert.setHeaderText("Caixa Anterior em Aberto!");
                alert.setContentText("O caixa do dia " + dataCaixa + " não foi fechado.\nVocê precisa fechá-lo antes de iniciar as vendas de hoje.");
                alert.showAndWait();

                abrirTelaFechamentoForcado();
                return; // Não deixa entrar no PDV
            }
        }
        // 2. Se não tem caixa aberto, abre a tela de Abertura (Fundo de Troco)
        else {
            boolean abriu = abrirTelaAberturaCaixa();
            if (!abriu) return; // Se cancelou a abertura, não entra
        }

        // 3. Tudo certo: Entra no PDV mantendo a janela maximizada
        Navegacao.trocarTela(event, "/br/com/churrasco/view/PDV.fxml", "Caixa - PDV");
    }

    @FXML
    public void abrirProdutos(ActionEvent event) {
        Navegacao.trocarTela(event, "/br/com/churrasco/view/Produtos.fxml", "Gerenciamento de Produtos");
    }

    @FXML
    public void abrirRelatorios(ActionEvent event) {
        Navegacao.trocarTela(event, "/br/com/churrasco/view/Relatorios.fxml", "Relatórios e Fechamento");
    }

    @FXML
    public void abrirHistoricoCaixas(ActionEvent event) {
        Navegacao.trocarTela(event, "/br/com/churrasco/view/HistoricoCaixas.fxml", "Auditoria de Caixas");
    }

    @FXML
    public void abrirRankingProdutos(ActionEvent event) {
        Navegacao.trocarTela(event, "/br/com/churrasco/view/RelatorioProdutos.fxml", "Ranking de Vendas");
    }

    // --- NOVO MÉTODO DE CONFIGURAÇÕES ---
    @FXML
    public void abrirConfiguracoes(ActionEvent event) {
        Navegacao.trocarTela(event, "/br/com/churrasco/view/Configuracoes.fxml", "Configurações do Sistema");
    }

    // --- JANELAS MODAIS (POPUPS) ---

    private boolean abrirTelaAberturaCaixa() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/AberturaCaixa.fxml"));
            Parent root = loader.load();
            CaixaController controller = loader.getController();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Abertura de Caixa");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            return controller.isConfirmado();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void abrirTelaFechamentoForcado() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/FechamentoCaixa.fxml"));
            Parent root = loader.load();

            // O Controller vai buscar automaticamente o caixa que está aberto no banco
            CaixaController controller = loader.getController();
            controller.carregarDadosFechamento();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Fechamento Pendente");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}