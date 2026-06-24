package br.com.churrasco.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import br.com.churrasco.dao.CaixaDAO;
import br.com.churrasco.dao.ProdutoDAO;
import br.com.churrasco.dao.UsuarioDAO;
import br.com.churrasco.dao.VendaDAO;
import br.com.churrasco.model.*;
import br.com.churrasco.service.BalancaService;
import br.com.churrasco.service.ImpressoraService;
import br.com.churrasco.service.PromocaoService;
import br.com.churrasco.util.CupomGenerator;
import br.com.churrasco.util.Sessao;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair; // Importante para o novo Dialog
import javafx.util.Duration;
import javafx.scene.Node;

import java.io.IOException;
import java.util.Locale;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PDVController {

    @FXML private BorderPane rootPane;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtPeso;
    @FXML private Label lblStatusBalanca;
    @FXML private Label lblPreviaValorBalanca;
    @FXML private Label lblTotalVenda;
    @FXML private Label lblResumoPromocoes;
    @FXML private Label lblProdutoIdentificado;
    @FXML private Label lblNumeroVenda;
    @FXML private Label lblResumoEstoquePdv;
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
    private final CaixaDAO caixaDAO = new CaixaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO(); // Instanciado aqui para uso geral
    private final PromocaoService promocaoService = new PromocaoService();

    private Produto produtoAtual = null;
    private ItemVenda itemEmEdicao = null;
    private final ObservableList<ItemVenda> carrinho = FXCollections.observableArrayList();
    private final List<Encomenda> encomendasAbertas = new ArrayList<>();
    private Integer idEncomendaEmAndamento = null;
    private volatile boolean lendoBalanca = false;
    private final List<Button> cardsEncomenda = new ArrayList<>();
    private final Map<Button, Encomenda> encomendasPorCard = new LinkedHashMap<>();
    private int indiceCardEncomendaFocado = -1;
    private Timeline timelineEncomendas;
    private ResultadoPromocao resultadoPromocaoAtual = new ResultadoPromocao();

    private static final String ESTILO_CARD_ENCOMENDA = "-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 170; -fx-min-height: 68; -fx-background-radius: 5;";
    private static final String ESTILO_CARD_ENCOMENDA_FOCADO = "-fx-background-color: #d35400; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 170; -fx-min-height: 68; -fx-background-radius: 5; -fx-border-color: #f1c40f; -fx-border-width: 3; -fx-border-radius: 5;";
    private static final String ESTILO_CARD_ENCOMENDA_ATRASADO = "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 170; -fx-min-height: 68; -fx-background-radius: 5;";
    private static final String ESTILO_CARD_ENCOMENDA_ATRASADO_FOCADO = "-fx-background-color: #a93226; -fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 170; -fx-min-height: 68; -fx-background-radius: 5; -fx-border-color: #f1c40f; -fx-border-width: 3; -fx-border-radius: 5;";

    // LIMITE PARA AVISAR QUE ESTÁ ACABANDO (Ex: 5kg ou 5 unidades)
    private static final double LIMITE_ESTOQUE_BAIXO = 5.0;

    @FXML
    public void initialize() {
        if (!verificarEForcarAberturaCaixa()) return;

        configurarTabela();
        configurarEventosGlobais();
        configurarEventosTabela();
        configurarMascaraPeso();
        configurarSelecaoTabela();
        configurarLeituraAutomaticaBalanca();
        iniciarAtualizacaoDosCardsEncomenda();
        resetarPDV();
        recuperarEncomendasPendentes();
        atualizarResumoEstoquePdv();

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getWindow() != null) {
                newScene.getWindow().setOnHidden(event -> pararAtualizacaoDosCardsEncomenda());
            }
        });

        if (Sessao.getUsuario() != null) {
            Platform.runLater(() -> {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setTitle("PDV - Operador: " + Sessao.getUsuario().getNome());
            });
        }
    }

    private boolean verificarEForcarAberturaCaixa() {
        Caixa caixaAberto = caixaDAO.buscarCaixaAberto();
        if (caixaAberto == null) {
            TextInputDialog dialog = new TextInputDialog("0.00");
            dialog.setTitle("Abertura de Caixa");
            dialog.setHeaderText("Nenhum caixa aberto encontrado.");
            dialog.setContentText("Informe o Fundo de Troco (R$) para iniciar:");
            dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(true);

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    String valorStr = result.get().replace(",", ".");
                    caixaDAO.abrirCaixa(Double.parseDouble(valorStr));
                    new Alert(Alert.AlertType.INFORMATION, "Caixa aberto com sucesso!", ButtonType.OK).showAndWait();
                    return true;
                } catch (Exception e) {
                    mostrarAlerta("Valor inválido! O caixa não foi aberto.", Alert.AlertType.ERROR);
                    voltarAoMenu();
                    return false;
                }
            } else {
                voltarAoMenu();
                return false;
            }
        }
        return true;
    }

    // --- LÓGICA DE VALIDAÇÃO DE ESTOQUE ---
    private boolean validarEstoque(Produto p, double qtdSolicitada) {
        double qtdNoCarrinho = carrinho.stream()
                .filter(item -> item.getProduto().getId().equals(p.getId()))
                .mapToDouble(ItemVenda::getQuantidade)
                .sum();

        if (itemEmEdicao != null && itemEmEdicao.getProduto().getId().equals(p.getId())) {
            qtdNoCarrinho -= itemEmEdicao.getQuantidade();
        }

        double totalRequerido = qtdNoCarrinho + qtdSolicitada;

        if (totalRequerido > p.getEstoque()) {
            double disponivel = p.getEstoque() - qtdNoCarrinho;
            if (disponivel < 0) disponivel = 0;

            mostrarAlerta(String.format("ESTOQUE INSUFICIENTE!\nEstoque Total: %.3f\nNo Carrinho: %.3f\nDisponível: %.3f",
                    p.getEstoque(), qtdNoCarrinho, disponivel), Alert.AlertType.ERROR);
            return false;
        }

        return true;
    }

    // --- BUSCA DO PRODUTO ---
    private void buscarProduto() {
        String codigo = txtCodigo.getText();
        if (codigo.isEmpty()) return;

        Produto p = produtoDAO.buscarPorCodigo(codigo);

        if (p != null) {
            produtoAtual = p;

            String infoEstoque = String.format("Estoque: %.3f %s", p.getEstoque(), p.getUnidade());
            lblProdutoIdentificado.setText(p.getNome() + " | " + infoEstoque);

            if (p.getEstoque() <= 0) {
                lblProdutoIdentificado.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else if (p.getEstoque() <= LIMITE_ESTOQUE_BAIXO) {
                lblProdutoIdentificado.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 14px;");
            } else {
                lblProdutoIdentificado.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 14px;");
            }

            if ("KG".equals(p.getUnidade())) {
                txtPeso.setDisable(false);
                txtPeso.setText("");
                txtPeso.requestFocus();
            } else {
                txtPeso.setText("1");
                adicionarAoCarrinho();
            }
        } else {
            lblProdutoIdentificado.setText("NÃO ENCONTRADO");
            lblProdutoIdentificado.setStyle("-fx-text-fill: red;");
            txtCodigo.selectAll();
        }
    }

    // --- ADICIONAR ---
    private void adicionarAoCarrinho() {
        try {
            if (produtoAtual == null) {
                mostrarAlerta("Selecione um produto antes de adicionar.", Alert.AlertType.WARNING);
                return;
            }
            double qtd = Double.parseDouble(txtPeso.getText().replace(",", "."));
            if (!validarEstoque(produtoAtual, qtd)) {
                txtCodigo.selectAll();
                return;
            }
            carrinho.add(new ItemVenda(produtoAtual, qtd));
            atualizarTotaisVisualmente();
            limparCamposAposInsercao();
        } catch (NumberFormatException e) {
            mostrarAlerta("Quantidade/peso invalido.", Alert.AlertType.WARNING);
        } catch (Exception e) {
            mostrarAlerta("Nao foi possivel adicionar o item: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void processarInputPeso() {
        try {
            if (produtoAtual == null) {
                mostrarAlerta("Selecione um produto antes de informar o peso.", Alert.AlertType.WARNING);
                return;
            }
            String textoPeso = txtPeso.getText().replace(",", ".");
            if (textoPeso.isEmpty()) return;
            double qtd = Double.parseDouble(textoPeso);
            if (qtd <= 0) return;

            if (!validarEstoque(produtoAtual, qtd)) {
                return;
            }

            if (itemEmEdicao != null) {
                itemEmEdicao.setQuantidade(qtd);
                cancelarEdicao();
            } else {
                carrinho.add(new ItemVenda(produtoAtual, qtd));
            }
            atualizarTotaisVisualmente();
            limparCamposAposInsercao();
        } catch (NumberFormatException e) {
            mostrarAlerta("Peso invalido. Informe um valor numerico.", Alert.AlertType.WARNING);
        } catch (Exception e) {
            mostrarAlerta("Nao foi possivel atualizar o item: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void configurarLeituraAutomaticaBalanca() {
        txtPeso.focusedProperty().addListener((obs, foiFocado, estaFocado) -> {
            if (estaFocado && produtoAtual != null && "KG".equals(produtoAtual.getUnidade())) {
                iniciarLoopBalanca();
            } else {
                pararLoopBalanca();
            }
        });
    }

    private void iniciarLoopBalanca() {
        if (lendoBalanca) return;
        lendoBalanca = true;
        Platform.runLater(() -> {
            if (lblStatusBalanca != null) {
                mostrarAguardandoBalanca();
            }
        });
        Thread threadBalanca = new Thread(() -> {
            while (lendoBalanca) {
                try {
                    Double pesoLido = balancaService.lerPeso();
                    if (pesoLido != null && pesoLido > 0) {
                        Platform.runLater(() -> {
                            if (txtPeso.isFocused()) {
                                txtPeso.setText(String.format("%.3f", pesoLido));
                                atualizarStatusBalancaComPrevia(pesoLido);
                            }
                        });
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    lendoBalanca = false;
                }
            }
        });
        threadBalanca.setDaemon(true);
        threadBalanca.start();
    }

    private void pararLoopBalanca() {
        lendoBalanca = false;
        Platform.runLater(() -> {
            if (lblStatusBalanca != null) lblStatusBalanca.setText("");
            if (lblPreviaValorBalanca != null) lblPreviaValorBalanca.setText("");
        });
    }

    private void mostrarAguardandoBalanca() {
        if (lblStatusBalanca == null || lblPreviaValorBalanca == null) return;
        lblStatusBalanca.setText("AGUARDANDO BALANÇA...");
        lblStatusBalanca.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 12px;");
        lblPreviaValorBalanca.setText("");
    }

    private void atualizarStatusBalancaComPrevia(Double peso) {
        if (lblStatusBalanca == null || lblPreviaValorBalanca == null) return;
        if (peso == null || peso <= 0 || produtoAtual == null || !"KG".equals(produtoAtual.getUnidade())) {
            mostrarAguardandoBalanca();
            return;
        }

        double precoUnitario = produtoAtual.getPrecoVenda() != null ? produtoAtual.getPrecoVenda() : 0.0;
        double valorPrevio = peso * precoUnitario;
        lblStatusBalanca.setText(String.format("PESO: %.3f kg", peso));
        lblStatusBalanca.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");
        lblPreviaValorBalanca.setText(String.format("PRÉVIA: R$ %.2f", valorPrevio));
        lblPreviaValorBalanca.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 24px;");
    }

    private void mostrarEdicaoItemKg() {
        if (lblStatusBalanca != null) {
            lblStatusBalanca.setText("EDITANDO ITEM EM KG");
            lblStatusBalanca.setStyle("-fx-text-fill: #8e44ad; -fx-font-weight: bold; -fx-font-size: 12px;");
        }

        if (lblPreviaValorBalanca != null && produtoAtual != null) {
            lblPreviaValorBalanca.setText(capitalizarPrimeiraLetra(produtoAtual.getNome()));
            lblPreviaValorBalanca.setStyle("-fx-text-fill: #8e44ad; -fx-font-weight: bold; -fx-font-size: 18px;");
        }
    }

    private void limparCamposAposInsercao() {
        pararLoopBalanca();
        txtPeso.setText("");
        txtPeso.setDisable(true);
        txtCodigo.setText("");
        if (lblProdutoIdentificado != null) {
            lblProdutoIdentificado.setText("AGUARDANDO...");
            lblProdutoIdentificado.setStyle("-fx-text-fill: black;");
        }
        if (lblStatusBalanca != null) lblStatusBalanca.setText("");
        if (lblPreviaValorBalanca != null) lblPreviaValorBalanca.setText("");
        produtoAtual = null;
        itemEmEdicao = null;
        tabelaItens.getSelectionModel().clearSelection();
        Platform.runLater(() -> {
            txtCodigo.requestFocus();
            txtCodigo.selectAll();
        });
    }

    @FXML
    public void finalizarVenda() {
        if (carrinho.isEmpty()) {
            mostrarAlerta("Carrinho vazio!", Alert.AlertType.WARNING);
            return;
        }
        try {
            PagamentoController pagController = abrirModalPagamento();
            if (!pagController.isConfirmado()) return;

            List<Pagamento> pagamentos = pagController.getPagamentosRealizados();
            double descontoManual = pagController.getValorDesconto();
            double descontoPromocional = pagController.getValorDescontoPromocional();
            double totalCarrinho = calcularSubtotalCarrinho();
            double totalLiquido = totalCarrinho - descontoPromocional - descontoManual;

            ConfirmacaoController confController = abrirModalConfirmacao(pagamentos, totalLiquido, descontoManual, descontoPromocional);
            if (!confController.isConfirmado()) return;

            salvarVendaNoBanco(totalLiquido, pagamentos, descontoManual, descontoPromocional, new ArrayList<>(resultadoPromocaoAtual.getPromocoesAplicadas()));

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Erro Crítico: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void salvarVendaNoBanco(double totalLiquido, List<Pagamento> pagamentos, double descontoManual, double descontoPromocional, List<PromocaoAplicada> promocoesAplicadas) {
        try {
            Venda venda = new Venda();
            venda.setDataHora(LocalDateTime.now());
            venda.setValorTotal(totalLiquido);
            venda.setDescontoManual(descontoManual);
            venda.setDescontoPromocional(descontoPromocional);
            venda.setDesconto(descontoManual + descontoPromocional);
            venda.setFormaPagamento(determinarTipoPagamento(pagamentos));
            if (idEncomendaEmAndamento != null) {
                venda.setId(idEncomendaEmAndamento);
            }
            int idVendaSalva = vendaDAO.salvarVenda(venda, new ArrayList<>(carrinho), pagamentos, promocoesAplicadas);
            venda.setId(idVendaSalva);
            List<ItemVenda> itensCopia = new ArrayList<>(carrinho);
            List<Pagamento> pagsCopia = new ArrayList<>(pagamentos);
            resetarPDV();
            atualizarResumoEstoquePdv();
            perguntarImpressaoComprovante(venda, itensCopia, pagsCopia);
        } catch (Exception e) {
            mostrarAlerta("ERRO AO SALVAR: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void perguntarImpressaoComprovante(Venda venda, List<ItemVenda> itens, List<Pagamento> pagamentos) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/ImpressaoComprovante.fxml"));
            Parent root = loader.load();
            ImpressaoComprovanteController controller = loader.getController();
            controller.setNumeroVenda(venda.getId());

            Stage stage = criarModalVenda("Impressao do Comprovante", root);
            stage.showAndWait();

            if (controller.isImprimir()) {
                new Thread(() -> impressoraService.imprimirCupom(venda, itens, pagamentos)).start();
            }
        } catch (IOException e) {
            mostrarAlerta("Nao foi possivel abrir a janela de impressao: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

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
                    vendaDAO.salvarEncomenda(nova);
                    encomendasAbertas.add(nova);
                    criarCardEncomenda(nova);
                    atualizarResumoEstoquePdv();
                    mostrarAlerta("Encomenda salva! Estoque reservado.", Alert.AlertType.INFORMATION);
                    resetarPDV();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Erro ao processar encomenda: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void recuperarEncomendasPendentes() {
        boxEncomendas.getChildren().clear();
        encomendasAbertas.clear();
        cardsEncomenda.clear();
        encomendasPorCard.clear();
        indiceCardEncomendaFocado = -1;
        Thread t = new Thread(() -> {
            try {
                List<Encomenda> pendentes = vendaDAO.buscarTodasEncomendasPendentes();
                Platform.runLater(() -> {
                    if (pendentes != null) {
                        for (Encomenda enc : pendentes) {
                            encomendasAbertas.add(enc);
                            criarCardEncomenda(enc);
                        }
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        });
        t.setDaemon(true); t.start();
    }

    private void criarCardEncomenda(Encomenda encomenda) {
        if (boxEncomendas == null) return;
        String idTexto = (encomenda.getId() != null) ? "#" + encomenda.getId() : "";
        Button btnCard = new Button();
        btnCard.setStyle(ESTILO_CARD_ENCOMENDA);
        btnCard.setFocusTraversable(true);
        HBox.setMargin(btnCard, new javafx.geometry.Insets(0, 5, 0, 5));
        btnCard.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                indiceCardEncomendaFocado = cardsEncomenda.indexOf(btnCard);
            }
            atualizarEstiloCardsEncomenda();
        });
        btnCard.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.LEFT) {
                event.consume();
                moverFocoEncomenda(-1);
            } else if (event.getCode() == KeyCode.RIGHT) {
                event.consume();
                moverFocoEncomenda(1);
            } else if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                sairModoEncomendas();
            }
        });
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
                    removerCardEncomenda(encomenda, btnCard);
                    mostrarAlerta("Encomenda aberta! Itens carregados.", Alert.AlertType.INFORMATION);
                    } else if (result.get() == btnCancelar) {
                        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Deseja cancelar esta encomenda? O estoque será devolvido.", ButtonType.YES, ButtonType.NO);
                        if (confirmacao.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                            vendaDAO.cancelarEncomenda(encomenda.getId());
                            removerCardEncomenda(encomenda, btnCard);
                            atualizarResumoEstoquePdv();
                            mostrarAlerta("Encomenda cancelada e estoque estornado.", Alert.AlertType.INFORMATION);
                        }
                    }
            }
        });
        cardsEncomenda.add(btnCard);
        encomendasPorCard.put(btnCard, encomenda);
        boxEncomendas.getChildren().add(btnCard);
        atualizarConteudoCardEncomenda(btnCard, encomenda);
        atualizarEstiloCardsEncomenda();
    }

    private void removerCardEncomenda(Encomenda encomenda, Button btnCard) {
        encomendasAbertas.remove(encomenda);
        boxEncomendas.getChildren().remove(btnCard);
        int indiceRemovido = cardsEncomenda.indexOf(btnCard);
        cardsEncomenda.remove(btnCard);
        encomendasPorCard.remove(btnCard);

        if (cardsEncomenda.isEmpty()) {
            indiceCardEncomendaFocado = -1;
            return;
        }

        if (indiceCardEncomendaFocado >= cardsEncomenda.size()) {
            indiceCardEncomendaFocado = cardsEncomenda.size() - 1;
        } else if (indiceRemovido >= 0 && indiceCardEncomendaFocado > indiceRemovido) {
            indiceCardEncomendaFocado--;
        }

        atualizarEstiloCardsEncomenda();
    }

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
            boolean autorizado = solicitarSenhaAdmin();
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

    // --- POPUP DE AUTORIZAÇÃO CORRIGIDO ---
    private boolean solicitarSenhaAdmin() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Autorização de Gerente/Dono");
        dialog.setHeaderText("Operação Bloqueada para Atendentes.\nUm Administrador deve autorizar.");

        ButtonType loginButtonType = new ButtonType("Autorizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> cmbAdmin = new ComboBox<>();
        cmbAdmin.setPromptText("Selecione o Admin");
        cmbAdmin.getItems().addAll(usuarioDAO.listarNomes()); // Carrega do Banco

        PasswordField pwdSenha = new PasswordField();
        pwdSenha.setPromptText("Senha");

        grid.add(new Label("Administrador:"), 0, 0);
        grid.add(cmbAdmin, 1, 0);
        grid.add(new Label("Senha:"), 0, 1);
        grid.add(pwdSenha, 1, 1);

        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        pwdSenha.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty() || cmbAdmin.getValue() == null);
        });

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(cmbAdmin::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(cmbAdmin.getValue(), pwdSenha.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if (result.isPresent()) {
            String adminNome = result.get().getKey();
            String adminSenha = result.get().getValue();

            // CHAMA A VALIDAÇÃO DO DAO (Aquela que imprimia no console o erro)
            if (usuarioDAO.validarAdmin(adminNome, adminSenha)) {
                return true;
            } else {
                mostrarAlerta("Senha incorreta ou usuário não é Administrador!", Alert.AlertType.ERROR);
                return false;
            }
        }
        return false;
    }

    private void resetarPDV() { carrinho.clear(); resultadoPromocaoAtual = new ResultadoPromocao(); atualizarTotaisVisualmente(); limparCamposAposInsercao(); idEncomendaEmAndamento = null; atualizarNumeroVenda(); if (lblProdutoIdentificado != null) lblProdutoIdentificado.setText("CAIXA LIVRE"); sairModoEncomendas(); }
    private void atualizarNumeroVenda() { try { int id = (idEncomendaEmAndamento != null) ? idEncomendaEmAndamento : vendaDAO.getProximoIdVenda(); if (lblNumeroVenda != null) lblNumeroVenda.setText("VENDA Nº " + id); } catch (Exception e) {} }
    private void atualizarTotaisVisualmente() {
        resultadoPromocaoAtual = promocaoService.calcularPromocoes(carrinho);
        lblTotalVenda.setText(String.format("R$ %.2f", resultadoPromocaoAtual.getTotalComPromocao()));
        atualizarResumoPromocoes();
        tabelaItens.refresh();
    }
    private void atualizarResumoEstoquePdv() {
        if (lblResumoEstoquePdv == null) return;

        try {
            List<Produto> produtos = produtoDAO.listarTodos();
            List<String> itensResumo = new ArrayList<>();
            double totalProteinas = 0.0;
            boolean possuiProteinasAgrupadas = false;

            for (Produto produto : produtos) {
                if (!produto.isExibirNoPdv()) continue;

                if (produto.isAgruparEmProteina()) {
                    possuiProteinasAgrupadas = true;
                    totalProteinas += produto.getEstoque() != null ? produto.getEstoque() : 0.0;
                    continue;
                }

                itensResumo.add(formatarResumoProduto(produto));
            }

            if (possuiProteinasAgrupadas) {
                itensResumo.add("Proteinas: " + formatarQuantidadeResumo(totalProteinas, "KG"));
            }

            lblResumoEstoquePdv.setText(itensResumo.isEmpty()
                    ? "Nenhum produto marcado para exibir no PDV"
                    : String.join(" | ", itensResumo));
        } catch (Exception e) {
            lblResumoEstoquePdv.setText("Nao foi possivel carregar o resumo de estoque");
        }
    }

    private String formatarResumoProduto(Produto produto) {
        String nome = capitalizarPrimeiraLetra(produto.getNome());
        return nome + ": " + formatarQuantidadeResumo(produto.getEstoque(), produto.getUnidade());
    }

    private String formatarQuantidadeResumo(Double estoque, String unidade) {
        double valor = estoque != null ? estoque : 0.0;
        if ("KG".equalsIgnoreCase(unidade)) {
            return formatarDecimalSemZeros(valor) + "kg";
        }
        return formatarDecimalSemZeros(valor);
    }

    private String formatarDecimalSemZeros(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 0.0000001d) {
            return String.format(Locale.US, "%.0f", valor);
        }
        return String.format(Locale.US, "%.3f", valor)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private String capitalizarPrimeiraLetra(String texto) {
        if (texto == null || texto.isBlank()) return "Produto";
        return texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }
    private void cancelarEdicao() {
        itemEmEdicao = null;
        txtCodigo.setDisable(false);
    }
    private void configurarEventosGlobais() {
        txtCodigo.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) buscarProduto();
        });
        txtPeso.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                processarInputPeso();
            }
        });
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getText().equals("*") || event.getCode() == KeyCode.MULTIPLY) {
                event.consume();
                abrirNovaEncomenda(null);
            } else if (event.getCode() == KeyCode.SUBTRACT || event.getCode() == KeyCode.MINUS) {
                event.consume();
                entrarModoEncomendas();
            } else if (event.getCode() == KeyCode.DIVIDE || event.getCode() == KeyCode.SLASH || event.getCode() == KeyCode.F5) {
                event.consume();
                finalizarVenda();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                if (itemEmEdicao != null) limparCamposAposInsercao();
                else if (indiceCardEncomendaFocado >= 0) sairModoEncomendas();
                else tentarSair();
            }
        });
    }
    private void configurarMascaraPeso() {
        txtPeso.textProperty().addListener((obs, old, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                if (produtoAtual != null && "KG".equals(produtoAtual.getUnidade()) && txtPeso.isFocused()) {
                    mostrarAguardandoBalanca();
                }
                return;
            }

            String digitos = newValue.replaceAll("[^0-9]", "");
            if (digitos.isEmpty()) {
                mostrarAguardandoBalanca();
                return;
            }

            try {
                long valorBruto = Long.parseLong(digitos);
                double pesoDigitado = valorBruto / 1000.0;
                String formatado = String.format("%.3f", pesoDigitado);

                if (!newValue.equals(formatado)) {
                    Platform.runLater(() -> {
                        txtPeso.setText(formatado);
                        txtPeso.positionCaret(formatado.length());
                    });
                }

                atualizarStatusBalancaComPrevia(pesoDigitado);
            } catch (NumberFormatException e) {
                mostrarAguardandoBalanca();
            }
        });
    }
    private void configurarTabela() { colNome.setCellValueFactory(new PropertyValueFactory<>("nomeProduto")); colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade")); colPreco.setCellValueFactory(new PropertyValueFactory<>("precoUnitario")); colTotal.setCellValueFactory(new PropertyValueFactory<>("totalItem")); colQtd.setCellFactory(tc -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText((empty || item == null) ? null : String.format("%.3f", item)); } }); colPreco.setCellFactory(tc -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText((empty || item == null) ? null : String.format("R$ %.2f", item)); } }); colTotal.setCellFactory(tc -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); setText((empty || item == null) ? null : String.format("R$ %.2f", item)); } }); tabelaItens.setItems(carrinho); }
    private void configurarEventosTabela() {
        tabelaItens.setOnKeyPressed(event -> {
            ItemVenda item = tabelaItens.getSelectionModel().getSelectedItem();
            if (item != null && event.getCode() == KeyCode.DELETE) {
                carrinho.remove(item);
                atualizarTotaisVisualmente();
            } else if (event.getCode() == KeyCode.ENTER && itemEmEdicao != null) {
                event.consume();
                processarInputPeso();
            } else if (event.getCode() == KeyCode.ESCAPE && itemEmEdicao != null) {
                event.consume();
                limparCamposAposInsercao();
            }
        });
    }
    private void configurarSelecaoTabela() {
        tabelaItens.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && "KG".equals(newVal.getProduto().getUnidade())) {
                itemEmEdicao = newVal;
                produtoAtual = newVal.getProduto();
                txtPeso.setDisable(false);
                txtPeso.setText(String.format("%.3f", newVal.getQuantidade()));
                txtCodigo.setDisable(true);
                mostrarEdicaoItemKg();
                Platform.runLater(() -> {
                    txtPeso.requestFocus();
                    txtPeso.selectAll();
                });
            }
        });
    }
    private void entrarModoEncomendas() { if (cardsEncomenda.isEmpty()) { mostrarAlerta("Nao existem encomendas pendentes para abrir.", Alert.AlertType.INFORMATION); return; } if (indiceCardEncomendaFocado < 0 || indiceCardEncomendaFocado >= cardsEncomenda.size()) indiceCardEncomendaFocado = 0; focarCardEncomenda(indiceCardEncomendaFocado); }
    private void sairModoEncomendas() { indiceCardEncomendaFocado = -1; atualizarEstiloCardsEncomenda(); Platform.runLater(() -> { txtCodigo.requestFocus(); txtCodigo.selectAll(); }); }
    private void moverFocoEncomenda(int delta) { if (cardsEncomenda.isEmpty()) return; if (indiceCardEncomendaFocado < 0) indiceCardEncomendaFocado = 0; else indiceCardEncomendaFocado = Math.floorMod(indiceCardEncomendaFocado + delta, cardsEncomenda.size()); focarCardEncomenda(indiceCardEncomendaFocado); }
    private void focarCardEncomenda(int indice) { if (indice < 0 || indice >= cardsEncomenda.size()) return; indiceCardEncomendaFocado = indice; atualizarEstiloCardsEncomenda(); Button card = cardsEncomenda.get(indice); Platform.runLater(card::requestFocus); }
    private void atualizarEstiloCardsEncomenda() { for (int i = 0; i < cardsEncomenda.size(); i++) { Button card = cardsEncomenda.get(i); Encomenda encomenda = encomendasPorCard.get(card); boolean focado = i == indiceCardEncomendaFocado || card.isFocused(); boolean atrasado = encomenda != null && isEncomendaAtrasada(encomenda); if (atrasado) card.setStyle(focado ? ESTILO_CARD_ENCOMENDA_ATRASADO_FOCADO : ESTILO_CARD_ENCOMENDA_ATRASADO); else card.setStyle(focado ? ESTILO_CARD_ENCOMENDA_FOCADO : ESTILO_CARD_ENCOMENDA); } }
    private void iniciarAtualizacaoDosCardsEncomenda() { if (timelineEncomendas != null) timelineEncomendas.stop(); timelineEncomendas = new Timeline(new KeyFrame(Duration.seconds(1), event -> atualizarCardsEncomenda())); timelineEncomendas.setCycleCount(Timeline.INDEFINITE); timelineEncomendas.play(); }
    private void pararAtualizacaoDosCardsEncomenda() { if (timelineEncomendas != null) timelineEncomendas.stop(); }
    private void atualizarCardsEncomenda() { for (Button card : cardsEncomenda) { Encomenda encomenda = encomendasPorCard.get(card); if (encomenda != null) atualizarConteudoCardEncomenda(card, encomenda); } atualizarEstiloCardsEncomenda(); }
    private void atualizarConteudoCardEncomenda(Button card, Encomenda encomenda) {
        String idTexto = (encomenda.getId() != null) ? "#" + encomenda.getId() : "";
        String cliente = encomenda.getNomeCliente() != null ? encomenda.getNomeCliente().trim() : "Sem nome";
        String resumoItens = resumirItensEncomenda(encomenda);
        card.setText("ENC " + idTexto + " | " + cliente + "\n" + resumoItens);
    }

    private String resumirItensEncomenda(Encomenda encomenda) {
        if (encomenda == null || encomenda.getItens() == null || encomenda.getItens().isEmpty()) {
            return "Sem itens";
        }

        List<String> partes = new ArrayList<>();
        for (ItemVenda item : encomenda.getItens()) {
            partes.add(formatarItemResumoEncomenda(item));
        }

        String resumo = String.join(" / ", partes);
        int limite = 36;
        if (resumo.length() <= limite) {
            return resumo;
        }

        return resumo.substring(0, Math.max(0, limite - 3)).trim() + "...";
    }

    private String formatarItemResumoEncomenda(ItemVenda item) {
        if (item == null || item.getProduto() == null) {
            return "Item";
        }

        String nome = capitalizarPrimeiraLetra(item.getProduto().getNome());
        String unidade = item.getProduto().getUnidade();
        double quantidade = item.getQuantidade();

        if ("KG".equalsIgnoreCase(unidade)) {
            if (quantidade > 0 && quantidade < 1) {
                long gramas = Math.round(quantidade * 1000);
                return gramas + "g " + nome;
            }
            return formatarDecimalSemZeros(quantidade) + "kg " + nome;
        }

        return formatarDecimalSemZeros(quantidade) + " " + nome;
    }
    private boolean isEncomendaAtrasada(Encomenda encomenda) { return encomenda.getDataRetirada() != null && !encomenda.getDataRetirada().isAfter(LocalDateTime.now()); }
    private String formatarStatusRetirada(Encomenda encomenda) { if (encomenda.getDataRetirada() == null) return "sem horario"; long minutos = ChronoUnit.MINUTES.between(LocalDateTime.now(), encomenda.getDataRetirada()); if (minutos > 0) return "retira em " + minutos + " min"; return "atrasado ha " + Math.abs(minutos) + " min"; }
    @FXML public void tentarSair() { voltarAoMenu(); }
    private void voltarAoMenu() { try { ((Stage) rootPane.getScene().getWindow()).close(); FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Menu.fxml")); Parent root = loader.load(); Stage stage = new Stage(); stage.setScene(new Scene(root)); stage.setMaximized(true); stage.setTitle("Menu Principal"); stage.show(); } catch (Exception e) { e.printStackTrace(); } }
    private Stage criarModalVenda(String titulo, Parent root) {
        Stage stage = new Stage();
        stage.setTitle(titulo);
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        if (rootPane.getScene() != null) {
            stage.initOwner(rootPane.getScene().getWindow());
        }
        stage.setResizable(false);
        stage.centerOnScreen();
        return stage;
    }
    private void atualizarResumoPromocoes() {
        if (lblResumoPromocoes == null) {
            return;
        }

        if (resultadoPromocaoAtual == null || resultadoPromocaoAtual.getPromocoesAplicadas() == null || resultadoPromocaoAtual.getPromocoesAplicadas().isEmpty()) {
            lblResumoPromocoes.setText("Nenhuma promoção aplicada");
            return;
        }

        String resumo = resultadoPromocaoAtual.getPromocoesAplicadas().stream()
                .map(promocao -> promocao.getQuantidadeAplicada() + "x " + promocao.getNomePromocao() + " (-" + String.format("R$ %.2f", promocao.getDescontoAplicado()) + ")")
                .reduce((a, b) -> a + " | " + b)
                .orElse("Nenhuma promoção aplicada");

        lblResumoPromocoes.setText(resumo + "\nDesconto promocional: " + String.format("R$ %.2f", resultadoPromocaoAtual.getDescontoPromocional()));
    }
    private PagamentoController abrirModalPagamento() throws IOException { FXMLLoader l = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Pagamento.fxml")); Parent r = l.load(); PagamentoController c = l.getController(); c.setDadosTotais(calcularSubtotalCarrinho(), resultadoPromocaoAtual != null ? resultadoPromocaoAtual.getDescontoPromocional() : 0.0); Stage s = criarModalVenda("Pagamento", r); s.showAndWait(); return c; }
    private ConfirmacaoController abrirModalConfirmacao(List<Pagamento> pagamentos, double total, double descontoManual, double descontoPromocional) throws IOException { FXMLLoader l = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Confirmacao.fxml")); Parent r = l.load(); ConfirmacaoController c = l.getController(); Venda vendaPreview = new Venda(); vendaPreview.setId(idEncomendaEmAndamento != null ? idEncomendaEmAndamento : vendaDAO.getProximoIdVenda()); vendaPreview.setDataHora(LocalDateTime.now()); vendaPreview.setValorTotal(total); vendaPreview.setDescontoManual(descontoManual); vendaPreview.setDescontoPromocional(descontoPromocional); vendaPreview.setDesconto(descontoManual + descontoPromocional); c.setTextoCupom(CupomGenerator.gerarTexto(vendaPreview, carrinho, pagamentos)); Stage s = criarModalVenda("Confirmacao da Venda", r); s.showAndWait(); return c; }
    private String determinarTipoPagamento(List<Pagamento> pagamentos) { if (pagamentos == null || pagamentos.isEmpty()) return "DESCONHECIDO"; return pagamentos.size() == 1 ? pagamentos.get(0).getTipo() : "MISTO"; }
    private double calcularSubtotalCarrinho() { return carrinho.stream().mapToDouble(ItemVenda::getTotalItem).sum(); }
    private void mostrarAlerta(String msg, Alert.AlertType type) { Alert alert = new Alert(type); alert.setContentText(msg); alert.showAndWait(); }
}
