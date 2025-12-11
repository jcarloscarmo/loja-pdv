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
    @FXML private Label lblTotalDescontos;

    @FXML private TableView<Venda> tabelaVendas;
    @FXML private TableColumn<Venda, Integer> colId;
    @FXML private TableColumn<Venda, String> colHora;
    @FXML private TableColumn<Venda, Double> colValor;
    @FXML private TableColumn<Venda, Double> colDesconto;
    @FXML private TableColumn<Venda, Double> colCusto;
    @FXML private TableColumn<Venda, Double> colLucro;

    @FXML private TableColumn<Venda, Double> colDinheiro;
    @FXML private TableColumn<Venda, Double> colDebito;
    @FXML private TableColumn<Venda, Double> colCredito;
    @FXML private TableColumn<Venda, Double> colPix;

    private final VendaDAO vendaDAO = new VendaDAO();
    private final ObservableList<Venda> listaVendas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        dtPicker.setValue(LocalDate.now());
        configurarTabela();
        carregarDados();

        dtPicker.valueProperty().addListener((obs, oldVal, newVal) -> carregarDados());

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

        // 1. Busca vendas do banco (trazendo detalhes de pagamentos e custos)
        List<Venda> vendas = vendaDAO.buscarVendasDetalhadasPorData(data);
        listaVendas.setAll(vendas);

        // 2. Calcula os totais iterando sobre a lista carregada (Garate que o total bate com a tabela)
        // Isso evita fazer 4 selects extras no banco e garante consistência matemática
        double tDin = vendas.stream().mapToDouble(v -> v.getValorDinheiro() != null ? v.getValorDinheiro() : 0.0).sum();
        double tPix = vendas.stream().mapToDouble(v -> v.getValorPix() != null ? v.getValorPix() : 0.0).sum();

        double tDeb = vendas.stream().mapToDouble(v -> v.getValorDebito() != null ? v.getValorDebito() : 0.0).sum();
        double tCre = vendas.stream().mapToDouble(v -> v.getValorCredito() != null ? v.getValorCredito() : 0.0).sum();
        double tCartao = tDeb + tCre;

        // Total Geral (Soma das entradas reais)
        double tTotalEntrada = tDin + tPix + tCartao;

        // Descontos
        double tDescontos = vendas.stream()
                .mapToDouble(v -> v.getDesconto() != null ? v.getDesconto() : 0.0)
                .sum();

        // Custo (Soma direta dos custos unitários registrados)
        double tCusto = vendas.stream()
                .mapToDouble(v -> v.getValorCusto() != null ? v.getValorCusto() : 0.0)
                .sum();

        // Lucro (Agora usa a lógica corrigida da Model: ValorTotal - Custo)
        double tLucro = vendas.stream().mapToDouble(Venda::getLucro).sum();

        // 3. Atualiza a tela
        lblTotalDinheiro.setText(formatar(tDin));
        lblTotalPix.setText(formatar(tPix));
        lblTotalCartao.setText(formatar(tCartao));

        lblTotalGeral.setText(formatar(tTotalEntrada));
        lblTotalCusto.setText(formatar(tCusto));
        lblTotalLucro.setText(formatar(tLucro));

        if (lblTotalDescontos != null) {
            lblTotalDescontos.setText(formatar(tDescontos));
            if (tDescontos > 0) lblTotalDescontos.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            else lblTotalDescontos.setStyle("-fx-text-fill: black;");
        }
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

        configurarColunaMoeda(colValor, "valorTotal"); // Valor Pago (Líquido)
        configurarColunaMoeda(colDesconto, "desconto");

        configurarColunaMoeda(colLucro, "lucro");
        configurarColunaMoeda(colCusto, "valorCusto");

        tabelaVendas.setItems(listaVendas);
    }

    private void configurarColunaMoeda(TableColumn<Venda, Double> coluna, String propriedade) {
        if (coluna == null) return;
        coluna.setCellValueFactory(new PropertyValueFactory<>(propriedade));
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || Math.abs(item) < 0.01) {
                    setText("-");
                } else {
                    setText(formatar(item));
                    // Opcional: Pintar lucro negativo de vermelho
                    if (propriedade.equals("lucro") && item < 0) {
                        setStyle("-fx-text-fill: red;");
                    } else {
                        setStyle("-fx-text-fill: black;");
                    }
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

            // ATENÇÃO: Verifique se ConfirmacaoController existe e tem esses métodos
            // Se der erro aqui, me avise para corrigir o controller do cupom
            Object controller = loader.getController();

            // Usando reflection ou cast se tiver a classe ConfirmacaoController
            // Aqui assumo que o método existe conforme seu código original
            try {
                // ConfirmacaoController c = (ConfirmacaoController) controller;
                // c.setTextoCupom(...);
                // c.ativarModoLeitura();

                // Mantendo sua lógica original refletida:
                java.lang.reflect.Method setTexto = controller.getClass().getMethod("setTextoCupom", String.class);
                java.lang.reflect.Method ativarLeitura = controller.getClass().getMethod("ativarModoLeitura");

                setTexto.invoke(controller, CupomGenerator.gerarTexto(itens, pagamentos, venda.getValorTotal()));
                ativarLeitura.invoke(controller);

            } catch (Exception ex) {
                System.err.println("Erro ao chamar métodos do ConfirmacaoController: " + ex.getMessage());
            }

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Detalhes da Venda #" + venda.getId());
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