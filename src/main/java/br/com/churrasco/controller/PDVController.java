package br.com.churrasco.controller;

import br.com.churrasco.dao.ProdutoDAO;
import br.com.churrasco.dao.VendaDAO;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import br.com.churrasco.model.Produto;
import br.com.churrasco.model.Venda;
import br.com.churrasco.service.BalancaService;
import br.com.churrasco.service.ImpressoraService;
import br.com.churrasco.util.CupomGenerator;
import br.com.churrasco.util.Navegacao; // Importante para navegação
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

    // Serviços
    private final ImpressoraService impressoraService = new ImpressoraService();
    private final BalancaService balancaService = new BalancaService();

    // DAOs
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final VendaDAO vendaDAO = new VendaDAO();

    // Estado
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

    // --- MÉTODOS PRINCIPAIS ---

    @FXML
    public void finalizarVenda() {
        if (carrinho.isEmpty()) {
            mostrarAlerta("Carrinho vazio!", Alert.AlertType.WARNING);
            return;
        }
        try {
            // 1. Pagamento
            PagamentoController pagController = abrirModalPagamento();
            if (!pagController.isConfirmado()) return;

            List<Pagamento> pagamentos = pagController.getPagamentosRealizados();
            double totalVenda = calcularTotalCarrinho();

            // 2. Conferência
            ConfirmacaoController confController = abrirModalConfirmacao(pagamentos, totalVenda);
            if (!confController.isConfirmado()) return;

            // 3. Salvar e Imprimir
            salvarVendaNoBanco(totalVenda, pagamentos);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Erro Crítico: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void salvarVendaNoBanco(double total, List<Pagamento> pagamentos) {
        try {
            Venda venda = new Venda();
            venda.setDataHora(LocalDateTime.now());
            venda.setValorTotal(total);
            venda.setFormaPagamento(determinarTipoPagamento(pagamentos));

            // Salva no Banco
            int idVenda = vendaDAO.salvarVenda(venda, new ArrayList<>(carrinho), pagamentos);
            venda.setId(idVenda);

            // Imprime em Thread separada
            List<ItemVenda> itensCopia = new ArrayList<>(carrinho);
            List<Pagamento> pagsCopia = new ArrayList<>(pagamentos);
            new Thread(() -> impressoraService.imprimirCupom(venda, itensCopia, pagsCopia)).start();

            // Limpa e Avisa
            resetarPDV();
            mostrarAlerta("Venda Nº " + idVenda + " realizada com sucesso!", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            mostrarAlerta("ERRO AO SALVAR: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // --- LÓGICA DE PRODUTO E PESO ---

    private void buscarProduto() {
        String codigo = txtCodigo.getText();
        if (codigo.isEmpty()) return;

        Produto p = produtoDAO.buscarPorCodigo(codigo);
        if (p != null) {
            produtoAtual = p;
            lblProdutoIdentificado.setText(p.getNome());

            if ("KG".equals(p.getUnidade())) {
                txtPeso.setDisable(false);
                txtPeso.setText("");
                txtPeso.setPromptText("F2 p/ Ler ou Digite...");
                txtPeso.requestFocus();
            } else {
                txtPeso.setText("1");
                adicionarAoCarrinho(); // Adição direta para itens UN
            }
        } else {
            lblProdutoIdentificado.setText("PRODUTO NÃO ENCONTRADO");
            txtCodigo.selectAll();
        }
    }

    // Método inteligente que serve tanto para ADICIONAR quanto para EDITAR peso
    private void processarInputPeso() {
        try {
            String textoPeso = txtPeso.getText().replace(",", ".");
            if (textoPeso.isEmpty()) return;
            double qtd = Double.parseDouble(textoPeso);
            if (qtd <= 0) return;

            if (itemEmEdicao != null) {
                // Edição
                itemEmEdicao.setQuantidade(qtd);
                tabelaItens.refresh();
                tabelaItens.getSelectionModel().clearSelection();
                cancelarEdicao();
                limparCamposAposInsercao();
                lblProdutoIdentificado.setText("ITEM ATUALIZADO!");
            } else {
                // Inserção Nova
                carrinho.add(new ItemVenda(produtoAtual, qtd));
                limparCamposAposInsercao();
            }
            atualizarTotaisVisualmente();
        } catch (NumberFormatException e) {
            System.out.println("Erro peso inválido");
        }
    }

    // Método simples usado apenas pelo buscarProduto (itens unitários)
    private void adicionarAoCarrinho() {
        try {
            double qtd = Double.parseDouble(txtPeso.getText().replace(",", "."));
            carrinho.add(new ItemVenda(produtoAtual, qtd));
            atualizarTotaisVisualmente();
            limparCamposAposInsercao();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void lerPesoBalanca() {
        if (txtPeso.isDisabled()) return;
        lblProdutoIdentificado.setText("LENDO BALANÇA...");

        new Thread(() -> {
            try {
                // Passamos NULL para que o Service busque a porta no Banco de Dados (Configurações)
                Double peso = balancaService.lerPeso(); // Removido argumento null

                Platform.runLater(() -> {
                    String pesoTexto = String.format("%.3f", peso);
                    txtPeso.setText(pesoTexto);
                    if (produtoAtual != null) lblProdutoIdentificado.setText(produtoAtual.getNome());
                    txtPeso.requestFocus();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblProdutoIdentificado.setText("ERRO: " + e.getMessage());
                    txtPeso.requestFocus();
                });
            }
        }).start();
    }

    // --- CONFIGURAÇÕES DE TELA E EVENTOS ---

    private void resetarPDV() {
        carrinho.clear();
        atualizarTotaisVisualmente();
        atualizarNumeroVenda();
        limparCamposAposInsercao();
        if (lblProdutoIdentificado != null) lblProdutoIdentificado.setText("CAIXA LIVRE");
    }

    private void limparCamposAposInsercao() {
        txtCodigo.setText("");
        txtPeso.setText("");
        txtPeso.setDisable(true);
        if (lblProdutoIdentificado != null) lblProdutoIdentificado.setText("AGUARDANDO...");
        Platform.runLater(() -> txtCodigo.requestFocus());
        produtoAtual = null;
    }

    private void configurarEventosGlobais() {
        txtCodigo.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) buscarProduto();
            else if (event.getCode() == KeyCode.F2) { lerPesoBalanca(); event.consume(); }
            else processarAtalhoGlobal(event);
        });
        txtPeso.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) processarInputPeso();
            else if (event.getCode() == KeyCode.F2) { lerPesoBalanca(); event.consume(); }
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
                limparCamposAposInsercao();
            } else {
                tentarSair();
            }
        }
    }

    // --- TABELA (Células e Edição) ---

    private void configurarTabela() {
        colNome.setCellValueFactory(new PropertyValueFactory<>("nomeProduto"));
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalItem"));

        // Formatação: KG ou UN
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

        // Formatação Moeda (Preço e Total)
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

    private void configurarSelecaoTabela() {
        tabelaItens.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && "KG".equals(newVal.getProduto().getUnidade())) {
                itemEmEdicao = newVal;
                produtoAtual = newVal.getProduto();
                lblProdutoIdentificado.setText("EDITANDO: " + newVal.getNomeProduto());
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
        txtCodigo.setDisable(false);
    }

    private void configurarEventosTabela() {
        tabelaItens.setOnKeyPressed(event -> {
            ItemVenda item = tabelaItens.getSelectionModel().getSelectedItem();
            if (item == null) return;

            if (event.getCode() == KeyCode.DELETE) {
                carrinho.remove(item);
                atualizarTotaisVisualmente();
                if (item == itemEmEdicao) limparCamposAposInsercao();
            }
            // Lógica + e - para UN
            else if ("UN".equals(item.getProduto().getUnidade())) {
                if (event.getCode() == KeyCode.ADD || event.getCode() == KeyCode.PLUS) {
                    item.setQuantidade(item.getQuantidade() + 1);
                } else if (event.getCode() == KeyCode.SUBTRACT || event.getCode() == KeyCode.MINUS) {
                    if (item.getQuantidade() > 1) item.setQuantidade(item.getQuantidade() - 1);
                    else carrinho.remove(item);
                }
                tabelaItens.refresh();
                atualizarTotaisVisualmente();
            }
        });
    }

    // --- UTILS ---

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

    @FXML
    public void acaoFecharCaixa() {
        if (!carrinho.isEmpty()) {
            mostrarAlerta("Finalize a venda atual antes de fechar o caixa.", Alert.AlertType.WARNING);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/FechamentoCaixa.fxml"));
            Parent root = loader.load();
            CaixaController controller = loader.getController();
            controller.carregarDadosFechamento();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            if (controller.isConfirmado()) {
                voltarAoMenu();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Erro ao abrir fechamento: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML public void tentarSair() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voltar ao Menu?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            voltarAoMenu();
        }
    }

    private void voltarAoMenu() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/br/com/churrasco/view/Menu.fxml"));
            Stage stage = (Stage) txtCodigo.getScene().getWindow();
            // Usa setRoot para não perder a maximização
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- MODAIS ---
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

    private String determinarTipoPagamento(List<Pagamento> pagamentos) {
        if (pagamentos == null || pagamentos.isEmpty()) return "DESCONHECIDO";
        if (pagamentos.size() == 1) return pagamentos.get(0).getTipo();
        else return "MISTO";
    }

    private double calcularTotalCarrinho() { return carrinho.stream().mapToDouble(ItemVenda::getTotalItem).sum(); }
    private void mostrarAlerta(String msg, Alert.AlertType type) { Alert alert = new Alert(type); alert.setContentText(msg); alert.showAndWait(); }
}