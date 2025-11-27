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
    @FXML private TextField txtPreco;
    @FXML private TextField txtEstoque;
    @FXML private ComboBox<String> comboUnidade;

    @FXML private TableView<Produto> tabelaProdutos;
    @FXML private TableColumn<Produto, String> colCodigo;
    @FXML private TableColumn<Produto, String> colNome;
    @FXML private TableColumn<Produto, Double> colPreco;
    @FXML private TableColumn<Produto, String> colUnidade;
    @FXML private TableColumn<Produto, Double> colEstoque;

    private ProdutoDAO produtoDAO = new ProdutoDAO();
    private ObservableList<Produto> listaProdutos = FXCollections.observableArrayList();
    private Produto produtoSelecionado = null; // Para controlar Edição

    @FXML
    public void initialize() {
        configurarTabela();
        carregarDados();

        comboUnidade.getItems().addAll("KG", "UN");
        comboUnidade.getSelectionModel().selectFirst();

        // Evento: Ao clicar na tabela, preenche o formulário para editar
        tabelaProdutos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                preencherFormulario(newVal);
            }
        });
    }

    private void configurarTabela() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoVenda")); // Atenção aqui: nome exato do atributo no Model
        colUnidade.setCellValueFactory(new PropertyValueFactory<>("unidade"));
        colEstoque.setCellValueFactory(new PropertyValueFactory<>("estoque"));

        tabelaProdutos.setItems(listaProdutos);
    }

    private void carregarDados() {
        listaProdutos.clear();
        listaProdutos.addAll(produtoDAO.listarTodos());
    }

    @FXML
    public void salvarProduto() {
        try {
            // Validações básicas
            if (txtCodigo.getText().isEmpty() || txtNome.getText().isEmpty() || txtPreco.getText().isEmpty()) {
                mostrarAlerta("Preencha os campos obrigatórios!");
                return;
            }

            String codigo = txtCodigo.getText();
            String nome = txtNome.getText();
            double preco = Double.parseDouble(txtPreco.getText().replace(",", "."));
            double estoque = Double.parseDouble(txtEstoque.getText().replace(",", "."));
            String unidade = comboUnidade.getValue();

            if (produtoSelecionado == null) {
                // MODO: NOVO PRODUTO
                Produto novo = new Produto(null, codigo, nome, 0.0, preco, unidade, estoque);
                produtoDAO.salvar(novo);
            } else {
                // MODO: EDITAR PRODUTO EXISTENTE
                produtoSelecionado.setCodigo(codigo);
                produtoSelecionado.setNome(nome);
                produtoSelecionado.setPrecoVenda(preco);
                produtoSelecionado.setUnidade(unidade);
                produtoSelecionado.setEstoque(estoque);
                produtoDAO.atualizar(produtoSelecionado);
            }

            limparCampos();
            carregarDados(); // Recarrega a tabela

        } catch (NumberFormatException e) {
            mostrarAlerta("Preço ou Estoque inválidos! Use números.");
        } catch (Exception e) {
            mostrarAlerta("Erro ao salvar: " + e.getMessage());
        }
    }

    @FXML
    public void excluirProduto() {
        Produto p = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (p == null) {
            mostrarAlerta("Selecione um produto na tabela para excluir.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir");
        confirm.setContentText("Tem certeza que deseja excluir " + p.getNome() + "?");

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
        txtEstoque.setText("0");
        comboUnidade.getSelectionModel().selectFirst();
        produtoSelecionado = null; // Sai do modo de edição
        tabelaProdutos.getSelectionModel().clearSelection();
    }

    private void preencherFormulario(Produto p) {
        produtoSelecionado = p;
        txtCodigo.setText(p.getCodigo());
        txtNome.setText(p.getNome());
        txtPreco.setText(String.valueOf(p.getPrecoVenda()));
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