package br.com.churrasco.controller;

import br.com.churrasco.dao.CaixaDAO;
import br.com.churrasco.model.Caixa;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class HistoricoCaixasController {

    @FXML private TableView<Caixa> tabelaHistorico;
    @FXML private TableColumn<Caixa, Integer> colId;
    @FXML private TableColumn<Caixa, String> colAbertura;
    @FXML private TableColumn<Caixa, String> colFechamento;
    @FXML private TableColumn<Caixa, String> colStatus;
    @FXML private TableColumn<Caixa, Double> colSaldoInicial;
    @FXML private TableColumn<Caixa, Double> colSaldoSistema;
    @FXML private TableColumn<Caixa, Double> colSaldoInformado;
    @FXML private TableColumn<Caixa, Double> colDiferenca;

    private CaixaDAO caixaDAO = new CaixaDAO();
    private ObservableList<Caixa> lista = FXCollections.observableArrayList();
    private DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        configurarColunas();
        carregarDados();

        // --- EVENTO DE CLIQUE NA LINHA ---
        tabelaHistorico.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Duplo clique
                Caixa caixaSelecionado = tabelaHistorico.getSelectionModel().getSelectedItem();
                if (caixaSelecionado != null) {
                    abrirDetalheCaixa(caixaSelecionado.getId());
                }
            }
        });
    }

    private void abrirDetalheCaixa(int caixaId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/DetalheCaixa.fxml"));
            Parent root = loader.load();

            DetalheCaixaController controller = loader.getController();
            controller.carregarVendasDoCaixa(caixaId); // Passa o ID

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Detalhamento de Caixa");
            stage.initModality(Modality.APPLICATION_MODAL); // Bloqueia a tela de trás
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarDados() {
        lista.setAll(caixaDAO.listarHistorico());
        tabelaHistorico.setItems(lista);
    }

    private void configurarColunas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        // Formatar Datas
        colAbertura.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDataAbertura().format(fmt)));

        colFechamento.setCellValueFactory(cell -> {
            if (cell.getValue().getDataFechamento() != null)
                return new SimpleStringProperty(cell.getValue().getDataFechamento().format(fmt));
            return new SimpleStringProperty("-");
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Formatar Valores R$
        configurarColunaMoeda(colSaldoInicial, "saldoInicial");
        configurarColunaMoeda(colSaldoSistema, "saldoFinal");
        configurarColunaMoeda(colSaldoInformado, "saldoInformado");

        // Coluna DIFERENÇA com CORES
        colDiferenca.setCellValueFactory(new PropertyValueFactory<>("diferenca"));
        colDiferenca.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("R$ %.2f", item));
                    if (item < -0.01) setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    else if (item > 0.01) setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    else setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });
    }

    private void configurarColunaMoeda(TableColumn<Caixa, Double> coluna, String prop) {
        coluna.setCellValueFactory(new PropertyValueFactory<>(prop));
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "-" : String.format("R$ %.2f", item));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
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