package br.com.churrasco.controller;

import br.com.churrasco.dao.ProdutoDAO;
import br.com.churrasco.dao.PromocaoDAO;
import br.com.churrasco.model.Produto;
import br.com.churrasco.model.Promocao;
import br.com.churrasco.model.PromocaoItem;
import br.com.churrasco.util.Navegacao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PromocoesController {

    @FXML private TextField txtNomePromocao;
    @FXML private TextField txtPrecoCombo;
    @FXML private DatePicker dtInicio;
    @FXML private DatePicker dtFim;
    @FXML private CheckBox chkAtiva;
    @FXML private TextField txtBuscaProdutos;
    @FXML private TextField txtQuantidadeItem;
    @FXML private Label lblResumoCombo;

    @FXML private TableView<Promocao> tabelaPromocoes;
    @FXML private TableColumn<Promocao, String> colPromocaoNome;
    @FXML private TableColumn<Promocao, Double> colPromocaoPreco;
    @FXML private TableColumn<Promocao, LocalDate> colPromocaoInicio;
    @FXML private TableColumn<Promocao, LocalDate> colPromocaoFim;
    @FXML private TableColumn<Promocao, Boolean> colPromocaoStatus;

    @FXML private TableView<Produto> tabelaProdutos;
    @FXML private TableColumn<Produto, String> colProdutoCodigo;
    @FXML private TableColumn<Produto, String> colProdutoNome;
    @FXML private TableColumn<Produto, String> colProdutoUnidade;
    @FXML private TableColumn<Produto, Double> colProdutoPreco;

    @FXML private TableView<PromocaoItem> tabelaItensCombo;
    @FXML private TableColumn<PromocaoItem, String> colItemCodigo;
    @FXML private TableColumn<PromocaoItem, String> colItemNome;
    @FXML private TableColumn<PromocaoItem, Integer> colItemQuantidade;
    @FXML private TableColumn<PromocaoItem, Double> colItemPrecoBase;

    private final PromocaoDAO promocaoDAO = new PromocaoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    private final ObservableList<Promocao> promocoes = FXCollections.observableArrayList();
    private final ObservableList<Produto> produtos = FXCollections.observableArrayList();
    private final ObservableList<Produto> produtosFiltrados = FXCollections.observableArrayList();
    private final ObservableList<PromocaoItem> itensCombo = FXCollections.observableArrayList();

    private Promocao promocaoEmEdicao;

    @FXML
    public void initialize() {
        configurarTabelas();
        carregarProdutos();
        carregarPromocoes();
        configurarFiltros();
        limparFormulario();
    }

    private void configurarTabelas() {
        colPromocaoNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colPromocaoPreco.setCellValueFactory(new PropertyValueFactory<>("precoCombo"));
        colPromocaoInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));
        colPromocaoFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));
        colPromocaoStatus.setCellValueFactory(new PropertyValueFactory<>("ativo"));
        colPromocaoPreco.setCellFactory(tc -> moedaCell());
        colPromocaoStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item ? "Ativa" : "Inativa"));
            }
        });
        tabelaPromocoes.setItems(promocoes);
        tabelaPromocoes.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                preencherFormulario(newValue);
            }
        });

        colProdutoCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colProdutoNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colProdutoUnidade.setCellValueFactory(new PropertyValueFactory<>("unidade"));
        colProdutoPreco.setCellValueFactory(new PropertyValueFactory<>("precoVenda"));
        colProdutoPreco.setCellFactory(tc -> moedaCell());
        tabelaProdutos.setItems(produtosFiltrados);

        colItemCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoProduto"));
        colItemNome.setCellValueFactory(new PropertyValueFactory<>("nomeProduto"));
        colItemQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colItemPrecoBase.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createObjectBinding(() -> {
            Produto produto = cell.getValue().getProduto();
            return produto != null ? produto.getPrecoVenda() : 0.0;
        }));
        colItemPrecoBase.setCellFactory(tc -> moedaCell());
        tabelaItensCombo.setItems(itensCombo);
    }

    private <S> TableCell<S, Double> moedaCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("R$ %.2f", item));
            }
        };
    }

    private void configurarFiltros() {
        txtBuscaProdutos.textProperty().addListener((obs, oldValue, newValue) -> filtrarProdutos());
    }

    private void carregarProdutos() {
        produtos.setAll(produtoDAO.listarTodos().stream()
                .filter(produto -> "UN".equalsIgnoreCase(produto.getUnidade()))
                .sorted(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList());
        produtosFiltrados.setAll(produtos);
    }

    private void filtrarProdutos() {
        String termo = txtBuscaProdutos.getText() != null ? txtBuscaProdutos.getText().trim().toLowerCase() : "";
        if (termo.isEmpty()) {
            produtosFiltrados.setAll(produtos);
            return;
        }

        produtosFiltrados.setAll(produtos.stream()
                .filter(produto -> produto.getNome().toLowerCase().contains(termo) || produto.getCodigo().toLowerCase().contains(termo))
                .toList());
    }

    private void carregarPromocoes() {
        promocoes.setAll(promocaoDAO.listarTodas());
    }

    @FXML
    public void adicionarProdutoAoCombo() {
        Produto produto = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (produto == null) {
            mostrarAlerta("Selecione um produto UN para adicionar ao combo.");
            return;
        }

        int quantidade;
        try {
            quantidade = Integer.parseInt(txtQuantidadeItem.getText());
        } catch (NumberFormatException e) {
            mostrarAlerta("Informe uma quantidade inteira válida.");
            return;
        }

        if (quantidade <= 0) {
            mostrarAlerta("A quantidade do item do combo deve ser maior que zero.");
            return;
        }

        PromocaoItem existente = itensCombo.stream()
                .filter(item -> item.getProduto() != null && item.getProduto().getId().equals(produto.getId()))
                .findFirst()
                .orElse(null);

        if (existente != null) {
            existente.setQuantidade(existente.getQuantidade() + quantidade);
            tabelaItensCombo.refresh();
        } else {
            itensCombo.add(new PromocaoItem(null, promocaoEmEdicao != null ? promocaoEmEdicao.getId() : null, produto, quantidade));
        }

        atualizarResumoCombo();
        txtQuantidadeItem.setText("1");
    }

    @FXML
    public void removerItemDoCombo() {
        PromocaoItem item = tabelaItensCombo.getSelectionModel().getSelectedItem();
        if (item == null) {
            mostrarAlerta("Selecione um item do combo para remover.");
            return;
        }

        itensCombo.remove(item);
        atualizarResumoCombo();
    }

    @FXML
    public void salvarPromocao() {
        String nome = txtNomePromocao.getText() != null ? txtNomePromocao.getText().trim() : "";
        if (nome.isEmpty()) {
            mostrarAlerta("Informe o nome da promoção.");
            return;
        }

        if (itensCombo.isEmpty()) {
            mostrarAlerta("Adicione pelo menos um item ao combo.");
            return;
        }

        double precoCombo;
        try {
            precoCombo = Double.parseDouble(txtPrecoCombo.getText().replace(',', '.'));
        } catch (NumberFormatException e) {
            mostrarAlerta("Informe um preço de combo válido.");
            return;
        }

        if (precoCombo <= 0) {
            mostrarAlerta("O preço do combo deve ser maior que zero.");
            return;
        }

        Promocao promocao = promocaoEmEdicao != null ? promocaoEmEdicao : new Promocao();
        promocao.setNome(nome);
        promocao.setPrecoCombo(precoCombo);
        promocao.setDataInicio(dtInicio.getValue());
        promocao.setDataFim(dtFim.getValue());
        promocao.setAtivo(chkAtiva.isSelected());
        promocao.setItens(new ArrayList<>(itensCombo));

        try {
            if (promocao.getId() == null) {
                promocaoDAO.salvar(promocao);
            } else {
                promocaoDAO.atualizar(promocao);
            }
            carregarPromocoes();
            limparFormulario();
            mostrarInfo("Promoção salva com sucesso.");
        } catch (Exception e) {
            mostrarAlerta("Erro ao salvar promoção: " + e.getMessage());
        }
    }

    @FXML
    public void excluirPromocao() {
        Promocao promocao = tabelaPromocoes.getSelectionModel().getSelectedItem();
        if (promocao == null) {
            mostrarAlerta("Selecione uma promoção para excluir.");
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Deseja excluir a promoção '" + promocao.getNome() + "'?", ButtonType.YES, ButtonType.NO);
        if (confirmacao.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            promocaoDAO.excluir(promocao.getId());
            carregarPromocoes();
            limparFormulario();
        } catch (Exception e) {
            mostrarAlerta("Erro ao excluir promoção: " + e.getMessage());
        }
    }

    @FXML
    public void limparFormulario() {
        promocaoEmEdicao = null;
        txtNomePromocao.clear();
        txtPrecoCombo.clear();
        txtBuscaProdutos.clear();
        txtQuantidadeItem.setText("1");
        dtInicio.setValue(LocalDate.now());
        dtFim.setValue(LocalDate.now().plusDays(7));
        chkAtiva.setSelected(true);
        itensCombo.clear();
        tabelaPromocoes.getSelectionModel().clearSelection();
        tabelaProdutos.getSelectionModel().clearSelection();
        tabelaItensCombo.getSelectionModel().clearSelection();
        atualizarResumoCombo();
        filtrarProdutos();
    }

    private void preencherFormulario(Promocao promocao) {
        promocaoEmEdicao = promocao;
        txtNomePromocao.setText(promocao.getNome());
        txtPrecoCombo.setText(String.format("%.2f", promocao.getPrecoCombo()).replace('.', ','));
        dtInicio.setValue(promocao.getDataInicio());
        dtFim.setValue(promocao.getDataFim());
        chkAtiva.setSelected(promocao.isAtivo());
        itensCombo.setAll(promocao.getItens());
        atualizarResumoCombo();
    }

    private void atualizarResumoCombo() {
        double totalBase = itensCombo.stream()
                .mapToDouble(item -> item.getProduto() != null && item.getProduto().getPrecoVenda() != null
                        ? item.getProduto().getPrecoVenda() * item.getQuantidade()
                        : 0.0)
                .sum();

        String resumo = itensCombo.isEmpty()
                ? "Nenhum item no combo"
                : itensCombo.stream()
                    .map(item -> item.getQuantidade() + "x " + item.getNomeProduto())
                    .reduce((a, b) -> a + " + " + b)
                    .orElse("Nenhum item no combo");

        lblResumoCombo.setText(resumo + "\nPreço base: " + String.format("R$ %.2f", totalBase));
    }

    @FXML
    public void voltarMenu(ActionEvent event) {
        try {
            Navegacao.trocarTela(event, "/br/com/churrasco/view/Menu.fxml", "Tiãozinho's Grill - Menu");
        } catch (Exception e) {
            mostrarAlerta("Erro ao voltar ao menu: " + e.getMessage());
        }
    }

    private void mostrarAlerta(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
