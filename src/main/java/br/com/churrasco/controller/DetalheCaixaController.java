package br.com.churrasco.controller;

import br.com.churrasco.dao.VendaDAO;
import br.com.churrasco.model.Venda;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class DetalheCaixaController {

    @FXML private Label lblTitulo;
    @FXML private TableView<Venda> tabelaVendas;
    @FXML private TableColumn<Venda, Integer> colId;
    @FXML private TableColumn<Venda, String> colHora;
    @FXML private TableColumn<Venda, Double> colDinheiro, colPix, colCartao;

    // NOVA COLUNA DE DESCONTO
    @FXML private TableColumn<Venda, Double> colDesconto;

    @FXML private TableColumn<Venda, Double> colValor;

    private final VendaDAO vendaDAO = new VendaDAO();
    private final ObservableList<Venda> lista = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarColunas();
    }

    public void carregarVendasDoCaixa(int caixaId) {
        lblTitulo.setText("VENDAS DO CAIXA Nº " + caixaId);
        var vendas = vendaDAO.buscarVendasDetalhadasPorCaixa(caixaId);
        lista.setAll(vendas);
    }

    private void configurarColunas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colHora.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDataHora().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));

        configurarColunaMoeda(colDinheiro, "valorDinheiro");
        configurarColunaMoeda(colPix, "valorPix");

        // Agrupa Crédito e Débito numa coluna só
        colCartao.setCellValueFactory(cell -> {
            double totalCartao = cell.getValue().getValorDebito() + cell.getValue().getValorCredito();
            return new javafx.beans.property.SimpleObjectProperty<>(totalCartao);
        });
        configurarCellFactoryMoeda(colCartao);

        // Coluna Desconto
        configurarColunaMoeda(colDesconto, "desconto");

        // Valor Final
        configurarColunaMoeda(colValor, "valorTotal");

        tabelaVendas.setItems(lista);
    }

    private void configurarColunaMoeda(TableColumn<Venda, Double> coluna, String prop) {
        if(coluna == null) return;
        coluna.setCellValueFactory(new PropertyValueFactory<>(prop));
        configurarCellFactoryMoeda(coluna);
    }

    private void configurarCellFactoryMoeda(TableColumn<Venda, Double> coluna) {
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null || Math.abs(item) < 0.01) ? "-" : String.format("R$ %.2f", item));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    @FXML public void fecharJanela() {
        ((Stage) tabelaVendas.getScene().getWindow()).close();
    }
}