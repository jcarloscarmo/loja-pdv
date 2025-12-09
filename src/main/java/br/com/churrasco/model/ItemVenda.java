package br.com.churrasco.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Data; // Se estiver usando Lombok, ajuda. Se não, mantenha os getters/setters manuais abaixo.

public class ItemVenda {
    private Produto produto;
    private double quantidade;
    private double precoUnitario;
    private double custoUnitario; // <--- NOVO CAMPO: Custo Histórico
    private double totalItem;

    // Construtor vazio
    public ItemVenda() {}

    public ItemVenda(Produto produto, double quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPrecoVenda();

        // --- A MÁGICA DO SNAPSHOT ---
        // Copia o custo do cadastro PARA A VENDA neste exato momento.
        if (produto.getPrecoCusto() != null) {
            this.custoUnitario = produto.getPrecoCusto();
        } else {
            this.custoUnitario = 0.0;
        }

        calcularTotalItem();
    }

    private void calcularTotalItem() {
        BigDecimal qtdBd = BigDecimal.valueOf(this.quantidade);
        BigDecimal precoBd = BigDecimal.valueOf(this.precoUnitario);
        BigDecimal totalBd = qtdBd.multiply(precoBd).setScale(2, RoundingMode.HALF_UP);
        this.totalItem = totalBd.doubleValue();
    }

    // Getters e Setters
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) {
        this.produto = produto;
        if (produto != null) {
            this.precoUnitario = produto.getPrecoVenda();
            // Atualiza custo se mudar o produto
            this.custoUnitario = (produto.getPrecoCusto() != null) ? produto.getPrecoCusto() : 0.0;
        }
    }

    public double getQuantidade() { return quantidade; }
    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
        calcularTotalItem();
    }

    public double getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(double precoUnitario) {
        this.precoUnitario = precoUnitario;
        calcularTotalItem();
    }

    // Getter e Setter do Novo Campo
    public double getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(double custoUnitario) { this.custoUnitario = custoUnitario; }

    public double getTotalItem() { return totalItem; }

    public String getNomeProduto() {
        return produto != null ? produto.getNome() : "";
    }
}