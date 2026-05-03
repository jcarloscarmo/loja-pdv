package br.com.churrasco.util;

import br.com.churrasco.dao.ConfigDAO;
import br.com.churrasco.model.ItemVenda;
import br.com.churrasco.model.Pagamento;
import br.com.churrasco.model.Produto;
import br.com.churrasco.model.Venda;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CupomGenerator {

    private static final int LARGURA_CUPOM = 32;

    public record ConfiguracaoCupom(
            String empresaNome,
            String cnpj,
            String endereco,
            String telefone,
            String rodape,
            boolean printCnpj,
            boolean printEndereco,
            boolean printTelefone,
            boolean printDataHora
    ) {}

    public static String gerarTexto(List<ItemVenda> itens, List<Pagamento> pagamentos, double totalLiquido) {
        return formatarCupom(carregarConfiguracao(), null, LocalDateTime.now(), itens, pagamentos, totalLiquido);
    }

    public static String gerarTexto(Venda venda, List<ItemVenda> itens, List<Pagamento> pagamentos) {
        Integer idVenda = venda != null ? venda.getId() : null;
        LocalDateTime dataHora = venda != null && venda.getDataHora() != null ? venda.getDataHora() : LocalDateTime.now();
        double totalLiquido = venda != null && venda.getValorTotal() != null ? venda.getValorTotal() : 0.0;

        return formatarCupom(carregarConfiguracao(), idVenda, dataHora, itens, pagamentos, totalLiquido);
    }

    public static String gerarPreview(ConfiguracaoCupom config) {
        List<ItemVenda> itens = new ArrayList<>();

        Produto picanha = new Produto();
        picanha.setNome("Picanha Fatiada");
        picanha.setUnidade("KG");
        picanha.setPrecoVenda(59.90);
        itens.add(new ItemVenda(picanha, 0.750));

        Produto refrigerante = new Produto();
        refrigerante.setNome("Refrigerante Lata");
        refrigerante.setUnidade("UN");
        refrigerante.setPrecoVenda(9.90);
        itens.add(new ItemVenda(refrigerante, 3));

        List<Pagamento> pagamentos = List.of(
                new Pagamento("DINHEIRO", 50.00),
                new Pagamento("PIX", 22.63)
        );

        return formatarCupom(config, 1234, LocalDateTime.now(), itens, pagamentos, 72.63);
    }

    public static List<String> gerarLinhasVenda(Venda venda, List<ItemVenda> itens, List<Pagamento> pagamentos) {
        return gerarLinhas(
                carregarConfiguracao(),
                venda != null ? venda.getId() : null,
                venda != null && venda.getDataHora() != null ? venda.getDataHora() : LocalDateTime.now(),
                itens,
                pagamentos,
                venda != null && venda.getValorTotal() != null ? venda.getValorTotal() : 0.0
        );
    }

    public static List<String> gerarLinhasPreview(ConfiguracaoCupom config) {
        return List.of(gerarPreview(config).split("\\R", -1));
    }

    public static ConfiguracaoCupom carregarConfiguracao() {
        ConfigDAO config = new ConfigDAO();
        return new ConfiguracaoCupom(
                config.getValor("empresa_nome").orElse("CHURRASCARIA"),
                config.getValor("empresa_cnpj").orElse(""),
                config.getValor("empresa_endereco").orElse(""),
                config.getValor("empresa_telefone").orElse(""),
                config.getValor("rodape_cupom").orElse("Obrigado pela preferencia!"),
                Boolean.parseBoolean(config.getValor("print_cnpj").orElse("true")),
                Boolean.parseBoolean(config.getValor("print_endereco").orElse("true")),
                Boolean.parseBoolean(config.getValor("print_telefone").orElse("true")),
                Boolean.parseBoolean(config.getValor("print_datahora").orElse("true"))
        );
    }

    private static String formatarCupom(ConfiguracaoCupom config, Integer idVenda, LocalDateTime dataHora, List<ItemVenda> itens, List<Pagamento> pagamentos, double totalLiquido) {
        return String.join("\n", gerarLinhas(config, idVenda, dataHora, itens, pagamentos, totalLiquido));
    }

    private static List<String> gerarLinhas(ConfiguracaoCupom config, Integer idVenda, LocalDateTime dataHora, List<ItemVenda> itens, List<Pagamento> pagamentos, double totalLiquido) {
        List<String> linhas = new ArrayList<>();
        List<ItemVenda> itensSeguros = itens != null ? itens : List.of();
        List<Pagamento> pagamentosSeguros = pagamentos != null ? pagamentos : List.of();

        adicionarCentralizado(linhas, valorOuPadrao(config.empresaNome(), "CHURRASCARIA"));

        if (config.printCnpj()) {
            adicionarTexto(linhas, prefixar("CNPJ: ", config.cnpj()));
        }
        if (config.printEndereco()) {
            adicionarTexto(linhas, config.endereco());
        }
        if (config.printTelefone()) {
            adicionarTexto(linhas, prefixar("Tel: ", config.telefone()));
        }
        if (config.printDataHora()) {
            LocalDateTime data = dataHora != null ? dataHora : LocalDateTime.now();
            linhas.add("Data: " + data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        if (idVenda != null) {
            linhas.add("Recibo #" + idVenda);
        }

        linhas.add(separador());

        double subtotal = 0.0;
        for (ItemVenda item : itensSeguros) {
            adicionarTexto(linhas, item != null ? item.getNomeProduto() : "Item");
            linhas.add(alinharEsquerdaDireita(descreverQuantidade(item), formatarValor(item != null ? item.getTotalItem() : 0.0)));
            subtotal += item != null ? item.getTotalItem() : 0.0;
        }

        if (!itensSeguros.isEmpty()) {
            linhas.add(separador());
        }

        double desconto = subtotal - totalLiquido;
        if (desconto > 0.01) {
            linhas.add(alinharEsquerdaDireita("SUBTOTAL", formatarValor(subtotal)));
            linhas.add(alinharEsquerdaDireita("DESCONTO", formatarValor(desconto)));
        }
        linhas.add(alinharEsquerdaDireita("TOTAL", formatarValor(totalLiquido)));

        if (!pagamentosSeguros.isEmpty()) {
            linhas.add(separador());
            linhas.add("PAGAMENTOS");
            for (Pagamento pagamento : pagamentosSeguros) {
                String tipo = pagamento != null ? valorOuPadrao(pagamento.getTipo(), "PAGAMENTO") : "PAGAMENTO";
                double valor = pagamento != null ? pagamento.getValor() : 0.0;
                linhas.add(alinharEsquerdaDireita(tipo, formatarValor(valor)));
            }

            double totalPago = pagamentosSeguros.stream().mapToDouble(Pagamento::getValor).sum();
            double troco = totalPago - totalLiquido;
            if (troco > 0.01) {
                linhas.add(alinharEsquerdaDireita("TROCO", formatarValor(troco)));
            }
        }

        String rodape = valorOuPadrao(config.rodape(), "Obrigado pela preferencia!");
        if (!rodape.isBlank()) {
            linhas.add(separador());
            adicionarCentralizado(linhas, rodape);
        }

        return linhas;
    }

    private static void adicionarCentralizado(List<String> linhas, String texto) {
        for (String parte : quebrarTexto(texto)) {
            linhas.add(centralizar(parte));
        }
    }

    private static void adicionarTexto(List<String> linhas, String texto) {
        for (String parte : quebrarTexto(texto)) {
            linhas.add(parte);
        }
    }

    private static String descreverQuantidade(ItemVenda item) {
        if (item == null) {
            return "0 x R$ 0.00";
        }

        String unidade = Optional.ofNullable(item.getProduto()).map(Produto::getUnidade).orElse("UN");
        String quantidade = "KG".equalsIgnoreCase(unidade)
                ? String.format("%.3fkg", item.getQuantidade())
                : String.format("%.0f un", item.getQuantidade());

        return quantidade + " x " + formatarValor(item.getPrecoUnitario());
    }

    private static String alinharEsquerdaDireita(String esquerda, String direita) {
        String textoEsquerda = valorOuPadrao(esquerda, "").trim();
        String textoDireita = valorOuPadrao(direita, "").trim();

        if (textoEsquerda.length() + textoDireita.length() + 1 >= LARGURA_CUPOM) {
            return cortar(textoEsquerda + " " + textoDireita);
        }

        int espacos = LARGURA_CUPOM - textoEsquerda.length() - textoDireita.length();
        return textoEsquerda + " ".repeat(Math.max(1, espacos)) + textoDireita;
    }

    private static List<String> quebrarTexto(String texto) {
        List<String> partes = new ArrayList<>();
        String valor = valorOuPadrao(texto, "").trim();
        if (valor.isEmpty()) {
            return partes;
        }

        String restante = valor;
        while (restante.length() > LARGURA_CUPOM) {
            int quebra = restante.lastIndexOf(' ', LARGURA_CUPOM);
            if (quebra <= 0) {
                quebra = LARGURA_CUPOM;
            }

            partes.add(cortar(restante.substring(0, quebra).trim()));
            restante = restante.substring(quebra).trim();
        }

        if (!restante.isEmpty()) {
            partes.add(cortar(restante));
        }
        return partes;
    }

    private static String centralizar(String texto) {
        String valor = cortar(valorOuPadrao(texto, ""));
        if (valor.length() >= LARGURA_CUPOM) {
            return valor;
        }
        int espacos = (LARGURA_CUPOM - valor.length()) / 2;
        return " ".repeat(Math.max(0, espacos)) + valor;
    }

    private static String separador() {
        return "-".repeat(LARGURA_CUPOM);
    }

    private static String cortar(String texto) {
        String valor = valorOuPadrao(texto, "");
        return valor.length() <= LARGURA_CUPOM ? valor : valor.substring(0, LARGURA_CUPOM);
    }

    private static String prefixar(String prefixo, String valor) {
        String texto = valorOuPadrao(valor, "").trim();
        return texto.isEmpty() ? "" : prefixo + texto;
    }

    private static String valorOuPadrao(String valor, String padrao) {
        return valor != null ? valor : padrao;
    }

    private static String formatarValor(double valor) {
        return String.format("R$ %.2f", valor);
    }
}
