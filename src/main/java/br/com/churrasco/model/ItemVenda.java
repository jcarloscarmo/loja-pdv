package br.com.churrasco.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ItemVenda {
    private Produto produto;
    private double quantidade;
    private double precoUnitario;
    private double totalItem;

    // Construtor vazio
    public ItemVenda() {}

    public ItemVenda(Produto produto, double quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPrecoVenda();
        calcularTotalItem(); // Calcula já arredondando
    }

    // Método robusto para cálculo financeiro
    private void calcularTotalItem() {
        BigDecimal qtdBd = BigDecimal.valueOf(this.quantidade);
        BigDecimal precoBd = BigDecimal.valueOf(this.precoUnitario);

        // Multiplica e arredonda para 2 casas decimais (HALF_UP é o padrão comercial)
        BigDecimal totalBd = qtdBd.multiply(precoBd)
                .setScale(2, RoundingMode.HALF_UP);

        this.totalItem = totalBd.doubleValue();
    }

    // Getters e Setters
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) {
        this.produto = produto;
        if (produto != null) this.precoUnitario = produto.getPrecoVenda();
    }

    public double getQuantidade() { return quantidade; }
    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
        calcularTotalItem(); // Recalcula se mudar a quantidade
    }

    public double getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(double precoUnitario) {
        this.precoUnitario = precoUnitario;
        calcularTotalItem(); // Recalcula se mudar o preço
    }

    public double getTotalItem() { return totalItem; }

    // Helper para a tabela (propriedade nomeProduto)
    public String getNomeProduto() {
        return produto != null ? produto.getNome() : "";
    }
}