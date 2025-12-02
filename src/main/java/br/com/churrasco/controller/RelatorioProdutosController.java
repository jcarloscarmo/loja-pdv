package br.com.churrasco.controller;

import br.com.churrasco.dao.RelatorioDAO;
import br.com.churrasco.model.ItemRelatorio;
import javafx.beans.property.SimpleStringProperty;
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

    @FXML private TableView<ItemRelatorio> tabela;
    @FXML private TableColumn<ItemRelatorio, String> colCodigo;
    @FXML private TableColumn<ItemRelatorio, String> colNome;
    @FXML private TableColumn<ItemRelatorio, String> colUnidade;
    @FXML private TableColumn<ItemRelatorio, Double> colQtd;
    @FXML private TableColumn<ItemRelatorio, Double> colTotal;

    private RelatorioDAO relatorioDAO = new RelatorioDAO();
    private ObservableList<ItemRelatorio> lista = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Padrão: Início do mês até hoje
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

        // Calcula totalzão lá embaixo
        double totalGeral = lista.stream().mapToDouble(ItemRelatorio::getValorTotal).sum();
        lblTotalPeriodo.setText(String.format("R$ %.2f", totalGeral));
    }

    private void configurarTabela() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colUnidade.setCellValueFactory(new PropertyValueFactory<>("unidade"));
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidadeTotal"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));

        // Formatação Inteligente de Quantidade (KG vs UN)
        colQtd.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    ItemRelatorio linha = getTableView().getItems().get(getIndex());
                    if ("KG".equals(linha.getUnidade())) {
                        setText(String.format("%.3f", item));
                    } else {
                        setText(String.format("%.0f", item));
                    }
                }
            }
        });

        // Formatação Moeda
        colTotal.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("R$ %.2f", item));
            }
        });

        tabela.setItems(lista);
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
}