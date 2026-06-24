package br.com.churrasco.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Venda {
    private Integer id;
    private LocalDateTime dataHora;
    private Double valorTotal; // Este valor já é o LÍQUIDO (Pago pelo cliente)
    private String formaPagamento;

    private Double desconto;
    private Double descontoManual;
    private Double descontoPromocional;

    // Campos auxiliares para Relatórios
    private Double valorCusto;
    private Double valorDinheiro;
    private Double valorDebito;
    private Double valorCredito;
    private Double valorPix;

    // CORREÇÃO: Como valorTotal já é o valor pago (pós-desconto),
    // não devemos subtrair o desconto novamente.
    public Double getLucro() {
        double vTotal = (valorTotal != null) ? valorTotal : 0.0;
        double vCusto = (valorCusto != null) ? valorCusto : 0.0;

        // Lucro = O que entrou no caixa - O que custou o produto
        return vTotal - vCusto;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public Double getValorTotal() { return valorTotal; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }
    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
    public Double getDesconto() { return desconto; }
    public void setDesconto(Double desconto) { this.desconto = desconto; }
    public Double getDescontoManual() { return descontoManual; }
    public void setDescontoManual(Double descontoManual) { this.descontoManual = descontoManual; }
    public Double getDescontoPromocional() { return descontoPromocional; }
    public void setDescontoPromocional(Double descontoPromocional) { this.descontoPromocional = descontoPromocional; }
    public Double getValorCusto() { return valorCusto; }
    public void setValorCusto(Double valorCusto) { this.valorCusto = valorCusto; }
    public Double getValorDinheiro() { return valorDinheiro; }
    public void setValorDinheiro(Double valorDinheiro) { this.valorDinheiro = valorDinheiro; }
    public Double getValorDebito() { return valorDebito; }
    public void setValorDebito(Double valorDebito) { this.valorDebito = valorDebito; }
    public Double getValorCredito() { return valorCredito; }
    public void setValorCredito(Double valorCredito) { this.valorCredito = valorCredito; }
    public Double getValorPix() { return valorPix; }
    public void setValorPix(Double valorPix) { this.valorPix = valorPix; }
}
