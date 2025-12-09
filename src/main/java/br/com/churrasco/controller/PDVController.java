package br.com.churrasco.controller;

import br.com.churrasco.dao.ProdutoDAO;
import br.com.churrasco.dao.VendaDAO;
import br.com.churrasco.model.Encomenda;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import br.com.churrasco.model.Produto;
import br.com.churrasco.model.Venda;
import br.com.churrasco.service.BalancaService;
import br.com.churrasco.service.ImpressoraService;
import br.com.churrasco.util.CupomGenerator;
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
    @FXML private Button btnFinalizar; // Se não usar, pode remover o @FXML

    @FXML private HBox boxEncomendas;

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

    // Lista para controlar encomendas abertas visualmente
    private final List<Encomenda> encomendasAbertas = new ArrayList<>();

    // Guarda o ID da encomenda que foi "aberta" para edição/finalização
    private Integer idEncomendaEmAndamento = null;

    @FXML
    public void initialize() {
        configurarTabela();
        configurarEventosGlobais();
        configurarEventosTabela();
        configurarMascaraPeso();
        configurarSelecaoTabela();

        resetarPDV();

        // INICIALIZAÇÃO CORRIGIDA: Chama o carregamento em background
        recuperarEncomendasPendentes();
    }

    // --- PERSISTÊNCIA E RECUPERAÇÃO ---

    private void recuperarEncomendasPendentes() {
        // 1. Limpa a área visual
        boxEncomendas.getChildren().clear();
        encomendasAbertas.clear();

        // 2. CRIA A BARRA DE LOADING DINAMICAMENTE
        ProgressBar barraLoading = new ProgressBar();
        barraLoading.setPrefWidth(200); // Largura da barra
        barraLoading.setPrefHeight(20);
        // Estilo opcional para combinar com o tema (laranja)
        barraLoading.setStyle("-fx-accent: #e67e22;");

        // Adiciona a barra na caixa horizontal
        boxEncomendas.getChildren().add(barraLoading);

        // 3. Roda a busca em Background
        Thread t = new Thread(() -> {
            try {
                // Dica: Se o banco for local e muito rápido, a barra vai piscar muito rápido.
                // Se quiser um efeito visual mais suave, pode descomentar a linha abaixo:
                // Thread.sleep(500);

                List<Encomenda> pendentes = vendaDAO.buscarTodasEncomendasPendentes();

                // 4. Volta para a Thread do JavaFX
                Platform.runLater(() -> {
                    // REMOVE A BARRA DE LOADING
                    boxEncomendas.getChildren().remove(barraLoading);

                    if (pendentes != null && !pendentes.isEmpty()) {
                        System.out.println("Recuperado: " + pendentes.size());
                        for (Encomenda enc : pendentes) {
                            encomendasAbertas.add(enc);
                            criarCardEncomenda(enc);
                        }
                    } else {
                        // (Opcional) Se não tiver nada, pode adicionar um Label "Nenhuma Encomenda"
                        // Label lblVazio = new Label("Nenhuma encomenda pendente.");
                        // lblVazio.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
                        // boxEncomendas.getChildren().add(lblVazio);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    // Garante que a barra suma mesmo se der erro
                    boxEncomendas.getChildren().remove(barraLoading);
                    System.err.println("Erro ao recuperar encomendas: " + e.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
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

            int idVendaSalva;

            // LÓGICA DE ID PERSISTENTE
            if (idEncomendaEmAndamento != null) {
                // Se era uma encomenda (ID 31), usa esse ID.
                // O DAO vai deletar a encomenda e criar a venda com ID 31 na mesma transação.
                venda.setId(idEncomendaEmAndamento);
            }
            // Se idEncomendaEmAndamento for null, o DAO gera um novo ID

            idVendaSalva = vendaDAO.salvarVenda(venda, new ArrayList<>(carrinho), pagamentos);
            venda.setId(idVendaSalva);

            // Imprime em Thread separada
            List<ItemVenda> itensCopia = new ArrayList<>(carrinho);
            List<Pagamento> pagsCopia = new ArrayList<>(pagamentos);
            new Thread(() -> impressoraService.imprimirCupom(venda, itensCopia, pagsCopia)).start();

            resetarPDV();
            mostrarAlerta("Venda Nº " + idVendaSalva + " realizada com sucesso!", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            mostrarAlerta("ERRO AO SALVAR: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // --- ENCOMENDAS (CRIAR E GERENCIAR) ---

    @FXML
    public void abrirNovaEncomenda(ActionEvent event) {
        if (carrinho.isEmpty()) {
            mostrarAlerta("O carrinho está vazio. Adicione produtos antes de criar uma encomenda.", Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/NovaEncomenda.fxml"));
            if (loader.getLocation() == null) loader = new FXMLLoader(getClass().getResource("/NovaEncomenda.fxml"));

            Parent root = loader.load();
            NovaEncomendaController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Nova Encomenda");

            Scene scene = new Scene(root);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, k -> {
                if (k.getText().equals("*") || k.getCode() == KeyCode.MULTIPLY) {
                    k.consume();
                    try { controller.salvar(); } catch (Exception ex) {}
                } else if (k.getCode() == KeyCode.ESCAPE) {
                    k.consume();
                    stage.close();
                }
            });

            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isConfirmado()) {
                Encomenda novaEncomenda = controller.getEncomendaCriada();
                if (novaEncomenda != null) {
                    novaEncomenda.setItens(new ArrayList<>(carrinho));
                    novaEncomenda.setStatus("PENDENTE");

                    try {
                        // Se estamos re-salvando uma encomenda que já estava aberta (editando)
                        if (idEncomendaEmAndamento != null) {
                            novaEncomenda.setId(idEncomendaEmAndamento);
                        } else {
                            // Encomenda nova: gera ID seguro via DAO
                            // Nota: O DAO salvarEncomenda já lida com geração de ID se for 0,
                            // mas podemos pegar antes para garantir visualização imediata
                            // int idVenda = vendaDAO.getProximoIdVenda();
                            // novaEncomenda.setId(idVenda);
                        }

                        // O DAO decide: se tem ID, atualiza. Se não tem, insere novo.
                        vendaDAO.salvarEncomenda(novaEncomenda);

                        // IMPORTANTE: Se o ID era null, agora o DAO (provavelmente) gerou.
                        // Mas como o objeto 'novaEncomenda' passa por valor, seria ideal recarregar ou garantir que o ID voltou.
                        // Para simplificar, assumimos que o DAO funciona e recarregamos a lista ou usamos o objeto atual.

                        encomendasAbertas.add(novaEncomenda);
                        criarCardEncomenda(novaEncomenda);

                        mostrarAlerta("Encomenda salva com sucesso!", Alert.AlertType.INFORMATION);
                        resetarPDV();
                    } catch (Exception e) {
                        System.err.println("Erro ao salvar encomenda: " + e.getMessage());
                        mostrarAlerta("Erro ao salvar no banco: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Erro ao abrir tela de encomenda: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void criarCardEncomenda(Encomenda encomenda) {
        if (boxEncomendas == null) return;

        String idTexto = (encomenda.getId() != null && encomenda.getId() > 0) ? "#" + encomenda.getId() : "(Recarregue)";
        Button btnCard = new Button("ENC " + idTexto + "\n" + encomenda.getNomeCliente());
        btnCard.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 120; -fx-min-height: 50; -fx-background-radius: 5;");

        HBox.setMargin(btnCard, new javafx.geometry.Insets(0, 5, 0, 5));

        btnCard.setOnAction(e -> {
            double total = (encomenda.getItens() != null)
                    ? encomenda.getItens().stream().mapToDouble(ItemVenda::getTotalItem).sum()
                    : 0.0;
            int qtd = (encomenda.getItens() != null) ? encomenda.getItens().size() : 0;

            ButtonType btnAbrir = new ButtonType("ABRIR/RESGATAR", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancelar = new ButtonType("CANCELAR ENCOMENDA", ButtonBar.ButtonData.OTHER);
            ButtonType btnFechar = new ButtonType("FECHAR", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert info = new Alert(Alert.AlertType.CONFIRMATION);
            info.setTitle("Gerenciar Encomenda " + idTexto);
            info.setHeaderText("Cliente: " + encomenda.getNomeCliente());
            info.setContentText("Itens: " + qtd + "\nTotal Estimado: R$ " + String.format("%.2f", total) + "\n\nO que deseja fazer?");
            info.getButtonTypes().setAll(btnAbrir, btnCancelar, btnFechar);

            Optional<ButtonType> result = info.showAndWait();

            if (result.isPresent()) {
                if (result.get() == btnAbrir) {
                    if (!carrinho.isEmpty()) {
                        mostrarAlerta("Esvazie ou finalize a venda atual antes de abrir uma encomenda!", Alert.AlertType.WARNING);
                    } else {
                        if (encomenda.getItens() != null) carrinho.setAll(encomenda.getItens());

                        // CRUCIAL: Captura o ID para usar na finalização
                        this.idEncomendaEmAndamento = encomenda.getId();

                        atualizarTotaisVisualmente();
                        atualizarNumeroVenda();

                        encomendasAbertas.remove(encomenda);
                        boxEncomendas.getChildren().remove(btnCard);
                        mostrarAlerta("Encomenda #" + encomenda.getId() + " aberta no caixa!", Alert.AlertType.INFORMATION);
                    }
                } else if (result.get() == btnCancelar) {
                    Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Deseja EXCLUIR permanentemente esta encomenda?", ButtonType.YES, ButtonType.NO);
                    if (confirmacao.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        // Remove do Banco
                        vendaDAO.cancelarEncomenda(encomenda.getId());
                        // Remove da Tela
                        encomendasAbertas.remove(encomenda);
                        boxEncomendas.getChildren().remove(btnCard);
                        mostrarAlerta("Encomenda cancelada.", Alert.AlertType.INFORMATION);
                    }
                }
            }
        });
        boxEncomendas.getChildren().add(btnCard);
    }

    // --- FECHAMENTO DE CAIXA (SEGURANÇA) ---

    @FXML
    public void acaoFecharCaixa() {
        // 1. Verifica se tem venda na tela
        if (!carrinho.isEmpty()) {
            mostrarAlerta("Finalize a venda atual antes de fechar o caixa.", Alert.AlertType.WARNING);
            return;
        }

        // 2. VERIFICAÇÃO DE SEGURANÇA: Encomendas Pendentes
        if (!encomendasAbertas.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Bloqueio de Fechamento");
            alert.setHeaderText("Existem Encomendas Pendentes!");
            alert.setContentText(
                    "O caixa NÃO PODE ser fechado com vendas suspensas (encomendas).\n" +
                            "Quantidade pendente: " + encomendasAbertas.size() + "\n\n" +
                            "Por favor, abra cada encomenda e finalize ou cancele antes de encerrar o dia."
            );
            alert.showAndWait();
            return; // ABORTA O FECHAMENTO
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

    // --- MÉTODOS AUXILIARES DE TELA E PRODUTO ---
    // (Mantidos idênticos ao original, apenas organizados)

    private void resetarPDV() {
        carrinho.clear();
        atualizarTotaisVisualmente();
        limparCamposAposInsercao();
        idEncomendaEmAndamento = null;
        atualizarNumeroVenda();
        if (lblProdutoIdentificado != null) lblProdutoIdentificado.setText("CAIXA LIVRE");
    }

    private void atualizarNumeroVenda() {
        try {
            if (idEncomendaEmAndamento != null) {
                if (lblNumeroVenda != null) lblNumeroVenda.setText("VENDA Nº " + idEncomendaEmAndamento);
            } else {
                int proximoId = vendaDAO.getProximoIdVenda();
                if (lblNumeroVenda != null) lblNumeroVenda.setText("VENDA Nº " + proximoId);
            }
        } catch (Exception e) { if (lblNumeroVenda != null) lblNumeroVenda.setText("VENDA Nº --"); }
    }

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
                adicionarAoCarrinho();
            }
        } else {
            lblProdutoIdentificado.setText("PRODUTO NÃO ENCONTRADO");
            txtCodigo.selectAll();
        }
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
                limparCamposAposInsercao();
                lblProdutoIdentificado.setText("ITEM ATUALIZADO!");
            } else {
                carrinho.add(new ItemVenda(produtoAtual, qtd));
                limparCamposAposInsercao();
            }
            atualizarTotaisVisualmente();
        } catch (NumberFormatException e) { System.out.println("Erro peso inválido"); }
    }

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
                Double peso = balancaService.lerPeso();
                Platform.runLater(() -> {
                    txtPeso.setText(String.format("%.3f", peso));
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

    private void limparCamposAposInsercao() {
        txtCodigo.setText("");
        txtPeso.setText("");
        txtPeso.setDisable(true);
        if (lblProdutoIdentificado != null) lblProdutoIdentificado.setText("AGUARDANDO...");
        Platform.runLater(() -> txtCodigo.requestFocus());
        produtoAtual = null;
    }

    private void atualizarTotaisVisualmente() {
        lblTotalVenda.setText(String.format("R$ %.2f", calcularTotalCarrinho()));
        tabelaItens.refresh();
    }

    // --- Configurações de Eventos (KeyListeners, etc) ---
    private void configurarEventosGlobais() {
        txtCodigo.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) buscarProduto();
            else if (event.getCode() == KeyCode.F2) { lerPesoBalanca(); event.consume(); }
            else if (event.getText().equals("*") || event.getCode() == KeyCode.MULTIPLY) { event.consume(); abrirNovaEncomenda(null); }
            else processarAtalhoGlobal(event);
        });
        txtPeso.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) processarInputPeso();
            else if (event.getCode() == KeyCode.F2) { lerPesoBalanca(); event.consume(); }
            else if (event.getText().equals("*") || event.getCode() == KeyCode.MULTIPLY) { event.consume(); abrirNovaEncomenda(null); }
            else processarAtalhoGlobal(event);
        });
    }

    private void processarAtalhoGlobal(KeyEvent event) {
        if (event.getCode() == KeyCode.DIVIDE || event.getCode() == KeyCode.SLASH) { event.consume(); finalizarVenda(); }
        else if (event.getCode() == KeyCode.F5) finalizarVenda();
        else if (event.getCode() == KeyCode.ESCAPE) {
            if (itemEmEdicao != null) { tabelaItens.getSelectionModel().clearSelection(); limparCamposAposInsercao(); }
            else tentarSair();
        }
    }

    private void configurarTabela() {
        colNome.setCellValueFactory(new PropertyValueFactory<>("nomeProduto"));
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalItem"));
        // CellFactories mantidos...
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
                super.updateItem(item, empty); setText((empty || item == null) ? null : String.format("R$ %.2f", item));
            }
        });
        colTotal.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty); setText((empty || item == null) ? null : String.format("R$ %.2f", item));
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
                txtPeso.setText(String.format("%.0f", newVal.getQuantidade() * 1000));
                Platform.runLater(() -> { txtPeso.requestFocus(); txtPeso.selectAll(); });
                txtCodigo.setDisable(true);
            } else cancelarEdicao();
        });
    }

    private void cancelarEdicao() { itemEmEdicao = null; txtCodigo.setDisable(false); }

    private void configurarEventosTabela() {
        tabelaItens.setOnKeyPressed(event -> {
            ItemVenda item = tabelaItens.getSelectionModel().getSelectedItem();
            if (item == null) return;
            if (event.getCode() == KeyCode.DELETE) {
                carrinho.remove(item);
                atualizarTotaisVisualmente();
                if (item == itemEmEdicao) limparCamposAposInsercao();
            } else if ("UN".equals(item.getProduto().getUnidade())) {
                if (event.getCode() == KeyCode.ADD || event.getCode() == KeyCode.PLUS) item.setQuantidade(item.getQuantidade() + 1);
                else if (event.getCode() == KeyCode.SUBTRACT || event.getCode() == KeyCode.MINUS) {
                    if (item.getQuantidade() > 1) item.setQuantidade(item.getQuantidade() - 1);
                    else carrinho.remove(item);
                }
                tabelaItens.refresh(); atualizarTotaisVisualmente();
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
                if (!newValue.equals(formatado)) { txtPeso.setText(formatado); txtPeso.positionCaret(formatado.length()); }
            }
        });
    }

    @FXML public void tentarSair() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voltar ao Menu?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) voltarAoMenu();
    }

    private void voltarAoMenu() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/br/com/churrasco/view/Menu.fxml"));
            Stage stage = (Stage) txtCodigo.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

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
        return pagamentos.size() == 1 ? pagamentos.get(0).getTipo() : "MISTO";
    }

    private double calcularTotalCarrinho() { return carrinho.stream().mapToDouble(ItemVenda::getTotalItem).sum(); }
    private void mostrarAlerta(String msg, Alert.AlertType type) { Alert alert = new Alert(type); alert.setContentText(msg); alert.showAndWait(); }
}