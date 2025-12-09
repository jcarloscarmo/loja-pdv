package br.com.churrasco.controller;

import br.com.churrasco.dao.ProdutoDAO;
import br.com.churrasco.dao.UsuarioDAO;
import br.com.churrasco.dao.VendaDAO;
import br.com.churrasco.model.*;
import br.com.churrasco.service.BalancaService;
import br.com.churrasco.service.ImpressoraService;
import br.com.churrasco.util.CupomGenerator;
import br.com.churrasco.util.Sessao;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PDVController {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtPeso;
    @FXML private Label lblTotalVenda;
    @FXML private Label lblProdutoIdentificado;
    @FXML private Label lblNumeroVenda;
    @FXML private Button btnFinalizar;

    @FXML private HBox boxEncomendas;

    @FXML private TableView<ItemVenda> tabelaItens;
    @FXML private TableColumn<ItemVenda, String> colNome;
    @FXML private TableColumn<ItemVenda, Double> colQtd;
    @FXML private TableColumn<ItemVenda, Double> colPreco;
    @FXML private TableColumn<ItemVenda, Double> colTotal;

    private final ImpressoraService impressoraService = new ImpressoraService();
    private final BalancaService balancaService = new BalancaService();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final VendaDAO vendaDAO = new VendaDAO();

    private Produto produtoAtual = null;
    private ItemVenda itemEmEdicao = null;
    private final ObservableList<ItemVenda> carrinho = FXCollections.observableArrayList();
    private final List<Encomenda> encomendasAbertas = new ArrayList<>();
    private Integer idEncomendaEmAndamento = null;

    @FXML
    public void initialize() {
        configurarTabela();
        configurarEventosGlobais();
        configurarEventosTabela();
        configurarMascaraPeso();
        configurarSelecaoTabela();

        resetarPDV();
        recuperarEncomendasPendentes();
    }

    // --- ENCOMENDAS (CORREÇÃO DE ERRO DE COMPILAÇÃO AQUI) ---
    @FXML
    public void abrirNovaEncomenda(ActionEvent event) {
        if (carrinho.isEmpty()) {
            mostrarAlerta("O carrinho está vazio.", Alert.AlertType.WARNING);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/NovaEncomenda.fxml"));
            if (loader.getLocation() == null) loader = new FXMLLoader(getClass().getResource("/NovaEncomenda.fxml"));

            Parent root = loader.load();
            NovaEncomendaController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Nova Encomenda");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            stage.addEventHandler(KeyEvent.KEY_PRESSED, k -> {
                if (k.getText().equals("*") || k.getCode() == KeyCode.MULTIPLY) {
                    try { controller.salvar(); } catch(Exception ex){}
                }
            });

            stage.showAndWait();

            if (controller.isConfirmado()) {
                Encomenda nova = controller.getEncomendaCriada();
                if (nova != null) {
                    nova.setItens(new ArrayList<>(carrinho));
                    nova.setStatus("PENDENTE");

                    if (idEncomendaEmAndamento != null) nova.setId(idEncomendaEmAndamento);

                    // AQUI DAVA ERRO: O catch precisava ser Exception, não só IOException
                    vendaDAO.salvarEncomenda(nova);

                    encomendasAbertas.add(nova);
                    criarCardEncomenda(nova);
                    mostrarAlerta("Encomenda salva! Estoque reservado.", Alert.AlertType.INFORMATION);
                    resetarPDV();
                }
            }
        } catch (Exception e) { // <--- CORREÇÃO: Catch genérico para pegar erros de Banco E Tela
            e.printStackTrace();
            mostrarAlerta("Erro ao processar encomenda: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // --- SEGURANÇA E FECHAMENTO ---
    @FXML
    public void acaoFecharCaixa() {
        if (!carrinho.isEmpty()) {
            mostrarAlerta("Finalize a venda atual antes de fechar o caixa.", Alert.AlertType.WARNING);
            return;
        }
        if (!encomendasAbertas.isEmpty()) {
            mostrarAlerta("Existem encomendas pendentes! Finalize ou cancele antes de fechar.", Alert.AlertType.ERROR);
            return;
        }
        if (Sessao.getUsuario() != null && !Sessao.getUsuario().isAdmin()) {
            boolean autorizado = solicitarSenhaAdmin("FECHAMENTO DE CAIXA");
            if (!autorizado) return;
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

    private boolean solicitarSenhaAdmin(String acao) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Autorização de Gerente");
        dialog.setHeaderText("Ação Bloqueada: " + acao);
        ButtonType btnLiberar = new ButtonType("Liberar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnLiberar, ButtonType.CANCEL);
        VBox vbox = new VBox(10);
        Label label = new Label("Digite a senha do Tião:");
        PasswordField passField = new PasswordField(); passField.setPromptText("Senha");
        vbox.getChildren().addAll(label, passField);
        dialog.getDialogPane().setContent(vbox);
        Platform.runLater(passField::requestFocus);
        dialog.setResultConverter(dialogButton -> (dialogButton == btnLiberar) ? passField.getText() : null);
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            Usuario admin = new UsuarioDAO().autenticar("Tião", result.get());
            if (admin != null && admin.isAdmin()) return true;
            mostrarAlerta("Senha incorreta! Ação negada.", Alert.AlertType.ERROR);
        }
        return false;
    }

    // --- VENDA ---
    @FXML public void finalizarVenda() {
        if (carrinho.isEmpty()) { mostrarAlerta("Carrinho vazio!", Alert.AlertType.WARNING); return; }
        try {
            PagamentoController pag = abrirModalPagamento();
            if (!pag.isConfirmado()) return;
            ConfirmacaoController conf = abrirModalConfirmacao(pag.getPagamentosRealizados(), calcularTotalCarrinho());
            if (!conf.isConfirmado()) return;
            salvarVendaNoBanco(calcularTotalCarrinho(), pag.getPagamentosRealizados());
        } catch (Exception e) { e.printStackTrace(); mostrarAlerta("Erro Crítico: " + e.getMessage(), Alert.AlertType.ERROR); }
    }

    private void salvarVendaNoBanco(double total, List<Pagamento> pagamentos) {
        try {
            Venda venda = new Venda();
            venda.setDataHora(LocalDateTime.now());
            venda.setValorTotal(total);
            venda.setFormaPagamento(determinarTipoPagamento(pagamentos));
            if (idEncomendaEmAndamento != null) venda.setId(idEncomendaEmAndamento);
            int id = vendaDAO.salvarVenda(venda, new ArrayList<>(carrinho), pagamentos);
            venda.setId(id);
            List<ItemVenda> itensCopia = new ArrayList<>(carrinho); List<Pagamento> pagsCopia = new ArrayList<>(pagamentos);
            new Thread(() -> impressoraService.imprimirCupom(venda, itensCopia, pagsCopia)).start();
            resetarPDV();
            mostrarAlerta("Venda Nº " + id + " realizada com sucesso!", Alert.AlertType.INFORMATION);
        } catch (Exception e) { mostrarAlerta("ERRO AO SALVAR: " + e.getMessage(), Alert.AlertType.ERROR); }
    }

    private void recuperarEncomendasPendentes() {
        boxEncomendas.getChildren().clear(); encomendasAbertas.clear();
        Thread t = new Thread(() -> {
            try {
                List<Encomenda> pendentes = vendaDAO.buscarTodasEncomendasPendentes();
                Platform.runLater(() -> {
                    if (pendentes != null) {
                        for (Encomenda enc : pendentes) { encomendasAbertas.add(enc); criarCardEncomenda(enc); }
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }); t.setDaemon(true); t.start();
    }

    private void criarCardEncomenda(Encomenda encomenda) {
        if (boxEncomendas == null) return;
        String idTexto = (encomenda.getId() != null) ? "#" + encomenda.getId() : "";
        Button btnCard = new Button("ENC " + idTexto + "\n" + encomenda.getNomeCliente());
        btnCard.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 120; -fx-min-height: 50; -fx-background-radius: 5;");
        HBox.setMargin(btnCard, new javafx.geometry.Insets(0, 5, 0, 5));

        btnCard.setOnAction(e -> {
            ButtonType btnAbrir = new ButtonType("ABRIR/RESGATAR", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancelar = new ButtonType("CANCELAR PEDIDO", ButtonBar.ButtonData.OTHER);
            ButtonType btnFechar = new ButtonType("FECHAR", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert info = new Alert(Alert.AlertType.CONFIRMATION);
            info.setTitle("Gerenciar Encomenda " + idTexto);
            info.setHeaderText("Cliente: " + encomenda.getNomeCliente());
            double total = (encomenda.getItens() != null) ? encomenda.getItens().stream().mapToDouble(ItemVenda::getTotalItem).sum() : 0.0;
            info.setContentText("Total: R$ " + String.format("%.2f", total) + "\n\nO que deseja fazer?");
            info.getButtonTypes().setAll(btnAbrir, btnCancelar, btnFechar);

            Optional<ButtonType> result = info.showAndWait();

            if (result.isPresent()) {
                if (result.get() == btnAbrir) {
                    if (!carrinho.isEmpty()) { mostrarAlerta("Esvazie o caixa antes de abrir encomenda!", Alert.AlertType.WARNING); return; }
                    if (encomenda.getItens() != null) carrinho.setAll(encomenda.getItens());
                    this.idEncomendaEmAndamento = encomenda.getId();
                    atualizarTotaisVisualmente(); atualizarNumeroVenda();
                    encomendasAbertas.remove(encomenda); boxEncomendas.getChildren().remove(btnCard);
                    mostrarAlerta("Encomenda aberta! Itens carregados.", Alert.AlertType.INFORMATION);
                } else if (result.get() == btnCancelar) {
                    Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Deseja cancelar esta encomenda? O estoque será devolvido.", ButtonType.YES, ButtonType.NO);
                    if (confirmacao.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        vendaDAO.cancelarEncomenda(encomenda.getId());
                        encomendasAbertas.remove(encomenda); boxEncomendas.getChildren().remove(btnCard);
                        mostrarAlerta("Encomenda cancelada e estoque estornado.", Alert.AlertType.INFORMATION);
                    }
                }
            }
        });
        boxEncomendas.getChildren().add(btnCard);
    }

    // --- AUXILIARES, CONFIGURAÇÕES E MÁSCARAS (Mantidos do anterior) ---
    private void resetarPDV() {
        carrinho.clear(); atualizarTotaisVisualmente(); limparCamposAposInsercao();
        idEncomendaEmAndamento = null; atualizarNumeroVenda();
        if (lblProdutoIdentificado != null) lblProdutoIdentificado.setText("CAIXA LIVRE");
    }
    private void atualizarNumeroVenda() { try { int id = (idEncomendaEmAndamento != null) ? idEncomendaEmAndamento : vendaDAO.getProximoIdVenda(); if (lblNumeroVenda != null) lblNumeroVenda.setText("VENDA Nº " + id); } catch (Exception e) {} }
    private void buscarProduto() {
        String codigo = txtCodigo.getText(); if (codigo.isEmpty()) return;
        Produto p = produtoDAO.buscarPorCodigo(codigo);
        if (p != null) {
            produtoAtual = p; lblProdutoIdentificado.setText(p.getNome());
            if ("KG".equals(p.getUnidade())) { txtPeso.setDisable(false); txtPeso.setText(""); txtPeso.requestFocus(); }
            else { txtPeso.setText("1"); adicionarAoCarrinho(); }
        } else { lblProdutoIdentificado.setText("NÃO ENCONTRADO"); txtCodigo.selectAll(); }
    }
    private void adicionarAoCarrinho() { try { double qtd = Double.parseDouble(txtPeso.getText().replace(",", ".")); carrinho.add(new ItemVenda(produtoAtual, qtd)); atualizarTotaisVisualmente(); limparCamposAposInsercao(); } catch (Exception e) {} }
    private void processarInputPeso() {
        try {
            double qtd = Double.parseDouble(txtPeso.getText().replace(",", "."));
            if (itemEmEdicao != null) { itemEmEdicao.setQuantidade(qtd); cancelarEdicao(); }
            else { carrinho.add(new ItemVenda(produtoAtual, qtd)); }
            atualizarTotaisVisualmente(); limparCamposAposInsercao();
        } catch (Exception e) {}
    }
    private void limparCamposAposInsercao() {
        txtCodigo.setText(""); txtPeso.setText(""); txtPeso.setDisable(true);
        if (lblProdutoIdentificado != null) lblProdutoIdentificado.setText("AGUARDANDO...");
        Platform.runLater(() -> txtCodigo.requestFocus());
        produtoAtual = null; itemEmEdicao = null; tabelaItens.getSelectionModel().clearSelection();
    }
    private void atualizarTotaisVisualmente() { lblTotalVenda.setText(String.format("R$ %.2f", calcularTotalCarrinho())); tabelaItens.refresh(); }
    private void cancelarEdicao() { itemEmEdicao = null; txtCodigo.setDisable(false); }
    private void configurarEventosGlobais() {
        txtCodigo.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) buscarProduto(); });
        txtPeso.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) processarInputPeso(); });
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getText().equals("*") || event.getCode() == KeyCode.MULTIPLY) { event.consume(); abrirNovaEncomenda(null); }
            else if (event.getCode() == KeyCode.DIVIDE || event.getCode() == KeyCode.SLASH || event.getCode() == KeyCode.F5) { event.consume(); finalizarVenda(); }
            else if (event.getCode() == KeyCode.ESCAPE) { event.consume(); tentarSair(); }
        });
    }
    private void configurarTabela() {
        colNome.setCellValueFactory(new PropertyValueFactory<>("nomeProduto"));
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalItem"));
        colQtd.setCellFactory(tc -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText((empty || item == null) ? null : String.format("%.3f", item)); } });
        colPreco.setCellFactory(tc -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText((empty || item == null) ? null : String.format("R$ %.2f", item)); } });
        colTotal.setCellFactory(tc -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText((empty || item == null) ? null : String.format("R$ %.2f", item)); } });
        tabelaItens.setItems(carrinho);
    }
    private void configurarEventosTabela() { tabelaItens.setOnKeyPressed(event -> { ItemVenda item = tabelaItens.getSelectionModel().getSelectedItem(); if (item != null && event.getCode() == KeyCode.DELETE) { carrinho.remove(item); atualizarTotaisVisualmente(); } }); }
    private void configurarMascaraPeso() {
        txtPeso.textProperty().addListener((obs, old, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            String digitos = newValue.replaceAll("[^0-9]", "");
            if (digitos.isEmpty()) return;
            try {
                long valorBruto = Long.parseLong(digitos);
                double valorFinal = valorBruto / 1000.0;
                String formatado = String.format("%.3f", valorFinal);
                if (!newValue.equals(formatado)) { Platform.runLater(() -> { txtPeso.setText(formatado); txtPeso.positionCaret(formatado.length()); }); }
            } catch (NumberFormatException e) { }
        });
    }
    private void configurarSelecaoTabela() { tabelaItens.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null && "KG".equals(newVal.getProduto().getUnidade())) { itemEmEdicao = newVal; produtoAtual = newVal.getProduto(); txtPeso.setDisable(false); txtPeso.setText(String.format("%.3f", newVal.getQuantidade())); txtCodigo.setDisable(true); } }); }
    @FXML public void tentarSair() { voltarAoMenu(); }
    private void voltarAoMenu() { try { Parent root = FXMLLoader.load(getClass().getResource("/br/com/churrasco/view/Menu.fxml")); Stage stage = (Stage) txtCodigo.getScene().getWindow(); stage.getScene().setRoot(root); } catch (Exception e) { e.printStackTrace(); } }
    private PagamentoController abrirModalPagamento() throws IOException { FXMLLoader l = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Pagamento.fxml")); Parent r = l.load(); PagamentoController c = l.getController(); c.setValorTotal(calcularTotalCarrinho()); Stage s = new Stage(); s.setScene(new Scene(r)); s.initModality(Modality.APPLICATION_MODAL); s.showAndWait(); return c; }
    private ConfirmacaoController abrirModalConfirmacao(List<Pagamento> pagamentos, double total) throws IOException { FXMLLoader l = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Confirmacao.fxml")); Parent r = l.load(); ConfirmacaoController c = l.getController(); c.setTextoCupom(CupomGenerator.gerarTexto(carrinho, pagamentos, total)); Stage s = new Stage(); s.setScene(new Scene(r)); s.initModality(Modality.APPLICATION_MODAL); s.showAndWait(); return c; }
    private String determinarTipoPagamento(List<Pagamento> pagamentos) { if (pagamentos == null || pagamentos.isEmpty()) return "DESCONHECIDO"; return pagamentos.size() == 1 ? pagamentos.get(0).getTipo() : "MISTO"; }
    private double calcularTotalCarrinho() { return carrinho.stream().mapToDouble(ItemVenda::getTotalItem).sum(); }
    private void mostrarAlerta(String msg, Alert.AlertType type) { Alert alert = new Alert(type); alert.setContentText(msg); alert.showAndWait(); }
}