package br.com.churrasco.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FluxoCaixaItem {
    private LocalDate data;
    private Double receitas;      // Total vendido no dia
    private Double despesas;      // Total pago no dia (Contas + Perdas)
    private Double saldoDia;      // receitas - despesas
    private Double saldoAcumulado; // Saldo do dia anterior + saldo do dia atual

    // Método auxiliar para saber se o dia teve movimentação
    public boolean temMovimento() {
        return (receitas != null && receitas != 0) || (despesas != null && despesas != 0);
    }
}