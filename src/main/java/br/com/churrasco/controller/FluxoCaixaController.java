package br.com.churrasco.controller;

import br.com.churrasco.dao.FinanceiroDAO;
import br.com.churrasco.model.Despesa;
import br.com.churrasco.model.FluxoCaixaItem;
import br.com.churrasco.model.Venda;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FluxoCaixaController {

    @FXML private DatePicker dtMes;
    @FXML private Label lblReceitas, lblDespesas, lblSaldo;

    // CHECKBOXES DO GRÁFICO
    @FXML private CheckBox chkVerAcumulado;
    @FXML private CheckBox chkVerReceita;
    @FXML private CheckBox chkVerDespesa;
    @FXML private CheckBox chkVerSaldoDia;

    @FXML private TableView<FluxoCaixaItem> tabelaFluxo;
    @FXML private TableColumn<FluxoCaixaItem, String> colDia;
    @FXML private TableColumn<FluxoCaixaItem, Double> colEntrada;
    @FXML private TableColumn<FluxoCaixaItem, Double> colSaida;
    @FXML private TableColumn<FluxoCaixaItem, Double> colSaldoDia;
    @FXML private TableColumn<FluxoCaixaItem, Double> colAcumulado;

    @FXML private LineChart<String, Number> graficoLinha;
    @FXML private BarChart<String, Number> graficoBarras;

    private final FinanceiroDAO financeiroDAO = new FinanceiroDAO();
    private final ObservableList<FluxoCaixaItem> listaFluxo = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        dtMes.setValue(LocalDate.now());

        configurarTabela();

        // Configura para recarregar ao mudar data ou clicar nos checkboxes
        dtMes.valueProperty().addListener((obs, oldVal, newVal) -> carregarDados());

        chkVerAcumulado.selectedProperty().addListener((obs, o, n) -> carregarDados());
        chkVerReceita.selectedProperty().addListener((obs, o, n) -> carregarDados());
        chkVerDespesa.selectedProperty().addListener((obs, o, n) -> carregarDados());
        chkVerSaldoDia.selectedProperty().addListener((obs, o, n) -> carregarDados());

        carregarDados();

        tabelaFluxo.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                FluxoCaixaItem item = tabelaFluxo.getSelectionModel().getSelectedItem();
                if (item != null) abrirDetalhesDia(item);
            }
        });
    }

    @FXML
    public void carregarDados() {
        LocalDate dataRef = dtMes.getValue();
        if (dataRef == null) return;

        List<FluxoCaixaItem> todosItens = financeiroDAO.buscarFluxoMensal(dataRef);
        List<FluxoCaixaItem> itensComMovimento = todosItens.stream()
                .filter(FluxoCaixaItem::temMovimento)
                .collect(Collectors.toList());

        listaFluxo.setAll(itensComMovimento);

        Map<String, Double> totais = financeiroDAO.buscarTotaisDRE(dataRef);
        double receita = totais.get("receita");
        double despesas = totais.get("despesas") + totais.get("custo_prod");
        double saldo = totais.get("lucro_liquido");

        lblReceitas.setText(formatar(receita));
        lblDespesas.setText(formatar(despesas));
        lblSaldo.setText(formatar(saldo));

        if (saldo >= 0) lblSaldo.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 18px;");
        else lblSaldo.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold; -fx-font-size: 18px;");

        atualizarGraficos(todosItens, totais);
    }

    private void atualizarGraficos(List<FluxoCaixaItem> itens, Map<String, Double> totais) {
        // --- A. GRÁFICO DE LINHA (MULTI-SÉRIE) ---
        graficoLinha.getData().clear();
        graficoLinha.setAnimated(false); // Desativa animação para atualizar rápido os checkboxes

        // Cria as séries baseadas nos checkboxes
        XYChart.Series<String, Number> sAcumulado = new XYChart.Series<>(); sAcumulado.setName("Acumulado");
        XYChart.Series<String, Number> sReceita = new XYChart.Series<>(); sReceita.setName("Receitas");
        XYChart.Series<String, Number> sDespesa = new XYChart.Series<>(); sDespesa.setName("Despesas");
        XYChart.Series<String, Number> sSaldoDia = new XYChart.Series<>(); sSaldoDia.setName("Lucro Diário");

        for (FluxoCaixaItem item : itens) {
            // Mostra dias passados ou com movimento
            if (item.getData().getDayOfMonth() <= LocalDate.now().getDayOfMonth() || item.temMovimento()) {
                String dia = String.valueOf(item.getData().getDayOfMonth());

                if (chkVerAcumulado.isSelected()) sAcumulado.getData().add(new XYChart.Data<>(dia, item.getSaldoAcumulado()));
                if (chkVerReceita.isSelected()) sReceita.getData().add(new XYChart.Data<>(dia, item.getReceitas()));
                if (chkVerDespesa.isSelected()) sDespesa.getData().add(new XYChart.Data<>(dia, item.getDespesas()));
                if (chkVerSaldoDia.isSelected()) sSaldoDia.getData().add(new XYChart.Data<>(dia, item.getSaldoDia()));
            }
        }

        // Adiciona ao gráfico apenas as séries que tem dados
        if (chkVerAcumulado.isSelected()) graficoLinha.getData().add(sAcumulado);
        if (chkVerReceita.isSelected()) graficoLinha.getData().add(sReceita);
        if (chkVerDespesa.isSelected()) graficoLinha.getData().add(sDespesa);
        if (chkVerSaldoDia.isSelected()) graficoLinha.getData().add(sSaldoDia);


        // --- B. GRÁFICO DE BARRAS (COM RÓTULOS INTERNOS) ---
        graficoBarras.getData().clear();
        graficoBarras.setAnimated(false);

        XYChart.Series<String, Number> seriesDRE = new XYChart.Series<>();
        seriesDRE.setName("Valores (R$)");

        seriesDRE.getData().add(criarDadoBarra("Receita", totais.get("receita")));
        seriesDRE.getData().add(criarDadoBarra("CMV", totais.get("custo_prod")));
        seriesDRE.getData().add(criarDadoBarra("Despesas", totais.get("despesas")));
        seriesDRE.getData().add(criarDadoBarra("Lucro Líq.", totais.get("lucro_liquido")));

        graficoBarras.getData().add(seriesDRE);
    }

    // Cria o dado e configura o listener para desenhar o texto dentro da barra
    private XYChart.Data<String, Number> criarDadoBarra(String categoria, Double valor) {
        XYChart.Data<String, Number> data = new XYChart.Data<>(categoria, valor);

        // O nó (barra) só é criado depois que o gráfico renderiza. Usamos nodeProperty para capturar esse momento.
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                // newNode é um StackPane por padrão no JavaFX BarChart
                setRotuloBarra((StackPane) newNode, valor);
            }
        });
        return data;
    }

    private void setRotuloBarra(StackPane barNode, Double valor) {
        // Formata o valor (ex: 1.5K ou valor normal)
        String textoValor = formatarCurto(valor);
        Label lbl = new Label(textoValor);

        // Estiliza o texto para ficar visível dentro da barra colorida
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(one-pass-box, black, 2, 0.0, 0, 1);");

        // Adiciona o label DENTRO da barra
        barNode.getChildren().add(lbl);
    }

    private String formatarCurto(double valor) {
        if (valor == 0) return "";
        if (Math.abs(valor) >= 1000) return String.format("%.1fk", valor / 1000); // Ex: 1.5k
        return String.format("%.0f", valor); // Ex: 500
    }

    // ... (Métodos abrirDetalhesDia, lancarDespesa, configurarTabela, formatar mantidos iguais) ...

    // --- REPITA AQUI OS MÉTODOS QUE JÁ EXISTIAM (abrirDetalhesDia, lancarDespesa, etc) ---
    // (Para economizar espaço na resposta, assumo que você manterá os métodos da versão anterior)

    // Vou reincluir apenas os pequenos utilitários para garantir que compile 100%
    private void abrirDetalhesDia(FluxoCaixaItem item) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detalhes: " + item.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dialog.setHeaderText("Movimentação Detalhada do Dia");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<String> listEntradas = new ListView<>();
        List<Venda> vendas = financeiroDAO.buscarVendasDetalhadasPorDia(item.getData());
        if (vendas.isEmpty()) listEntradas.getItems().add("Nenhuma venda registrada.");
        else for (Venda v : vendas) listEntradas.getItems().add(String.format("%s | Venda #%d | %s | %s", v.getDataHora().format(DateTimeFormatter.ofPattern("HH:mm")), v.getId(), formatar(v.getValorTotal()), v.getFormaPagamento()));

        ListView<String> listSaidas = new ListView<>();
        List<Despesa> despesas = financeiroDAO.buscarDespesasDetalhadasPorDia(item.getData());
        if (despesas.isEmpty()) listSaidas.getItems().add("Nenhuma despesa registrada.");
        else for (Despesa d : despesas) listSaidas.getItems().add(String.format("%s | %s | %s", d.getDescricao(), d.getCategoria(), formatar(d.getValor())));

        VBox boxEntradas = new VBox(5, new Label("ENTRADAS"), listEntradas);
        VBox boxSaidas = new VBox(5, new Label("SAÍDAS"), listSaidas);
        HBox layout = new HBox(10, boxEntradas, boxSaidas);
        HBox.setHgrow(boxEntradas, Priority.ALWAYS); HBox.setHgrow(boxSaidas, Priority.ALWAYS);
        layout.setPadding(new Insets(10)); layout.setPrefSize(800, 400);

        dialog.getDialogPane().setContent(layout);
        dialog.showAndWait();
    }

    @FXML
    public void lancarDespesa(ActionEvent event) {
        Dialog<Despesa> dialog = new Dialog<>();
        dialog.setTitle("Lançar Despesa");
        ButtonType btnSalvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, ButtonType.CANCEL);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField txtDesc = new TextField(); TextField txtVal = new TextField();
        ComboBox<String> cmb = new ComboBox<>(); cmb.getItems().addAll("FIXA", "VARIAVEL", "PESSOAL", "DESPERDICIO"); cmb.setValue("VARIAVEL");
        DatePicker dt = new DatePicker(LocalDate.now());

        grid.addRow(0, new Label("Descrição:"), txtDesc); grid.addRow(1, new Label("Valor:"), txtVal);
        grid.addRow(2, new Label("Categoria:"), cmb); grid.addRow(3, new Label("Data:"), dt);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == btnSalvar) {
                try { return new Despesa(null, txtDesc.getText(), Double.parseDouble(txtVal.getText().replace(",",".")), dt.getValue(), cmb.getValue(), ""); }
                catch (Exception e) { return null; }
            } return null;
        });

        dialog.showAndWait().ifPresent(d -> { financeiroDAO.salvarDespesa(d); carregarDados(); });
    }

    private void configurarTabela() {
        colDia.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getData().format(DateTimeFormatter.ofPattern("dd/MM"))));
        configurarColunaMoeda(colEntrada, "receitas", false);
        configurarColunaMoeda(colSaida, "despesas", true);
        configurarColunaMoeda(colSaldoDia, "saldoDia", false);
        configurarColunaMoeda(colAcumulado, "saldoAcumulado", false);
        tabelaFluxo.setItems(listaFluxo);
    }

    private void configurarColunaMoeda(TableColumn<FluxoCaixaItem, Double> coluna, String prop, boolean isDesp) {
        coluna.setCellValueFactory(new PropertyValueFactory<>(prop));
        coluna.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setTextFill(Color.BLACK); }
                else {
                    setText(formatar(item));
                    if(isDesp && item > 0) setTextFill(Color.RED);
                    else if(!isDesp) setTextFill(item < 0 ? Color.RED : (item > 0 ? Color.GREEN : Color.BLACK));
                }
            }
        });
    }

    @FXML public void voltarMenu(ActionEvent event) { try { ((Stage)((Node)event.getSource()).getScene().getWindow()).setScene(new Scene(FXMLLoader.load(getClass().getResource("/br/com/churrasco/view/Menu.fxml")))); } catch (Exception e) {} }
    private String formatar(double valor) { return String.format("R$ %.2f", valor); }
}