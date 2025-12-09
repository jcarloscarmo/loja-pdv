package br.com.churrasco.model;

import lombok.Data;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class ItemEncomenda {
    private Integer id;
    private Integer encomendaId;
    private Produto produto; // Mantemos o objeto Produto para acessar o nome na tela
    private Double quantidade;
    private Double valorUnitario;
    private Double totalItem;

    public ItemEncomenda() {}

    public ItemEncomenda(Produto produto, Double quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.valorUnitario = produto.getPrecoVenda();
        calcularTotal();
    }

    // Recalcula o total sempre que mudar a quantidade (com arredondamento correto)
    public void setQuantidade(Double quantidade) {
        this.quantidade = quantidade;
        calcularTotal();
    }

    private void calcularTotal() {
        if (quantidade == null || valorUnitario == null) {
            this.totalItem = 0.0;
            return;
        }
        BigDecimal qtdBd = BigDecimal.valueOf(quantidade);
        BigDecimal precoBd = BigDecimal.valueOf(valorUnitario);
        BigDecimal totalBd = qtdBd.multiply(precoBd).setScale(2, RoundingMode.HALF_UP);
        this.totalItem = totalBd.doubleValue();
    }

    // Helper para exibir na tabela
    public String getNomeProduto() {
        return produto != null ? produto.getNome() : "";
    }
}