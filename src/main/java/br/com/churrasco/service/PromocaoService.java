package br.com.churrasco.service;

import br.com.churrasco.dao.PromocaoDAO;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Promocao;
import br.com.churrasco.model.PromocaoAplicada;
import br.com.churrasco.model.PromocaoItem;
import br.com.churrasco.model.ResultadoPromocao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PromocaoService {

    private final PromocaoDAO promocaoDAO = new PromocaoDAO();

    public ResultadoPromocao calcularPromocoes(List<ItemVenda> carrinho) {
        ResultadoPromocao resultado = new ResultadoPromocao();
        resultado.setSubtotalOriginal(calcularSubtotal(carrinho));
        resultado.setDescontoPromocional(0.0);
        resultado.setPromocoesAplicadas(new ArrayList<>());

        if (carrinho == null || carrinho.isEmpty()) {
            return resultado;
        }

        List<Promocao> promocoesAtivas = promocaoDAO.listarAtivasParaData(LocalDate.now());
        if (promocoesAtivas.isEmpty()) {
            return resultado;
        }

        Map<Integer, Integer> quantidadesDisponiveis = mapearQuantidadesUn(carrinho);

        for (Promocao promocao : promocoesAtivas) {
            int combos = calcularQuantidadeDeCombos(promocao, quantidadesDisponiveis);
            if (combos <= 0) {
                continue;
            }

            double valorOriginalCombo = calcularValorOriginalCombo(promocao);
            double valorPromocionalTotal = arredondar(promocao.getPrecoCombo() * combos);
            double valorOriginalTotal = arredondar(valorOriginalCombo * combos);
            double desconto = arredondar(valorOriginalTotal - valorPromocionalTotal);

            if (desconto <= 0.0) {
                continue;
            }

            consumirQuantidades(promocao, quantidadesDisponiveis, combos);

            resultado.getPromocoesAplicadas().add(new PromocaoAplicada(
                    promocao.getId(),
                    promocao.getNome(),
                    combos,
                    desconto,
                    valorOriginalTotal,
                    valorPromocionalTotal
            ));
            resultado.setDescontoPromocional(arredondar(resultado.getDescontoPromocional() + desconto));
        }

        return resultado;
    }

    private Map<Integer, Integer> mapearQuantidadesUn(List<ItemVenda> carrinho) {
        Map<Integer, Integer> quantidades = new HashMap<>();
        for (ItemVenda item : carrinho) {
            if (item == null || item.getProduto() == null || item.getProduto().getId() == null) {
                continue;
            }
            if (!"UN".equalsIgnoreCase(item.getProduto().getUnidade())) {
                continue;
            }

            int quantidadeInteira = (int) Math.floor(item.getQuantidade());
            if (quantidadeInteira <= 0) {
                continue;
            }

            quantidades.merge(item.getProduto().getId(), quantidadeInteira, Integer::sum);
        }
        return quantidades;
    }

    private int calcularQuantidadeDeCombos(Promocao promocao, Map<Integer, Integer> quantidadesDisponiveis) {
        int combos = Integer.MAX_VALUE;
        if (promocao.getItens() == null || promocao.getItens().isEmpty()) {
            return 0;
        }

        for (PromocaoItem item : promocao.getItens()) {
            if (item.getProduto() == null || item.getProduto().getId() == null || item.getQuantidade() == null || item.getQuantidade() <= 0) {
                return 0;
            }

            int disponivel = quantidadesDisponiveis.getOrDefault(item.getProduto().getId(), 0);
            combos = Math.min(combos, disponivel / item.getQuantidade());
        }

        return combos == Integer.MAX_VALUE ? 0 : combos;
    }

    private void consumirQuantidades(Promocao promocao, Map<Integer, Integer> quantidadesDisponiveis, int combos) {
        for (PromocaoItem item : promocao.getItens()) {
            int produtoId = item.getProduto().getId();
            int restante = quantidadesDisponiveis.getOrDefault(produtoId, 0) - (item.getQuantidade() * combos);
            quantidadesDisponiveis.put(produtoId, Math.max(restante, 0));
        }
    }

    private double calcularValorOriginalCombo(Promocao promocao) {
        double total = 0.0;
        for (PromocaoItem item : promocao.getItens()) {
            if (item.getProduto() == null || item.getProduto().getPrecoVenda() == null || item.getQuantidade() == null) {
                continue;
            }
            total += item.getProduto().getPrecoVenda() * item.getQuantidade();
        }
        return arredondar(total);
    }

    private double calcularSubtotal(List<ItemVenda> carrinho) {
        return arredondar(carrinho.stream().mapToDouble(ItemVenda::getTotalItem).sum());
    }

    private double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
