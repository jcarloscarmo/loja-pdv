package br.com.churrasco.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Despesa {
    private Integer id;
    private String descricao;
    private Double valor;
    private LocalDate dataPagamento;
    private String categoria; // FIXA, VARIAVEL, PESSOAL, DESPERDICIO
    private String observacao;
}