# Flow Sequences

## Purpose

Este arquivo resume os fluxos criticos do sistema para consulta rapida antes de alterar controllers, DAOs ou tabelas.

## 1. Inicializacao

Sequencia:
1. `Launcher.main()`
2. `Main.main()`
3. `DatabaseConnection.getConnection()`
4. criacao/migracao basica das tabelas
5. abertura de `Login.fxml`

Observacoes:
- o banco `pdv.db` passa a ficar em um diretorio persistente do usuario em `%APPDATA%/PDVChurrasco`
- se existir um `pdv.db` legado ao lado da aplicacao, ele e migrado automaticamente antes da criacao de um novo banco
- o usuario `ADMIN` sem senha e criado apenas quando a tabela `usuarios` esta vazia

## 2. Login -> Menu

Sequencia:
1. `LoginController.initialize()` carrega nomes de usuarios
2. operador seleciona usuario e informa senha
3. `UsuarioDAO.autenticar(nome, senha)`
4. em sucesso, `Sessao.setUsuario(usuario)`
5. abre `Menu.fxml`

Pontos importantes:
- senha e comparada diretamente no banco
- nao ha hash de senha
- `MenuController` esconde botoes para usuarios nao admin, mas isso e controle visual

## 3. Menu -> Modulos

Tela central:
- `Menu.fxml`

Rotas principais:
- PDV -> `PDV.fxml`
- Produtos -> `Produtos.fxml`
- Fluxo de Caixa -> `FluxoCaixa.fxml`
- Relatorios diarios -> `Relatorios.fxml`
- Ranking produtos -> `RelatorioProdutos.fxml`
- Auditoria -> `HistoricoCaixas.fxml`
- Configuracoes -> `Configuracoes.fxml`

## 4. PDV -> Abertura obrigatoria de caixa

Sequencia:
1. `PDVController.initialize()`
2. `verificarEForcarAberturaCaixa()`
3. `CaixaDAO.buscarCaixaAberto()`
4. se nao houver caixa aberto, abre `TextInputDialog`
5. `CaixaDAO.abrirCaixa(valor)`

Importante:
- o fluxo real nao usa `AberturaCaixa.fxml`
- se o usuario cancelar a abertura, o controller volta ao menu

## 5. PDV -> Busca de produto -> Carrinho

Sequencia:
1. operador digita codigo
2. `ProdutoDAO.buscarPorCodigo(codigo)`
3. `PDVController` atualiza informacoes visuais do produto
4. se unidade = `KG`, habilita `txtPeso`
5. se unidade = `UN`, define quantidade `1` e adiciona direto
6. `validarEstoque(produto, quantidade)`
7. adiciona `ItemVenda` ao carrinho

Regras importantes:
- a validacao considera o estoque total menos o que ja esta no carrinho
- em modo de edicao, o item atual e temporariamente desconsiderado do somatorio

## 6. PDV -> Leitura da balanca

Sequencia:
1. `txtPeso` recebe foco
2. `PDVController.iniciarLoopBalanca()`
3. loop em thread chama `BalancaService.lerPeso()`
4. `BalancaService` abre a porta serial, envia `ENQ`, le resposta, fecha porta e converte peso
5. UI atualiza `txtPeso` e `lblStatusBalanca`

Pontos de atencao:
- a porta e aberta e fechada a cada leitura
- o loop roda com `sleep(100)`
- `BalancaService` faz heuristica para converter gramas em kg quando valor excede 50

## 7. PDV -> Finalizacao de venda

Sequencia:
1. usuario clica em finalizar
2. `PagamentoController` e aberto em modal
3. operador informa formas de pagamento e desconto
4. `ConfirmacaoController` e aberto em modal
5. se confirmado, `PDVController.salvarVendaNoBanco(...)`
6. cria `Venda`
7. `VendaDAO.salvarVenda(venda, itens, pagamentos)`
8. dispara impressao em thread separada
9. reseta o PDV

Persistencia da venda:
1. insere em `vendas`
2. insere em `itens_venda`
3. insere em `pagamentos_venda`
4. baixa estoque em `produtos`
5. faz `commit`

Regra importante:
- `valor_total` salvo em `vendas` ja representa o valor liquido, apos desconto

## 8. PDV -> Encomenda

