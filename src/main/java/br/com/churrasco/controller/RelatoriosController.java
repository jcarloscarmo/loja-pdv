package br.com.churrasco.controller;

import br.com.churrasco.dao.VendaDAO;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import br.com.churrasco.model.Venda;
import br.com.churrasco.util.CupomGenerator;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RelatoriosController {

    @FXML private DatePicker dtPicker;

    @FXML private Label lblTotalGeral, lblTotalLucro, lblTotalCusto;
    @FXML private Label lblTotalDinheiro, lblTotalPix, lblTotalCartao;

    @FXML private TableView<Venda> tabelaVendas;
    @FXML private TableColumn<Venda, Integer> colId;
    @FXML private TableColumn<Venda, String> colHora;
    @FXML private TableColumn<Venda, Double> colValor;
    @FXML private TableColumn<Venda, Double> colCusto; // <--- NOVO
    @FXML private TableColumn<Venda, Double> colLucro;

    @FXML private TableColumn<Venda, Double> colDinheiro;
    @FXML private TableColumn<Venda, Double> colDebito;
    @FXML private TableColumn<Venda, Double> colCredito;
    @FXML private TableColumn<Venda, Double> colPix;

    private VendaDAO vendaDAO = new VendaDAO();
    private ObservableList<Venda> listaVendas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        dtPicker.setValue(LocalDate.now());
        configurarTabela();
        carregarDados();

        tabelaVendas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 || event.getClickCount() == 1) {
                Venda vendaSelecionada = tabelaVendas.getSelectionModel().getSelectedItem();
                if (vendaSelecionada != null) abrirCupomVenda(vendaSelecionada);
            }
        });
    }

    @FXML
    public void carregarDados() {
        LocalDate data = dtPicker.getValue();
        if (data == null) return;

        List<Venda> vendas = vendaDAO.buscarVendasDetalhadasPorData(data);
        listaVendas.setAll(vendas);

        double tDin = vendaDAO.buscarTotalPorTipo(data, "DINHEIRO");
        double tPix = vendaDAO.buscarTotalPorTipo(data, "PIX");
        double tDeb = vendaDAO.buscarTotalPorTipo(data, "DÉBITO");
        double tCre = vendaDAO.buscarTotalPorTipo(data, "CRÉDITO");
        double tTotal = tDin + tPix + tDeb + tCre;

        // Somas Totais
        double tLucro = vendas.stream().mapToDouble(Venda::getLucro).sum();
        // Custo = Total - Lucro (Como o lucro já é Venda - Custo, matematicamente isso dá o custo)
        double tCusto = tTotal - tLucro;

        lblTotalDinheiro.setText(formatar(tDin));
        lblTotalPix.setText(formatar(tPix));
        lblTotalCartao.setText(formatar(tDeb + tCre));

        lblTotalGeral.setText(formatar(tTotal));
        lblTotalCusto.setText(formatar(tCusto));
        lblTotalLucro.setText(formatar(tLucro));
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colHora.setCellValueFactory(cell -> {
            if (cell.getValue().getDataHora() != null)
                return new SimpleStringProperty(cell.getValue().getDataHora().format(DateTimeFormatter.ofPattern("HH:mm")));
            return null;
        });

        configurarColunaMoeda(colDinheiro, "valorDinheiro");
        configurarColunaMoeda(colDebito, "valorDebito");
        configurarColunaMoeda(colCredito, "valorCredito");
        configurarColunaMoeda(colPix, "valorPix");
        configurarColunaMoeda(colValor, "valorTotal");
        configurarColunaMoeda(colLucro, "lucro");

        // Coluna Custo (Mapeia direto do atributo que criamos no Model Venda)
        configurarColunaMoeda(colCusto, "valorCusto");

        tabelaVendas.setItems(listaVendas);
    }

    private void configurarColunaMoeda(TableColumn<Venda, Double> coluna, String propriedade) {
        coluna.setCellValueFactory(new PropertyValueFactory<>(propriedade));
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || Math.abs(item) < 0.01) {
                    setText("-");
                } else {
                    setText(formatar(item));
                }
            }
        });
    }

    private void abrirCupomVenda(Venda venda) {
        try {
            List<ItemVenda> itens = vendaDAO.buscarItensPorVenda(venda.getId());
            List<Pagamento> pagamentos = vendaDAO.buscarPagamentosPorVenda(venda.getId());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Confirmacao.fxml"));
            Parent root = loader.load();
            ConfirmacaoController controller = loader.getController();
            controller.setTextoCupom(CupomGenerator.gerarTexto(itens, pagamentos, venda.getValorTotal()));
            controller.ativarModoLeitura();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Detalhes da Venda");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
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

    private String formatar(double valor) { return String.format("R$ %.2f", valor); }
}