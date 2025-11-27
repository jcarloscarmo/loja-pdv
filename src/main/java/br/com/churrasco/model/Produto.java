package br.com.churrasco.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Produto {
    private Integer id;
    private String codigo;
    private String nome;
    private Double precoCusto;
    private Double precoVenda;
    private String unidade;
    private Double estoque;
}