Sequencia:
1. carrinho montado
2. operador abre `NovaEncomenda.fxml`
3. `NovaEncomendaController` coleta dados do cliente e retirada
4. `PDVController` monta `Encomenda`
5. `VendaDAO.salvarEncomenda(enc)`
6. itens vao para `itens_encomenda`
7. estoque e baixado imediatamente
8. card da encomenda e exibido no topo do PDV

Regras importantes:
- encomenda compartilha o mesmo espaco de IDs com vendas
- estoque fica reservado assim que a encomenda e gravada

## 9. Encomenda pendente -> Retomar ou cancelar

Retomar:
1. PDV carrega encomendas pendentes em `recuperarEncomendasPendentes()`
2. ao abrir uma encomenda, itens voltam para o carrinho
3. o ID da encomenda fica em `idEncomendaEmAndamento`
4. ao concluir venda, `VendaDAO` restaura estoque da encomenda antiga, exclui encomenda e grava a venda final

Cancelar:
1. `VendaDAO.cancelarEncomenda(id)`
2. restaura estoque
3. exclui itens da encomenda
4. exclui cabecalho da encomenda

## 10. PDV -> Fechar caixa

Sequencia:
1. operador solicita fechamento
2. fluxo bloqueia se houver carrinho nao vazio ou encomendas pendentes
3. `CaixaController.carregarDadosFechamento()` calcula saldo esperado
4. se usuario logado nao for admin, solicita credenciais de admin
5. `CaixaDAO.fecharCaixa(...)`
6. opcionalmente pergunta sobre impressao
7. fecha a janela

Pontos de atencao:
- `imprimirFechamento()` esta vazio
- `exibirRelatorioFinal(...)` esta vazio

## 11. Produtos -> CRUD

Sequencia:
1. tela carrega lista de produtos
2. cadastro e edicao usam `ProdutoDAO.salvar()` e `ProdutoDAO.atualizar()`
3. exclusao usa `ProdutoDAO.deletar()`
4. codigo pode ser sugerido por `buscarProximoCodigoDisponivel()`

## 12. Fluxo de Caixa mensal

Sequencia:
1. usuario escolhe mes de referencia
2. `FinanceiroDAO.buscarFluxoMensal(dataRef)` monta receitas e despesas por dia
3. `FinanceiroDAO.buscarTotaisDRE(dataRef)` calcula receita, custo, despesas e lucro liquido
4. `FluxoCaixaController` exibe graficos e tabela

## 13. Relatorios diarios

Sequencia:
1. usuario escolhe data
2. `VendaDAO.buscarVendasDetalhadasPorData(data)` busca vendas com agregacao de pagamentos e custo
3. tela calcula totais gerais
4. clique em uma venda abre cupom em modo leitura

Ponto de atencao:
- o codigo atual abre detalhe em clique simples ou duplo

## 14. Ranking de produtos

Sequencia:
1. usuario escolhe periodo
2. `RelatorioDAO.buscarVendasPorPeriodo(inicio, fim)` agrega itens por produto
3. `RelatorioDAO.buscarTotalDescontosPorPeriodo(inicio, fim)` busca descontos totais
4. tela exibe faturamento, quantidade e lucro por item

## 15. Configuracoes

Sequencia:
1. `ConfiguracoesController.initialize()`
2. carrega listas de impressoras e portas seriais
3. carrega configuracoes salvas do banco
4. carrega lista de usuarios
5. salva configuracoes com `ConfigDAO`

Inclui:
- dados da empresa para cupom
- impressora
- porta e velocidade da balanca
- CRUD de usuarios
- teste de balanca
- teste de impressora

## 16. Historico e auditoria de caixas

Fluxo esperado:
1. abrir `HistoricoCaixas.fxml`
2. listar caixas fechados
3. selecionar caixa
4. abrir detalhes

Ponto de atencao atual:
- `CaixaDAO.listarHistorico()` retorna lista vazia
- auditoria provavelmente esta incompleta

## Impact Analysis Quick Guide

Quando alterar:

### Venda
Revise:
- `PDVController`
- `PagamentoController`
- `ConfirmacaoController`
- `VendaDAO`
- `ItemVenda`
- `Venda`

### Caixa
Revise:
- `PDVController`
- `CaixaController`
- `CaixaDAO`
- `VendaDAO`

### Encomenda
Revise:
- `PDVController`
- `NovaEncomendaController`
- `VendaDAO`

### Hardware
Revise:
- `ConfiguracoesController`
- `BalancaService`
- `ImpressoraService`
- `ConfigDAO`
