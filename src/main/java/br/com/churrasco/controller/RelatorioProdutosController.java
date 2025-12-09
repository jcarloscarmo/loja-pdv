package br.com.churrasco.controller;

import br.com.churrasco.dao.RelatorioDAO;
import br.com.churrasco.model.ItemRelatorio;
import javafx.beans.property.SimpleObjectProperty;
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

import java.time.LocalDate;
import java.util.List;

public class RelatorioProdutosController {

    @FXML private DatePicker dtInicio;
    @FXML private DatePicker dtFim;
    @FXML private Label lblTotalPeriodo;
    @FXML private Label lblLucroPeriodo;
    @FXML private Label lblCustoPeriodo; // <--- NOVO

    @FXML private TableView<ItemRelatorio> tabela;
    @FXML private TableColumn<ItemRelatorio, String> colCodigo;
    @FXML private TableColumn<ItemRelatorio, String> colNome;
    @FXML private TableColumn<ItemRelatorio, String> colUnidade;
    @FXML private TableColumn<ItemRelatorio, Double> colQtd;
    @FXML private TableColumn<ItemRelatorio, Double> colTotal;
    @FXML private TableColumn<ItemRelatorio, Double> colLucro;
    @FXML private TableColumn<ItemRelatorio, Double> colCusto; // <--- NOVO

    private RelatorioDAO relatorioDAO = new RelatorioDAO();
    private ObservableList<ItemRelatorio> lista = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        dtInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dtFim.setValue(LocalDate.now());

        configurarTabela();
        carregarDados();
    }

    @FXML
    public void carregarDados() {
        LocalDate inicio = dtInicio.getValue();
        LocalDate fim = dtFim.getValue();
        if (inicio == null || fim == null) return;

        List<ItemRelatorio> itens = relatorioDAO.buscarVendasPorPeriodo(inicio, fim);
        lista.setAll(itens);

        double totalGeral = lista.stream().mapToDouble(ItemRelatorio::getValorTotal).sum();
        double lucroGeral = lista.stream().mapToDouble(ItemRelatorio::getLucroTotal).sum();
        double custoGeral = totalGeral - lucroGeral; // Cálculo simples

        lblTotalPeriodo.setText(formatar(totalGeral));
        lblLucroPeriodo.setText(formatar(lucroGeral));
        lblCustoPeriodo.setText(formatar(custoGeral));
    }

    private void configurarTabela() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colUnidade.setCellValueFactory(new PropertyValueFactory<>("unidade"));
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidadeTotal"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));
        colLucro.setCellValueFactory(new PropertyValueFactory<>("lucroTotal"));

        // CALCULA O CUSTO NA TABELA (Faturamento - Lucro)
        colCusto.setCellValueFactory(data -> {
            double custo = data.getValue().getValorTotal() - data.getValue().getLucroTotal();
            return new SimpleObjectProperty<>(custo);
        });

        colQtd.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    ItemRelatorio linha = getTableView().getItems().get(getIndex());
                    String fmt = "KG".equals(linha.getUnidade()) ? "%.3f" : "%.0f";
                    setText(String.format(fmt, item));
                }
            }
        });

        colTotal.setCellFactory(this::criarCelulaMoeda);
        colLucro.setCellFactory(this::criarCelulaMoeda);
        colCusto.setCellFactory(this::criarCelulaMoeda);

        tabela.setItems(lista);
    }

    private TableCell<ItemRelatorio, Double> criarCelulaMoeda(TableColumn<ItemRelatorio, Double> param) {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(formatar(item));
            }
        };
    }

    @FXML
    public void voltarMenu(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/br/com/churrasco/view/Menu.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String formatar(double val) { return String.format("R$ %.2f", val); }
}