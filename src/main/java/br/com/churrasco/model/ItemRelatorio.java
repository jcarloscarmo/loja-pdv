package br.com.churrasco.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemRelatorio {
    private String codigo;
    private String nome;
    private String unidade;
    private Double quantidadeTotal;
    private Double valorTotal;
}