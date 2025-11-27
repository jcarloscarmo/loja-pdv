package br.com.churrasco.controller;

import br.com.churrasco.dao.ProdutoDAO;
import br.com.churrasco.dao.VendaDAO;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import br.com.churrasco.model.Produto;
import br.com.churrasco.model.Venda;
import br.com.churrasco.util.CupomGenerator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PDVController {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtPeso;
    @FXML private Label lblTotalVenda;
    @FXML private Label lblProdutoIdentificado;
    @FXML private Label lblNumeroVenda;
    @FXML private Button btnFinalizar;

    @FXML private TableView<ItemVenda> tabelaItens;
    @FXML private TableColumn<ItemVenda, String> colNome;
    @FXML private TableColumn<ItemVenda, Double> colQtd;
    @FXML private TableColumn<ItemVenda, Double> colPreco;
    @FXML private TableColumn<ItemVenda, Double> colTotal;

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final VendaDAO vendaDAO = new VendaDAO();

    private Produto produtoAtual = null;
    private ItemVenda itemEmEdicao = null;

    private final ObservableList<ItemVenda> carrinho = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabela();
        configurarEventosGlobais();
        configurarEventosTabela();
        configurarMascaraPeso();
        configurarSelecaoTabela();
        resetarPDV();
    }

    // --- CONFIGURAÇÃO DA TABELA ---
    private void configurarTabela() {
        colNome.setCellValueFactory(new PropertyValueFactory<>("nomeProduto"));
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalItem"));

        colQtd.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    ItemVenda linha = getTableView().getItems().get(getIndex());
                    String sufixo = "KG".equals(linha.getProduto().getUnidade()) ? "kg" : "un";
                    setText(String.format("%.3f %s", item, sufixo));
                }
            }
        });

        colPreco.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("R$ %.2f", item));
            }
        });

        colTotal.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("R$ %.2f", item));
            }
        });

        tabelaItens.setItems(carrinho);
    }

    // --- LÓGICA DE EDIÇÃO DE PESO ---
    private void configurarSelecaoTabela() {
        tabelaItens.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && "KG".equals(newVal.getProduto().getUnidade())) {
                itemEmEdicao = newVal;
                produtoAtual = newVal.getProduto();
                lblProdutoIdentificado.setText("EDITANDO: " + newVal.getNomeProduto());
                lblProdutoIdentificado.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-background-color: #fdedec; -fx-padding: 10; -fx-background-radius: 5;");
                txtPeso.setDisable(false);
                long pesoFormatado = (long) (newVal.getQuantidade() * 1000);
                txtPeso.setText(String.format("%d", pesoFormatado));
                Platform.runLater(() -> { txtPeso.requestFocus(); txtPeso.selectAll(); });
                txtCodigo.setDisable(true);
            } else {
                cancelarEdicao();
            }
        });
    }

    private void cancelarEdicao() {
        itemEmEdicao = null;
        if (produtoAtual != null) {
            lblProdutoIdentificado.setText(produtoAtual.getNome());
            lblProdutoIdentificado.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: #d6eaf8; -fx-padding: 10; -fx-background-radius: 5;");
        } else {
            lblProdutoIdentificado.setText("AGUARDANDO...");
            lblProdutoIdentificado.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: #d6eaf8; -fx-padding: 10; -fx-background-radius: 5;");
        }
        txtCodigo.setDisable(false);
    }

    private void processarInputPeso() {
        try {
            String textoPeso = txtPeso.getText().replace(",", ".");
            if (textoPeso.isEmpty()) return;
            double qtd = Double.parseDouble(textoPeso);
            if (qtd <= 0) return;

            if (itemEmEdicao != null) {
                itemEmEdicao.setQuantidade(qtd);
                tabelaItens.refresh();
                tabelaItens.getSelectionModel().clearSelection();
                cancelarEdicao();
                txtPeso.setText(""); txtPeso.setDisable(true); txtCodigo.requestFocus();
                produtoAtual = null;
                lblProdutoIdentificado.setText("ITEM ATUALIZADO!");
            } else {
                carrinho.add(new ItemVenda(produtoAtual, qtd));
                txtCodigo.setText(""); txtPeso.setText(""); txtPeso.setDisable(true);
                lblProdutoIdentificado.setText("AGUARDANDO..."); txtCodigo.requestFocus();
                produtoAtual = null;
            }
            atualizarTotaisVisualmente();
        } catch (NumberFormatException e) { System.out.println("Erro peso"); }
    }

    // --- EVENTOS E ATALHOS ---
    private void configurarEventosGlobais() {
        txtCodigo.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) buscarProduto();
            else processarAtalhoGlobal(event);
        });
        txtPeso.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) processarInputPeso();
            else processarAtalhoGlobal(event);
        });
    }

    private void processarAtalhoGlobal(KeyEvent event) {
        if (event.getCode() == KeyCode.DIVIDE || event.getCode() == KeyCode.SLASH) {
            event.consume(); finalizarVenda();
        } else if (event.getCode() == KeyCode.F5) {
            finalizarVenda();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            if (itemEmEdicao != null) {
                tabelaItens.getSelectionModel().clearSelection();
                txtPeso.setText(""); txtPeso.setDisable(true); txtCodigo.requestFocus();
            } else {
                tentarSair();
            }
        }
    }

    private void resetarPDV() {
        carrinho.clear();
        atualizarTotaisVisualmente();
        atualizarNumeroVenda();
        txtCodigo.setText(""); txtPeso.setText(""); txtPeso.setDisable(true);
        if (lblProdutoIdentificado != null) {
            lblProdutoIdentificado.setText("CAIXA LIVRE");
            lblProdutoIdentificado.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: #d6eaf8; -fx-padding: 10; -fx-background-radius: 5;");
        }
        itemEmEdicao = null;
        Platform.runLater(() -> txtCodigo.requestFocus());
        produtoAtual = null;
    }

    private void buscarProduto() {
        String codigo = txtCodigo.getText();
        if (codigo.isEmpty()) return;
        Produto p = produtoDAO.buscarPorCodigo(codigo);
        if (p != null) {
            produtoAtual = p;
            lblProdutoIdentificado.setText(p.getNome());
            if ("KG".equals(p.getUnidade())) {
                txtPeso.setDisable(false); txtPeso.setText(""); txtPeso.requestFocus();
            } else {
                txtPeso.setText("1"); processarInputPeso();
            }
        } else {
            lblProdutoIdentificado.setText("PRODUTO NÃO ENCONTRADO"); txtCodigo.selectAll();
        }
    }

    private void configurarEventosTabela() {
        tabelaItens.setOnKeyPressed(event -> {
            ItemVenda itemSelecionado = tabelaItens.getSelectionModel().getSelectedItem();
            if (itemSelecionado == null) return;
            KeyCode tecla = event.getCode();

            if (tecla == KeyCode.DELETE) {
                carrinho.remove(itemSelecionado);
                atualizarTotaisVisualmente();
                event.consume();
                if (itemSelecionado == itemEmEdicao) {
                    tabelaItens.getSelectionModel().clearSelection();
                    txtPeso.setText(""); txtPeso.setDisable(true); txtCodigo.requestFocus();
                }
            }
            else if ("UN".equals(itemSelecionado.getProduto().getUnidade())) {
                if (tecla == KeyCode.ADD || tecla == KeyCode.PLUS) {
                    itemSelecionado.setQuantidade(itemSelecionado.getQuantidade() + 1);
                    tabelaItens.refresh();
                    atualizarTotaisVisualmente();
                    event.consume();
                }
                else if (tecla == KeyCode.SUBTRACT || tecla == KeyCode.MINUS) {
                    double novaQtd = itemSelecionado.getQuantidade() - 1;
                    if (novaQtd > 0) {
                        itemSelecionado.setQuantidade(novaQtd);
                        tabelaItens.refresh();
                    } else {
                        carrinho.remove(itemSelecionado);
                    }
                    atualizarTotaisVisualmente();
                    event.consume();
                }
            }
        });
    }

    private void configurarMascaraPeso() {
        txtPeso.textProperty().addListener((obs, old, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            if (produtoAtual != null && "KG".equals(produtoAtual.getUnidade())) {
                String digitos = newValue.replaceAll("[^0-9]", "");
                if (digitos.isEmpty()) digitos = "0";
                String formatado = String.format("%.3f", Long.parseLong(digitos) / 1000.0);
                if (!newValue.equals(formatado)) {
                    txtPeso.setText(formatado);
                    txtPeso.positionCaret(formatado.length());
                }
            }
        });
    }

    private void atualizarTotaisVisualmente() {
        lblTotalVenda.setText(String.format("R$ %.2f", calcularTotalCarrinho()));
        tabelaItens.refresh();
    }

    private void atualizarNumeroVenda() {
        try {
            int proximoId = vendaDAO.getProximoIdVenda();
            if (lblNumeroVenda != null) lblNumeroVenda.setText("VENDA Nº " + proximoId);
        } catch (Exception e) { if (lblNumeroVenda != null) lblNumeroVenda.setText("VENDA Nº --"); }
    }

    @FXML public void finalizarVenda() {
        if (carrinho.isEmpty()) { mostrarAlerta("Carrinho vazio!", Alert.AlertType.WARNING); return; }
        try {
            PagamentoController pagController = abrirModalPagamento();
            if (!pagController.isConfirmado()) return;
            List<Pagamento> pagamentos = pagController.getPagamentosRealizados();
            double totalVenda = calcularTotalCarrinho();
            ConfirmacaoController confController = abrirModalConfirmacao(pagamentos, totalVenda);
            if (!confController.isConfirmado()) return;
            salvarVendaNoBanco(totalVenda, pagamentos);
        } catch (Exception e) { e.printStackTrace(); mostrarAlerta("Erro: " + e.getMessage(), Alert.AlertType.ERROR); }
    }

    private void salvarVendaNoBanco(double total, List<Pagamento> pagamentos) {
        try {
            Venda venda = new Venda();
            venda.setDataHora(LocalDateTime.now());
            venda.setValorTotal(total);

            // Usa a lógica de MISTO/DÉBITO/PIX
            venda.setFormaPagamento(determinarTipoPagamento(pagamentos));

            int idVenda = vendaDAO.salvarVenda(venda, new ArrayList<>(carrinho), pagamentos);
            resetarPDV();
            mostrarAlerta("Venda Nº " + idVenda + " realizada com sucesso!", Alert.AlertType.INFORMATION);
        } catch (Exception e) { mostrarAlerta("ERRO AO SALVAR: " + e.getMessage(), Alert.AlertType.ERROR); }
    }

    // --- JANELAS MODAIS E NAVEGAÇÃO ---

    private PagamentoController abrirModalPagamento() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Pagamento.fxml"));
        Parent root = loader.load();
        PagamentoController controller = loader.getController();
        controller.setValorTotal(calcularTotalCarrinho());
        Stage stage = new Stage(); stage.setScene(new Scene(root)); stage.initModality(Modality.APPLICATION_MODAL); stage.showAndWait();
        return controller;
    }

    private ConfirmacaoController abrirModalConfirmacao(List<Pagamento> pagamentos, double total) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Confirmacao.fxml"));
        Parent root = loader.load();
        ConfirmacaoController controller = loader.getController();
        controller.setTextoCupom(CupomGenerator.gerarTexto(carrinho, pagamentos, total));
        Stage stage = new Stage(); stage.setScene(new Scene(root)); stage.initModality(Modality.APPLICATION_MODAL); stage.showAndWait();
        return controller;
    }

    @FXML
    public void tentarSair() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voltar ao Menu?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            voltarAoMenu();
        }
    }

    @FXML
    public void acaoFecharCaixa() {
        if (!carrinho.isEmpty()) {
            mostrarAlerta("Você tem itens lançados! Finalize ou cancele a venda antes de fechar o caixa.", Alert.AlertType.WARNING);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/FechamentoCaixa.fxml"));
            Parent root = loader.load();
            CaixaController controller = loader.getController();
            controller.carregarDadosFechamento();
            Stage stage = new Stage(); stage.setScene(new Scene(root)); stage.initModality(Modality.APPLICATION_MODAL); stage.showAndWait();

            if (controller.isConfirmado()) {
                voltarAoMenu();
            }
        } catch (Exception e) { e.printStackTrace(); mostrarAlerta("Erro ao abrir fechamento: " + e.getMessage(), Alert.AlertType.ERROR); }
    }

    // Método essencial que estava faltando no código que você enviou
    private void voltarAoMenu() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/br/com/churrasco/view/Menu.fxml"));
            Stage stage = (Stage) txtCodigo.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String determinarTipoPagamento(List<Pagamento> pagamentos) {
        if (pagamentos == null || pagamentos.isEmpty()) return "DESCONHECIDO";
        if (pagamentos.size() == 1) return pagamentos.get(0).getTipo();
        else return "MISTO";
    }

    private double calcularTotalCarrinho() { return carrinho.stream().mapToDouble(ItemVenda::getTotalItem).sum(); }
    private void mostrarAlerta(String msg, Alert.AlertType type) { Alert alert = new Alert(type); alert.setContentText(msg); alert.showAndWait(); }
}