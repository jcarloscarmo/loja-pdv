# Screen Matrix

## Purpose

Mapa rapido de telas, controllers, dependencias e riscos de impacto.

## Matrix

### `Login.fxml`
- Controller: `LoginController`
- Responsabilidade: autenticar usuario e abrir menu
- DAOs: `UsuarioDAO`
- Utils: `Sessao`, `LogUtil`
- Fluxos relacionados: login, sessao
- Risco de impacto: medio

### `Menu.fxml`
- Controller: `MenuController`
- Responsabilidade: navegacao principal e ocultacao visual de modulos por perfil
- Utils: `Sessao`, `LogUtil`
- Fluxos relacionados: navegacao, logout
- Risco de impacto: medio

### `PDV.fxml`
- Controller: `PDVController`
- Responsabilidade: frente de caixa completa
- DAOs: `ProdutoDAO`, `VendaDAO`, `CaixaDAO`, `UsuarioDAO`
- Services: `BalancaService`, `ImpressoraService`
- Utils: `CupomGenerator`, `Sessao`
- Fluxos relacionados: abertura de caixa, venda, pagamento, encomenda, fechamento de caixa
- Risco de impacto: muito alto

### `Pagamento.fxml`
- Controller: `PagamentoController`
- Responsabilidade: compor pagamentos, desconto e troco
- Models: `Pagamento`
- Fluxos relacionados: finalizacao de venda
- Risco de impacto: alto

### `Confirmacao.fxml`
- Controller: `ConfirmacaoController`
- Responsabilidade: confirmar operacao e exibir cupom
- Utils: `CupomGenerator`
- Fluxos relacionados: venda, relatorio diario
- Risco de impacto: alto

### `NovaEncomenda.fxml`
- Controller: `NovaEncomendaController`
- Responsabilidade: coletar dados de encomenda
- Fluxos relacionados: encomenda
- Risco de impacto: medio

### `Produtos.fxml`
- Controller: `ProdutosController`
- Responsabilidade: CRUD de produtos
- DAOs: `ProdutoDAO`
- Fluxos relacionados: cadastro, estoque, PDV
- Risco de impacto: alto

### `FluxoCaixa.fxml`
- Controller: `FluxoCaixaController`
- Responsabilidade: visao mensal de fluxo e DRE
- DAOs: `FinanceiroDAO`
- Fluxos relacionados: despesas, financeiro
- Risco de impacto: medio

### `Relatorios.fxml`
- Controller: `RelatoriosController`
- Responsabilidade: relatorio diario de vendas
- DAOs: `VendaDAO`
- Utils: `CupomGenerator`
- Fluxos relacionados: vendas, cupom em modo leitura
- Risco de impacto: medio

### `RelatorioProdutos.fxml`
- Controller: `RelatorioProdutosController`
- Responsabilidade: ranking e margem por produto
- DAOs: `RelatorioDAO`
- Fluxos relacionados: rentabilidade
- Risco de impacto: medio

### `Configuracoes.fxml`
- Controller: `ConfiguracoesController`
- Responsabilidade: dados da empresa, hardware e usuarios
- DAOs: `ConfigDAO`, `UsuarioDAO`
- Services: `BalancaService`, `ImpressoraService`
- Fluxos relacionados: impressao, balanca, cadastro de usuario
- Risco de impacto: alto

### `FechamentoCaixa.fxml`
- Controller: `CaixaController`
- Responsabilidade: conferencia e fechamento de caixa
- DAOs: `CaixaDAO`, `VendaDAO`, `UsuarioDAO`
- Services: `ImpressoraService`
- Fluxos relacionados: caixa, autorizacao admin
- Risco de impacto: alto

### `HistoricoCaixas.fxml`
- Controller: `HistoricoCaixasController`
- Responsabilidade: auditoria de caixas
- DAOs esperados: `CaixaDAO`, possivelmente `VendaDAO`
- Fluxos relacionados: auditoria
- Risco de impacto: medio
- Observacao: depende de historico real no DAO

### `DetalheCaixa.fxml`
- Controller: `DetalheCaixaController`
- Responsabilidade: detalhar um caixa selecionado
- DAOs esperados: `CaixaDAO`, `VendaDAO`
- Fluxos relacionados: auditoria, fechamento
- Risco de impacto: medio

### `Splash.fxml`
- Controller: `SplashController`
- Responsabilidade observada: tela residual ou nao integrada ao fluxo atual
- Fluxos relacionados: inicializacao teorica
- Risco de impacto: baixo
- Observacao: nao aparenta ser usada

### `AberturaCaixa.fxml`
- Controller esperado: abertura de caixa
- Responsabilidade observada: tela residual ou alternativa nao usada
- Fluxos relacionados: abertura de caixa teorica
- Risco de impacto: baixo
- Observacao: o fluxo real abre `TextInputDialog` no `PDVController`

## Suggested Usage

Antes de alterar uma tela:
1. localizar a linha correspondente nesta matriz
2. abrir o controller e os DAOs listados
3. revisar os fluxos relacionados em `FLOW_SEQUENCES.md`
4. revisar riscos tecnicos em `TECH_DEBT.md`
