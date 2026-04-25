package br.com.churrasco.controller;

import br.com.churrasco.util.LogUtil;
import br.com.churrasco.util.Navegacao;
import br.com.churrasco.util.Sessao;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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

    // --- BOTÕES DO MENU (PRECISAMOS DELES PARA OCULTAR) ---
    @FXML private Button btnFinanceiro;
    @FXML private Button btnProdutos;
    @FXML private Button btnRelatorios;
    @FXML private Button btnRanking;
    @FXML private Button btnAuditoria;
    @FXML private Button btnConfiguracoes;
    // O btnPDV e btnSair não precisam sumir, então não é obrigatório declarar,
    // mas se quiser manipular no futuro, pode declarar também.

    @FXML
    public void initialize() {
        if (Sessao.getUsuario() != null) {
            lblUsuarioLogado.setText("Usuário: " + Sessao.getUsuario().getNome());

            // --- LÓGICA DE PERMISSÃO ---
            // Se NÃO for DONO (ou seja, se for ATENDENTE), esconde tudo que não é PDV
            if (!Sessao.getUsuario().isAdmin()) { // isAdmin verifica se é "DONO"
                esconderBotao(btnFinanceiro);
                esconderBotao(btnProdutos);
                esconderBotao(btnRelatorios);
                esconderBotao(btnRanking);
                esconderBotao(btnAuditoria);
                esconderBotao(btnConfiguracoes);
            }
        }
    }

    // Método auxiliar para sumir com o botão (Visual e Espaço)
    private void esconderBotao(Button btn) {
        if (btn != null) {
            btn.setVisible(false); // Fica invisível
            btn.setManaged(false); // Não ocupa espaço no layout (os outros botões sobem)
        }
    }

    @FXML
    public void fazerLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sair");
        alert.setHeaderText(null);
        alert.setContentText("Deseja realmente sair?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                Sessao.logout();
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
            Navegacao.trocarTela(event, fxmlPath, "Tiãozinho's Grill - " + titulo);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.registrarErro("Falha ao abrir tela: " + titulo, e);
            mostrarErroDetalhado("Não foi possível abrir a tela: " + titulo, e);
        }
    }

    private void mostrarErroDetalhado(String cabecalho, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro de Sistema");
        alert.setHeaderText(cabecalho);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(new Label("Detalhes:"), 0, 0);
        expContent.add(textArea, 0, 1);
        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }
}
