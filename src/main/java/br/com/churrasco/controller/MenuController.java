package br.com.churrasco.controller;

import br.com.churrasco.dao.CaixaDAO;
import br.com.churrasco.dao.UsuarioDAO;
import br.com.churrasco.model.Caixa;
import br.com.churrasco.model.Usuario;
import br.com.churrasco.util.Navegacao;
import br.com.churrasco.util.Sessao;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class MenuController {

    @FXML private Button btnProdutos;
    @FXML private Button btnRelatorios;
    @FXML private Button btnAuditoria;
    @FXML private Button btnConfig;
    @FXML private Button btnRanking;
    @FXML private Label lblUsuarioLogado;

    @FXML
    public void initialize() {
        Usuario usuario = Sessao.getUsuario();
        if (usuario != null) {
            lblUsuarioLogado.setText("Usuário: " + usuario.getNome() + " (" + usuario.getPerfil() + ")");
            if ("ATENDENTE".equalsIgnoreCase(usuario.getPerfil())) {
                bloquearAcessoGerencial();
            }
        }
    }

    private void bloquearAcessoGerencial() {
        btnProdutos.setDisable(true);
        btnRelatorios.setDisable(true);
        btnAuditoria.setDisable(true);
        btnConfig.setDisable(true);
        btnRanking.setDisable(true);
    }

    @FXML
    public void abrirPDV(ActionEvent event) {
        CaixaDAO caixaDAO = new CaixaDAO();
        Caixa caixaAberto = caixaDAO.buscarCaixaAberto();

        // 1. Se TEM caixa aberto, verifica se é antigo (Trava de Segurança)
        if (caixaAberto != null) {
            java.time.LocalDate dataCaixa = caixaAberto.getDataAbertura().toLocalDate();
            java.time.LocalDate hoje = java.time.LocalDate.now();

            if (dataCaixa.isBefore(hoje)) {
                mostrarAlerta("Caixa de ontem (" + dataCaixa + ") ainda está aberto!\nChame o gerente para fechar.");
                // Aqui poderíamos pedir senha também, mas vamos forçar o fechamento seguro
                abrirTelaFechamentoForcado();
                return;
            }
        }
        // 2. Se NÃO tem caixa aberto, vamos abrir um novo
        else {
            // SEGURANÇA: Se não for DONO, pede senha do Tião
            if (!Sessao.getUsuario().isAdmin()) {
                boolean autorizado = solicitarSenhaAdmin("ABERTURA DE CAIXA");
                if (!autorizado) return; // Senha errada ou cancelou
            }

            boolean abriu = abrirTelaAberturaCaixa();
            if (!abriu) return; // Cancelou a abertura
        }

        // 3. Entra no PDV
        Navegacao.trocarTela(event, "/br/com/churrasco/view/PDV.fxml", "Caixa - PDV");
    }

    // --- MÉTODOS AUXILIARES ---

    private boolean solicitarSenhaAdmin(String acao) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Autorização de Gerente");
        dialog.setHeaderText("Ação Bloqueada: " + acao);
        dialog.setContentText("Digite a senha do Tião para liberar:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String senhaDigitada = result.get();
            // Verifica no banco se a senha bate com o usuário Tião
            Usuario admin = new UsuarioDAO().autenticar("Tião", senhaDigitada);
            if (admin != null && admin.isAdmin()) {
                return true;
            }
            mostrarAlerta("Senha incorreta! Ação negada.");
        }
        return false;
    }

    @FXML public void abrirProdutos(ActionEvent event) { Navegacao.trocarTela(event, "/br/com/churrasco/view/Produtos.fxml", "Gerenciamento de Produtos"); }
    @FXML public void abrirRelatorios(ActionEvent event) { Navegacao.trocarTela(event, "/br/com/churrasco/view/Relatorios.fxml", "Relatórios e Fechamento"); }
    @FXML public void abrirHistoricoCaixas(ActionEvent event) { Navegacao.trocarTela(event, "/br/com/churrasco/view/HistoricoCaixas.fxml", "Auditoria de Caixas"); }
    @FXML public void abrirRankingProdutos(ActionEvent event) { Navegacao.trocarTela(event, "/br/com/churrasco/view/RelatorioProdutos.fxml", "Ranking de Vendas"); }
    @FXML public void abrirConfiguracoes(ActionEvent event) { Navegacao.trocarTela(event, "/br/com/churrasco/view/Configuracoes.fxml", "Configurações do Sistema"); }

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
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private void abrirTelaFechamentoForcado() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/FechamentoCaixa.fxml"));
            Parent root = loader.load();
            CaixaController controller = loader.getController();
            controller.carregarDadosFechamento();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Fechamento Pendente");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}