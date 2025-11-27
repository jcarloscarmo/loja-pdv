package br.com.churrasco.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Caixa {
    private Integer id;
    private Integer usuarioId;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataFechamento;
    private Double saldoInicial;   // Troco
    private Double saldoFinal;     // Calculado pelo sistema
    private Double saldoInformado; // Contado pelo operador
    private Double diferenca;      // Sobra ou Falta
    private String status;         // ABERTO ou FECHADO
}