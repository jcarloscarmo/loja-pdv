package br.com.churrasco.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromocaoItem {
    private Integer id;
    private Integer promocaoId;
    private Produto produto;
    private Integer quantidade;

    public String getNomeProduto() {
        return produto != null ? produto.getNome() : "";
    }

    public String getCodigoProduto() {
        return produto != null ? produto.getCodigo() : "";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPromocaoId() { return promocaoId; }
    public void setPromocaoId(Integer promocaoId) { this.promocaoId = promocaoId; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
}
