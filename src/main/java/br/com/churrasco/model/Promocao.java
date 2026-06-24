package br.com.churrasco.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promocao {
    private Integer id;
    private String nome;
    private Double precoCombo;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private boolean ativo;
    private List<PromocaoItem> itens = new ArrayList<>();

    public Promocao(Integer id, String nome, Double precoCombo, LocalDate dataInicio, LocalDate dataFim, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.precoCombo = precoCombo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.ativo = ativo;
        this.itens = new ArrayList<>();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Double getPrecoCombo() { return precoCombo; }
    public void setPrecoCombo(Double precoCombo) { this.precoCombo = precoCombo; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public List<PromocaoItem> getItens() { return itens; }
    public void setItens(List<PromocaoItem> itens) { this.itens = itens; }
}
