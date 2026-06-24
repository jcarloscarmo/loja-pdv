package br.com.churrasco.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromocaoAplicada {
    private Integer promocaoId;
    private String nomePromocao;
    private Integer quantidadeAplicada;
    private Double descontoAplicado;
    private Double valorOriginal;
    private Double valorPromocional;

    public Integer getPromocaoId() { return promocaoId; }
    public void setPromocaoId(Integer promocaoId) { this.promocaoId = promocaoId; }
    public String getNomePromocao() { return nomePromocao; }
    public void setNomePromocao(String nomePromocao) { this.nomePromocao = nomePromocao; }
    public Integer getQuantidadeAplicada() { return quantidadeAplicada; }
    public void setQuantidadeAplicada(Integer quantidadeAplicada) { this.quantidadeAplicada = quantidadeAplicada; }
    public Double getDescontoAplicado() { return descontoAplicado; }
    public void setDescontoAplicado(Double descontoAplicado) { this.descontoAplicado = descontoAplicado; }
    public Double getValorOriginal() { return valorOriginal; }
    public void setValorOriginal(Double valorOriginal) { this.valorOriginal = valorOriginal; }
    public Double getValorPromocional() { return valorPromocional; }
    public void setValorPromocional(Double valorPromocional) { this.valorPromocional = valorPromocional; }
}
