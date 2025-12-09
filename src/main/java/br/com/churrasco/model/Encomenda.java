package br.com.churrasco.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Encomenda {
    private Integer id;
    private String nomeCliente;
    private LocalDateTime dataRetirada;
    private Double valorTotal;
    private String status; // 'PENDENTE' ou 'FINALIZADA'
    private List<ItemVenda> itens = new ArrayList<>();

    // Construtor vazio padrão
    public Encomenda() {}

    public Encomenda(String nomeCliente, LocalDateTime dataRetirada) {
        this.nomeCliente = nomeCliente;
        this.dataRetirada = dataRetirada;
        this.valorTotal = 0.0;
        this.status = "PENDENTE";
    }

}