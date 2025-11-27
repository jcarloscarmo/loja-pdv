package br.com.churrasco.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Venda {
    // Campos que existem na tabela 'vendas' do banco
    private Integer id;
    private LocalDateTime dataHora;
    private Double valorTotal;
    private String formaPagamento;

    // --- CAMPOS AUXILIARES PARA RELATÓRIO ---
    // Esses campos não existem na tabela 'vendas' do banco de dados.
    // Eles serão preenchidos pelo VendaDAO através do cálculo dos pagamentos
    // apenas para exibição na Tabela de Relatórios.
    private double valorDinheiro;
    private double valorDebito;
    private double valorCredito;
    private double valorPix;
}