package br.com.churrasco.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoPromocao {
    private double subtotalOriginal;
    private double descontoPromocional;
    private List<PromocaoAplicada> promocoesAplicadas = new ArrayList<>();

    public double getTotalComPromocao() {
        return subtotalOriginal - descontoPromocional;
    }

    public double getSubtotalOriginal() { return subtotalOriginal; }
    public void setSubtotalOriginal(double subtotalOriginal) { this.subtotalOriginal = subtotalOriginal; }
    public double getDescontoPromocional() { return descontoPromocional; }
    public void setDescontoPromocional(double descontoPromocional) { this.descontoPromocional = descontoPromocional; }
    public List<PromocaoAplicada> getPromocoesAplicadas() { return promocoesAplicadas; }
    public void setPromocoesAplicadas(List<PromocaoAplicada> promocoesAplicadas) { this.promocoesAplicadas = promocoesAplicadas; }
}
