package br.com.churrasco.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pagamento {
    private String tipo; // DINHEIRO, DEBITO, CREDITO, PIX
    private double valor;
}