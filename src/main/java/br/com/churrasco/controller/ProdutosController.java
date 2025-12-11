package br.com.churrasco.controller;

import br.com.churrasco.dao.ProdutoDAO;
import br.com.churrasco.model.Produto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class ProdutosController {

    @FXML private TextField txtCodigo;
    @FXML private TextField txtNome;
    @FXML private TextField txtCusto;
    @FXML private TextField txtPreco;
    @FXML private TextField txtEstoque;
    @FXML private ComboBox<String> comboUnidade;

    @FXML private TableView<Produto> tabelaProdutos;
    @FXML private TableColumn<Produto, String> colCodigo;
    @FXML private TableColumn<Produto, String> colNome;
    @FXML private TableColumn<Produto, Double> colCusto; // <--- NOVA COLUNA
    @FXML private TableColumn<Produto, Double> colPreco;
    @FXML private TableColumn<Produto, String> colUnidade;
    @FXML private TableColumn<Produto, Double> colEstoque;

    private ProdutoDAO produtoDAO = new ProdutoDAO();
    private ObservableList<Produto> listaProdutos = FXCollections.observableArrayList();
    private Produto produtoSelecionado = null;

    @FXML
    public void initialize() {
        configurarTabela();
        carregarDados();
        sugerirCodigo(); // <--- JÁ PREENCHE O CÓDIGO AO ABRIR

        comboUnidade.getItems().addAll("KG", "UN");
        comboUnidade.getSelectionModel().selectFirst();

        tabelaProdutos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                preencherFormulario(newVal);
            }
        });
    }

    private void configurarTabela() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colUnidade.setCellValueFactory(new PropertyValueFactory<>("unidade"));

        // --- COLUNA CUSTO (FORMATADA) ---
        colCusto.setCellValueFactory(new PropertyValueFactory<>("precoCusto"));
        colCusto.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText("-");
                else setText(String.format("R$ %.2f", item));
            }
        });

        // --- COLUNA PREÇO VENDA (FORMATADA) ---
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoVenda"));
        colPreco.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText("-");
                else setText(String.format("R$ %.2f", item));
            }
        });

        // --- COLUNA ESTOQUE (3 CASAS DECIMAIS) ---
        colEstoque.setCellValueFactory(new PropertyValueFactory<>("estoque"));
        colEstoque.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%.3f", item)); // <--- MÁSCARA APLICADA
            }
        });

        tabelaProdutos.setItems(listaProdutos);
    }

    private void carregarDados() {
        listaProdutos.clear();
        listaProdutos.addAll(produtoDAO.listarTodos());
    }

    // Método auxiliar para preencher o código
    private void sugerirCodigo() {
        if (produtoSelecionado == null) {
            txtCodigo.setText(produtoDAO.buscarProximoCodigoDisponivel());
        }
    }

    @FXML
    public void salvarProduto() {
        try {
            if (txtCodigo.getText().isEmpty() || txtNome.getText().isEmpty() || txtPreco.getText().isEmpty()) {
                mostrarAlerta("Preencha os campos obrigatórios!");
                return;
            }

            String codigo = txtCodigo.getText();
            String nome = txtNome.getText();
            double precoVenda = Double.parseDouble(txtPreco.getText().replace(",", "."));
            double estoque = Double.parseDouble(txtEstoque.getText().replace(",", "."));

            double precoCusto = 0.0;
            if (txtCusto != null && !txtCusto.getText().isEmpty()) {
                precoCusto = Double.parseDouble(txtCusto.getText().replace(",", "."));
            }

            String unidade = comboUnidade.getValue();

            if (produtoSelecionado == null) {
                // Valida se o código já existe apenas se for novo
                if(produtoDAO.buscarPorCodigo(codigo) != null) {
                    mostrarAlerta("Este código já existe! O sistema sugeriu um novo.");
                    sugerirCodigo();
                    return;
                }

                Produto novo = new Produto(null, codigo, nome, precoCusto, precoVenda, unidade, estoque);
                produtoDAO.salvar(novo);
            } else {
                produtoSelecionado.setCodigo(codigo);
                produtoSelecionado.setNome(nome);
                produtoSelecionado.setPrecoCusto(precoCusto);
                produtoSelecionado.setPrecoVenda(precoVenda);
                produtoSelecionado.setUnidade(unidade);
                produtoSelecionado.setEstoque(estoque);
                produtoDAO.atualizar(produtoSelecionado);
            }

            limparCampos();
            carregarDados();

        } catch (NumberFormatException e) {
            mostrarAlerta("Preço ou Estoque inválidos! Use números.");
        } catch (Exception e) {
            mostrarAlerta("Erro ao salvar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void excluirProduto() {
        Produto p = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (p == null) {
            mostrarAlerta("Selecione um produto para excluir.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir");
        confirm.setContentText("Deseja excluir " + p.getNome() + "?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            produtoDAO.deletar(p.getId());
            limparCampos();
            carregarDados();
        }
    }

    @FXML
    public void limparCampos() {
        txtCodigo.setText("");
        txtNome.setText("");
        txtPreco.setText("");
        if(txtCusto != null) txtCusto.setText("");
        txtEstoque.setText("0");
        comboUnidade.getSelectionModel().selectFirst();
        produtoSelecionado = null;
        tabelaProdutos.getSelectionModel().clearSelection();

        sugerirCodigo(); // <--- Gera o próximo código ao limpar
    }

    private void preencherFormulario(Produto p) {
        produtoSelecionado = p;
        txtCodigo.setText(p.getCodigo());
        txtNome.setText(p.getNome());
        txtPreco.setText(String.valueOf(p.getPrecoVenda()));

        if (p.getPrecoCusto() != null) {
            if(txtCusto != null) txtCusto.setText(String.valueOf(p.getPrecoCusto()));
        } else {
            if(txtCusto != null) txtCusto.setText("0.0");
        }

        txtEstoque.setText(String.valueOf(p.getEstoque()));
        comboUnidade.setValue(p.getUnidade());
    }

    @FXML
    public void voltarMenu(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/br/com/churrasco/view/Menu.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}