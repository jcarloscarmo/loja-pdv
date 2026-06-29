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
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MenuController {

    @FXML private Label lblUsuarioLogado;
    @FXML private Label lblVersaoSistema;
    @FXML private Button btnUpdate;
    
    private br.com.churrasco.util.UpdateService.UpdateInfo updateDisponivel;

    // --- BOTÕES DO MENU (PRECISAMOS DELES PARA OCULTAR) ---
    @FXML private Button btnFinanceiro;
    @FXML private Button btnProdutos;
    @FXML private Button btnRelatorios;
    @FXML private Button btnRanking;
    @FXML private Button btnAuditoria;
    @FXML private Button btnConfiguracoes;
    @FXML private Button btnPromocoes;
    // O btnPDV e btnSair não precisam sumir, então não é obrigatório declarar,
    // mas se quiser manipular no futuro, pode declarar também.

    @FXML
    public void initialize() {
        if (lblVersaoSistema != null) {
            lblVersaoSistema.setText("Versão: " + System.getProperty("pdvchurrasco.app.version", "Dev"));
        }

        new Thread(() -> {
            br.com.churrasco.util.UpdateService.checkUpdate().ifPresent(info -> {
                javafx.application.Platform.runLater(() -> {
                    updateDisponivel = info;
                    if (btnUpdate != null) {
                        btnUpdate.setText("🚀 Atualização " + info.version + " Disponível (Baixar)");
                        btnUpdate.setVisible(true);
                        btnUpdate.setManaged(true);
                    }
                });
            });
        }).start();

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
                esconderBotao(btnPromocoes);
            }
        }
        
        // Garante que o botão X (Fechar) do Windows no Menu acione o backup!
        javafx.application.Platform.runLater(() -> {
            if (lblUsuarioLogado != null && lblUsuarioLogado.getScene() != null) {
                Stage stage = (Stage) lblUsuarioLogado.getScene().getWindow();
                stage.setOnCloseRequest(ev -> {
                    ev.consume();
                    br.com.churrasco.controller.LoginController.executarBackupEFechar();
                });
            }
        });
    }

    // Método auxiliar para sumir com o botão (Visual e Espaço)
    private void esconderBotao(Button btn) {
        if (btn != null) {
            btn.setVisible(false); // Fica invisível
            btn.setManaged(false); // Não ocupa espaço no layout (os outros botões sobem)
        }
    }

    @FXML
    public void baixarAtualizacao() {
        if (updateDisponivel == null) return;
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Atualizando Sistema");
        alert.setHeaderText("Baixando nova versão: " + updateDisponivel.version);
        
        javafx.scene.control.ProgressBar pBar = new javafx.scene.control.ProgressBar(0);
        pBar.setPrefWidth(300);
        Label lblStatus = new Label("Conectando ao servidor...");
        
        VBox vbox = new VBox(10, lblStatus, pBar);
        alert.getDialogPane().setContent(vbox);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        
        javafx.concurrent.Task<java.nio.file.Path> downloadTask = br.com.churrasco.util.UpdateService.createDownloadTask(updateDisponivel);
        
        pBar.progressProperty().bind(downloadTask.progressProperty());
        lblStatus.textProperty().bind(downloadTask.messageProperty());
        
        downloadTask.setOnSucceeded(e -> {
            alert.setResult(ButtonType.OK);
            alert.close();
            br.com.churrasco.util.UpdateService.installAndExit(downloadTask.getValue());
        });
        
        downloadTask.setOnFailed(e -> {
            alert.setResult(ButtonType.CANCEL);
            alert.close();
            mostrarErroDetalhado("Falha ao baixar atualização", (Exception) downloadTask.getException());
        });
        
        new Thread(downloadTask).start();
        alert.showAndWait();
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
    @FXML public void abrirPromocoes(ActionEvent event) { navegar(event, "/br/com/churrasco/view/Promocoes.fxml", "Promoções"); }

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
