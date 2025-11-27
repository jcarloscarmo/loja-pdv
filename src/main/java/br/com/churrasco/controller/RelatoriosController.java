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

    // Cards
    @FXML private Label lblTotalGeral, lblTotalDinheiro, lblTotalPix, lblTotalDebito, lblTotalCredito;

    // Tabela e Colunas Novas
    @FXML private TableView<Venda> tabelaVendas;
    @FXML private TableColumn<Venda, Integer> colId;
    @FXML private TableColumn<Venda, String> colHora;
    @FXML private TableColumn<Venda, Double> colDinheiro;
    @FXML private TableColumn<Venda, Double> colDebito;
    @FXML private TableColumn<Venda, Double> colCredito;
    @FXML private TableColumn<Venda, Double> colPix;
    @FXML private TableColumn<Venda, Double> colValor;

    private VendaDAO vendaDAO = new VendaDAO();
    private ObservableList<Venda> listaVendas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        dtPicker.setValue(LocalDate.now());
        configurarTabela();
        carregarDados();

        // --- EVENTO DE CLIQUE NA LINHA (ABRIR AMARELINHO) ---
        tabelaVendas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 || event.getClickCount() == 1) { // 1 ou 2 cliques
                Venda vendaSelecionada = tabelaVendas.getSelectionModel().getSelectedItem();
                if (vendaSelecionada != null) {
                    abrirCupomVenda(vendaSelecionada);
                }
            }
        });
    }

    @FXML
    public void carregarDados() {
        LocalDate data = dtPicker.getValue();
        if (data == null) return;

        // 1. Carrega Tabela (Agora usando o método detalhado)
        List<Venda> vendas = vendaDAO.buscarVendasDetalhadasPorData(data);
        listaVendas.setAll(vendas);

        // 2. Carrega Cards (Mantém a lógica que já funcionava bem)
        double tDin = vendaDAO.buscarTotalPorTipo(data, "DINHEIRO");
        double tPix = vendaDAO.buscarTotalPorTipo(data, "PIX");
        double tDeb = vendaDAO.buscarTotalPorTipo(data, "DÉBITO");
        double tCre = vendaDAO.buscarTotalPorTipo(data, "CRÉDITO");

        lblTotalDinheiro.setText(formatarMoeda(tDin));
        lblTotalPix.setText(formatarMoeda(tPix));
        lblTotalDebito.setText(formatarMoeda(tDeb));
        lblTotalCredito.setText(formatarMoeda(tCre));
        lblTotalGeral.setText(formatarMoeda(tDin + tPix + tDeb + tCre));
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colHora.setCellValueFactory(cell -> {
            if (cell.getValue().getDataHora() != null)
                return new SimpleStringProperty(cell.getValue().getDataHora().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            return null;
        });

        // Configura as colunas de valores usando os campos novos da Venda
        configurarColunaMoeda(colDinheiro, "valorDinheiro");
        configurarColunaMoeda(colDebito, "valorDebito");
        configurarColunaMoeda(colCredito, "valorCredito");
        configurarColunaMoeda(colPix, "valorPix");
        configurarColunaMoeda(colValor, "valorTotal"); // Total geral da linha

        tabelaVendas.setItems(listaVendas);
    }

    // Método auxiliar para não repetir código de formatação R$
    private void configurarColunaMoeda(TableColumn<Venda, Double> coluna, String propriedade) {
        coluna.setCellValueFactory(new PropertyValueFactory<>(propriedade));
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText("-"); // Fica mais limpo que mostrar "R$ 0,00"
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #bdc3c7;"); // Cinza claro
                } else {
                    setText(formatarMoeda(item));
                    // Mantém a cor original da coluna definida no FXML
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });
    }

    // --- REUTILIZA A TELA DE CONFIRMAÇÃO PARA MOSTRAR O CUPOM ---
    private void abrirCupomVenda(Venda venda) {
        try {
            List<ItemVenda> itens = vendaDAO.buscarItensPorVenda(venda.getId());
            List<Pagamento> pagamentos = vendaDAO.buscarPagamentosPorVenda(venda.getId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/churrasco/view/Confirmacao.fxml"));
            Parent root = loader.load();

            ConfirmacaoController controller = loader.getController();

            String texto = CupomGenerator.gerarTexto(itens, pagamentos, venda.getValorTotal());
            controller.setTextoCupom(texto);

            // --- AQUI ESTÁ A MUDANÇA ---
            controller.ativarModoLeitura(); // Transforma o modal em "Apenas Visualizar"
            // ---------------------------

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Detalhes da Venda Nº " + venda.getId());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private String formatarMoeda(double valor) {
        return String.format("R$ %.2f", valor);
    }
}