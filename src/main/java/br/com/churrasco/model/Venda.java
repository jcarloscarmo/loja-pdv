package br.com.churrasco.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Venda {
    private Integer id;
    private LocalDateTime dataHora;
    private Double valorTotal;
    private String formaPagamento;

    // Campos auxiliares para Relatórios
    private Double valorCusto; // <--- NOVO
    private Double valorDinheiro;
    private Double valorDebito;
    private Double valorCredito;
    private Double valorPix;

    // Método auxiliar para facilitar na tela
    public Double getLucro() {
        double vTotal = (valorTotal != null) ? valorTotal : 0.0;
        double vCusto = (valorCusto != null) ? valorCusto : 0.0;
        return vTotal - vCusto;
    }
}