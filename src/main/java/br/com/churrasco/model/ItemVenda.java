package br.com.churrasco.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Data;

@Data // O Lombok gera getters/setters, mas como você definiu manualmente, ele respeita os seus.
public class ItemVenda {
    private Produto produto;
    private double quantidade;
    private double precoUnitario;
    private double custoUnitario;
    private double totalItem;

    public ItemVenda() {}

    public ItemVenda(Produto produto, double quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPrecoVenda();

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
// ... restante da classe ...

    public void setProduto(Produto produto) {
        this.produto = produto;
        if (produto != null) {
            // SÓ TENTA LER PREÇO SE ELE EXISTIR (EVITA O NULL POINTER)
            if (produto.getPrecoVenda() != null) {
                this.precoUnitario = produto.getPrecoVenda();
            }
            // SÓ TENTA LER CUSTO SE ELE EXISTIR
            if (produto.getPrecoCusto() != null) {
                this.custoUnitario = produto.getPrecoCusto();
            } else {
                this.custoUnitario = 0.0;
            }
        }
    }

    // ... restante da classe ...

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

    public double getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(double custoUnitario) { this.custoUnitario = custoUnitario; }

    public double getTotalItem() { return totalItem; }

    // --- O MÉTODO QUE FALTAVA ---
    public void setTotalItem(double totalItem) {
        this.totalItem = totalItem;
    }

    public String getNomeProduto() {
        return produto != null ? produto.getNome() : "";
    }
}