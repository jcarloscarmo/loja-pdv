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
}