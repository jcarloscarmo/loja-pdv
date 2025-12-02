package br.com.churrasco.controller;

import br.com.churrasco.dao.CaixaDAO;
import br.com.churrasco.dao.VendaDAO;
import br.com.churrasco.model.Caixa;
import br.com.churrasco.service.ImpressoraService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CaixaController {

    @FXML private TextField txtValor;
    @FXML private Label lblSaldoSistema;
    @FXML private Label lblDiferenca;

    private final CaixaDAO caixaDAO = new CaixaDAO();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final ImpressoraService impressoraService = new ImpressoraService();

    private boolean confirmado = false;
    private Caixa caixaAberto;

    @FXML
    public void initialize() {
        configurarMascaraDinheiro();
    }

    // --- MASCARA TIPO CAIXA ELETRÔNICO ---
    private void configurarMascaraDinheiro() {
        if(txtValor == null) return;
        txtValor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            String digitos = newValue.replaceAll("[^0-9]", "");
            if (digitos.isEmpty()) digitos = "0";
            long valorLong = Long.parseLong(digitos);
            double valorDecimal = valorLong / 100.0;
            String formatado = String.format("%.2f", valorDecimal);
            if (!newValue.equals(formatado)) {
                txtValor.setText(formatado);
                txtValor.positionCaret(formatado.length());
            }
        });
    }

    // --- LÓGICA DE ABERTURA ---
    @FXML
    public void confirmarAbertura() {
        try {
            double valor = lerValorMonetario();
            caixaDAO.abrirCaixa(valor);
            this.confirmado = true;
            fecharJanela();
        } catch (Exception e) {
            mostrarAlerta("Valor inválido: " + e.getMessage());
        }
    }

    // --- LÓGICA DE FECHAMENTO ---
    public void carregarDadosFechamento() {
        this.caixaAberto = caixaDAO.buscarCaixaAberto();
        if (caixaAberto == null) return;

        // Calcula quanto deve ter de DINHEIRO FÍSICO (Saldo Inicial + Vendas em Dinheiro)
        double vendasDinheiro = caixaDAO.calcularSaldoDinheiroSistema(caixaAberto.getId());
        double totalEsperadoNaGaveta = caixaAberto.getSaldoInicial() + vendasDinheiro;

        caixaAberto.setSaldoFinal(totalEsperadoNaGaveta);

        lblSaldoSistema.setText(String.format("R$ %.2f", totalEsperadoNaGaveta));

        // Listener para atualizar a diferença visualmente enquanto digita
        txtValor.textProperty().addListener((obs, old, novo) -> atualizarDiferenca(totalEsperadoNaGaveta));

        Platform.runLater(() -> txtValor.requestFocus());
    }

    private void atualizarDiferenca(double esperado) {
        try {
            double informado = lerValorMonetario();
            double dif = informado - esperado;

            lblDiferenca.setText(String.format("Diferença: R$ %.2f", dif));

            if (dif < -0.01) lblDiferenca.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            else if (dif > 0.01) lblDiferenca.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            else lblDiferenca.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Verde se bater (Zero)

        } catch (Exception e) {}
    }

    @FXML
    public void confirmarFechamento() {
        try {
            double informado = lerValorMonetario();
            double esperado = caixaAberto.getSaldoFinal();
            double diferenca = informado - esperado;

            // 1. Fecha no Banco de Dados
            caixaDAO.fecharCaixa(caixaAberto.getId(), esperado, informado, diferenca);

            // Atualiza objeto local para uso nos relatórios
            caixaAberto.setSaldoInformado(informado);
            caixaAberto.setDiferenca(diferenca);

            // 2. Pergunta se quer IMPRIMIR
            Alert printAlert = new Alert(Alert.AlertType.CONFIRMATION, "Caixa Fechado! Deseja imprimir o comprovante?", ButtonType.YES, ButtonType.NO);
            if (printAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                imprimirFechamento();
            }

            // 3. Mostra o Relatório Visual na Tela (Amarelinho)
            exibirRelatorioFinal(informado, diferenca);

            this.confirmado = true;
            fecharJanela();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Erro ao fechar: " + e.getMessage());
        }
    }

    private void imprimirFechamento() {
        // Busca dados para impressão
        // Nota: usamos a data de abertura do caixa para filtrar as vendas (caso esteja fechando um caixa de ontem)
        LocalDate dataCaixa = caixaAberto.getDataAbertura().toLocalDate();

        double tDin = caixaDAO.calcularSaldoDinheiroSistema(caixaAberto.getId());
        double tPix = vendaDAO.buscarTotalPorTipo(dataCaixa, "PIX");
        double tDeb = vendaDAO.buscarTotalPorTipo(dataCaixa, "DÉBITO");
        double tCre = vendaDAO.buscarTotalPorTipo(dataCaixa, "CRÉDITO");

        new Thread(() -> {
            impressoraService.imprimirRelatorioFechamento(caixaAberto, tDin, tPix, tDeb, tCre);
        }).start();
    }

    private void exibirRelatorioFinal(double informado, double diferenca) {
        try {
            // Busca dados para o visual
            LocalDate dataCaixa = caixaAberto.getDataAbertura().toLocalDate();
            double tPix = vendaDAO.buscarTotalPorTipo(dataCaixa, "PIX");
            double tDeb = vendaDAO.buscarTotalPorTipo(dataCaixa, "DÉBITO");
            double tCre = vendaDAO.buscarTotalPorTipo(dataCaixa, "CRÉDITO");
            double tDinVendas = caixaDAO.calcularSaldoDinheiroSistema(caixaAberto.getId());

            StringBuilder sb = new StringBuilder();
            sb.append("COMPROVANTE DE FECHAMENTO\n");
            sb.append("Data: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
            sb.append("--------------------------------\n");
            sb.append("MOVIMENTAÇÃO DO DIA:\n");
            sb.append(String.format("Fundo Troco:        R$ %8.2f\n", caixaAberto.getSaldoInicial()));
            sb.append(String.format("(+) Vendas Dinheiro:R$ %8.2f\n", tDinVendas));
            sb.append(String.format("(+) Vendas Pix:     R$ %8.2f\n", tPix));
            sb.append(String.format("(+) Vendas Débito:  R$ %8.2f\n", tDeb));
            sb.append(String.format("(+) Vendas Crédito: R$ %8.2f\n", tCre));
            sb.append("--------------------------------\n");
            sb.append("CONFERÊNCIA GAVETA (ESPÉCIE):\n");
            sb.append(String.format("Esperado (Sist):    R$ %8.2f\n", caixaAberto.getSaldoFinal()));
            sb.append(String.format("Contado (Você):     R$ %8.2f\n", informado));
            sb.append("--------------------------------\n");
            sb.append(String.format("DIFERENÇA:          R$ %8.2f\n", diferenca));

            if (diferenca == 0) sb.append("STATUS: CAIXA BATEU! OK\n");
            else if (diferenca > 0) sb.append("STATUS: SOBRA DE CAIXA\n");
            else sb.append("STATUS: QUEBRA DE CAIXA (FALTA)\n");

            // Abre o Amarelinho
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Confirmacao.fxml"));
            Parent root = loader.load();
            ConfirmacaoController controller = loader.getController();
            controller.setTextoCupom(sb.toString());
            controller.ativarModoLeitura(); // Bloqueia botões de ação

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Relatório Final");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double lerValorMonetario() {
        String limpo = txtValor.getText().replaceAll("[^0-9]", "");
        if(limpo.isEmpty()) limpo = "0";
        return Double.parseDouble(limpo) / 100.0;
    }

    private void fecharJanela() {
        if (txtValor.getScene() != null) {
            ((Stage) txtValor.getScene().getWindow()).close();
        }
    }

    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public boolean isConfirmado() { return confirmado; }
}