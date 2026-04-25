# Architecture Map

## Overview

O projeto `PDVChurrasco` e uma aplicacao desktop monolitica em Java 21 com JavaFX, Maven e SQLite.

Arquitetura real observada:
- UI em FXML + controllers JavaFX
- Persistencia em DAOs JDBC diretos sobre SQLite
- Models simples com Lombok
- Services focados em hardware e impressao
- Utils para banco, sessao, log, navegacao e geracao de cupom

Na pratica, a regra de negocio nao passa por uma service layer dedicada. O fluxo dominante e:

`Controller -> DAO -> SQLite`

O controller mais importante do sistema e `src/main/java/br/com/churrasco/controller/PDVController.java`, que concentra regras de venda, encomenda, leitura de balanca, estoque, navegacao de modais e disparo de impressao.

## Tech Stack

- Java 21
- JavaFX `javafx-controls`, `javafx-fxml`
- Maven
- SQLite via `sqlite-jdbc`
- Lombok
- SLF4J Simple
- `escpos-coffee` para impressora termica ESC/POS
- `jSerialComm` para balanca serial

## Entry Points

- `src/main/java/br/com/churrasco/Launcher.java`
- `src/main/java/br/com/churrasco/Main.java`

Fluxo de inicializacao:
1. `Launcher.main()` chama `Main.main()`
2. `Main.main()` abre conexao inicial com o banco via `DatabaseConnection.getConnection()`
3. `DatabaseConnection` cria/atualiza tabelas e garante usuario admin inicial
4. `Main.start()` carrega `Login.fxml`

Observacao:
- O comentario em `Main` menciona splash, mas o fluxo real carrega `Login.fxml`
- `Splash.fxml` e `SplashController` aparentam nao participar do fluxo atual

## Package Map

### `br.com.churrasco`

- `Main`: bootstrap JavaFX e inicializacao do banco
- `Launcher`: wrapper para execucao empacotada

### `br.com.churrasco.controller`

Controllers por tela. Sao o principal ponto de coordenacao do sistema.

Principais:
- `LoginController`: autenticacao e abertura do menu principal
- `MenuController`: hub de navegacao e controle visual de permissoes
- `PDVController`: frente de caixa, carrinho, estoque, pagamento, encomenda, fechamento de caixa e impressao
- `PagamentoController`: multipagamento, desconto, troco
- `ConfirmacaoController`: confirmacao e exibicao do cupom
- `CaixaController`: fechamento de caixa e autorizacao administrativa
- `ProdutosController`: CRUD de produtos
- `FluxoCaixaController`: fluxo mensal e DRE
- `RelatoriosController`: relatorio diario de vendas
- `RelatorioProdutosController`: ranking e rentabilidade por produto
- `ConfiguracoesController`: configuracoes da empresa, hardware e usuarios
- `HistoricoCaixasController`: auditoria de caixas
- `DetalheCaixaController`: detalhe de um caixa especifico
- `NovaEncomendaController`: coleta metadados de encomenda

Ponto de atencao:
- Existe `src/main/java/br/com/churrasco/controller/ImpressoraService.java`, que parece duplicado ou fora do pacote correto

### `br.com.churrasco.dao`

Persistencia JDBC direta.

- `UsuarioDAO`: autenticacao, CRUD e validacao de admin
- `ProdutoDAO`: CRUD de produtos e busca por codigo
- `VendaDAO`: vendas, pagamentos, itens, encomendas e baixa/restauracao de estoque
- `CaixaDAO`: abertura, fechamento, caixa aberto e saldo em dinheiro
- `FinanceiroDAO`: despesas, fluxo mensal e DRE
- `ConfigDAO`: chave/valor da aplicacao
- `RelatorioDAO`: agregacoes por periodo e produto
- `EncomendaDAO`: implementacao paralela que aparenta nao ser o fluxo principal atual

### `br.com.churrasco.model`

Models anemicos usados como DTOs do sistema.

Principais:
- `Usuario`
- `Produto`
- `ItemVenda`
- `Venda`
- `Pagamento`
- `Caixa`
- `Encomenda`
- `ItemEncomenda`
- `Despesa`
- `FluxoCaixaItem`
- `ItemRelatorio`

### `br.com.churrasco.service`

- `BalancaService`: leitura serial da balanca usando configuracao salva no banco
- `ImpressoraService`: impressao de cupom e relatorio de fechamento em ESC/POS

### `br.com.churrasco.util`

- `DatabaseConnection`: conexao SQLite, PRAGMA, criacao e migracoes simples
- `Sessao`: estado global do usuario logado
- `LogUtil`: log textual da aplicacao
- `CupomGenerator`: cupom textual para preview e confirmacao
- `Navegacao`: helper de troca de tela, aparentemente pouco ou nao usado
- `ConfigKeys`: constantes de configuracao, mas desalinhadas das chaves reais gravadas no banco

### `src/main/resources/br/com/churrasco/view`

FXMLs correspondentes as telas.

Principais:
- `Login.fxml`
- `Menu.fxml`
- `PDV.fxml`
- `Pagamento.fxml`
- `Confirmacao.fxml`
- `Produtos.fxml`
- `FluxoCaixa.fxml`
- `Relatorios.fxml`
- `RelatorioProdutos.fxml`
- `Configuracoes.fxml`
- `HistoricoCaixas.fxml`
- `DetalheCaixa.fxml`
- `NovaEncomenda.fxml`
- `FechamentoCaixa.fxml`

FXMLs com indicio de nao serem o fluxo real atual:
- `Splash.fxml`
- `AberturaCaixa.fxml`

## Screen to Controller to DAO Map

### Login
- FXML: `Login.fxml`
- Controller: `LoginController`
- DAOs: `UsuarioDAO`
- Util: `Sessao`, `LogUtil`

### Menu
- FXML: `Menu.fxml`
- Controller: `MenuController`
- Util: `Sessao`, `LogUtil`

### PDV
- FXML: `PDV.fxml`
- Controller: `PDVController`
- DAOs: `ProdutoDAO`, `VendaDAO`, `CaixaDAO`, `UsuarioDAO`
- Services: `BalancaService`, `ImpressoraService`
- Util: `CupomGenerator`, `Sessao`

### Pagamento
- FXML: `Pagamento.fxml`
- Controller: `PagamentoController`
- Model: `Pagamento`

### Confirmacao
- FXML: `Confirmacao.fxml`
- Controller: `ConfirmacaoController`
- Util: `CupomGenerator`

### Caixa / Fechamento
- FXML: `FechamentoCaixa.fxml`
- Controller: `CaixaController`
- DAOs: `CaixaDAO`, `VendaDAO`, `UsuarioDAO`
- Service: `ImpressoraService`

### Produtos
- FXML: `Produtos.fxml`
- Controller: `ProdutosController`
- DAO: `ProdutoDAO`

### Fluxo de Caixa
- FXML: `FluxoCaixa.fxml`
- Controller: `FluxoCaixaController`
- DAO: `FinanceiroDAO`

### Relatorios diarios
- FXML: `Relatorios.fxml`
- Controller: `RelatoriosController`
- DAO: `VendaDAO`
- Util: `CupomGenerator`

### Ranking de produtos
- FXML: `RelatorioProdutos.fxml`
- Controller: `RelatorioProdutosController`
- DAO: `RelatorioDAO`

### Configuracoes
- FXML: `Configuracoes.fxml`
- Controller: `ConfiguracoesController`
- DAOs: `ConfigDAO`, `UsuarioDAO`
- Services: `BalancaService`, `ImpressoraService`

## Runtime Modules

### Authentication and Session

- Usuario e autenticado por nome e senha em texto puro via `UsuarioDAO.autenticar()`
- Em caso de sucesso, `Sessao.setUsuario()` mantem o usuario em memoria
- A aplicacao depende de `Sessao.getUsuario()` em varios fluxos de autorizacao e rotulagem de tela

### PDV and Cart

- Busca de produto por codigo
- Diferenciacao entre produto por unidade e por peso
- Validacao de estoque com consideracao do que ja esta no carrinho
- Carrinho mantido em `ObservableList<ItemVenda>`
- Total visual recalculado no controller

### Payments

- Multipagamento suportado
- Desconto tratado antes da persistencia
- Valor liquido salvo em `vendas.valor_total`
- Pagamentos detalhados salvos em `pagamentos_venda`

### Orders / Encomendas

- Compartilham o mesmo espaco de identificador de venda
- Reserva de estoque e feita no momento da gravacao da encomenda
- Converter encomenda em venda envolve restaurar estoque da encomenda, excluir registros da encomenda e gravar a venda final

### Cash Register

- PDV exige caixa aberto
- Se nao existir, o proprio `PDVController` abre um `TextInputDialog`
- O fechamento usa `CaixaController` e pode exigir credenciais de admin

### Reports

- Relatorio diario usa `VendaDAO`
- Fluxo mensal usa `FinanceiroDAO`
- Ranking e margem por item usam `RelatorioDAO`

### Hardware

- Balança usa porta e velocidade salvas em `configuracoes`
- Impressora usa nome de impressora salvo em `configuracoes`

## Business Logic Placement

Regras importantes estao espalhadas nestes pontos:

- `PDVController`
- `CaixaController`
- `VendaDAO`
- `CaixaDAO`
- `DatabaseConnection`

Isso significa que alterar comportamento do negocio exige revisar controller e DAO juntos.

## Key Technical Risks

### 1. Controllers grandes e acoplados

Especialmente `PDVController`, que mistura:
- UI
- validacao
- regra de negocio
- leitura de hardware
- navegacao de modais
- persistencia indireta
- impressao

### 2. Regra de negocio sem camada dedicada

Nao existe uma service layer de negocio para venda, caixa ou encomenda.

### 3. Auditoria inconsistente por usuario

Foram encontrados usos de `usuario_id = 1` em pontos criticos:
- abertura de caixa em `CaixaDAO`
- gravacao de venda em `VendaDAO`

Isso significa que parte da auditoria nao reflete o operador logado.

### 4. Seguranca fraca de autenticacao

- senha armazenada em texto puro
- admin padrao criado automaticamente como `ADMIN / admin`

### 5. Funcionalidades incompletas

- `CaixaController.imprimirFechamento()` vazio
- `CaixaController.exibirRelatorioFinal(...)` vazio
- `CaixaDAO.listarHistorico()` retorna lista vazia

### 6. Inconsistencias de codigo

- `Splash` nao entra no fluxo real
- `AberturaCaixa.fxml` nao e o fluxo real de abertura
- `ConfigKeys` nao bate com as chaves efetivamente usadas
- existe classe `controller.ImpressoraService` em pacote improvavel

### 7. Banco com migracao artesanal

- `ALTER TABLE` protegido por `catch` vazio
- sem versionamento formal de schema
- sem migration runner

### 8. Integridade e dialeto SQLite

- `PRAGMA foreign_keys=ON` nao esta explicito
- consultas usam `CAST(... AS UNSIGNED)`, que nao e idiomatico para SQLite

### 9. Tratamento de erros irregular

Ha varios `catch` vazios ou que apenas fazem `printStackTrace()`.

## Change Safety Guide

Antes de mexer, siga estas regras:

### Ao alterar fluxo de venda
Revise juntos:
- `PDVController`
- `PagamentoController`
- `ConfirmacaoController`
- `VendaDAO`
- `ItemVenda`
- `Venda`
- `CupomGenerator`
- `ImpressoraService`

### Ao alterar abertura/fechamento de caixa
Revise juntos:
- `PDVController`
- `CaixaController`
- `CaixaDAO`
- `VendaDAO`

### Ao alterar encomendas
Revise juntos:
- `PDVController`
- `NovaEncomendaController`
- `VendaDAO`
- tabelas `encomendas` e `itens_encomenda`

### Ao alterar configuracoes de hardware
Revise juntos:
- `ConfiguracoesController`
- `ConfigDAO`
- `BalancaService`
- `ImpressoraService`
- chaves reais na tabela `configuracoes`

### Ao alterar usuarios e permissao
Revise juntos:
- `LoginController`
- `MenuController`
- `UsuarioDAO`
- `Sessao`

## Recommended Refactoring Order

Se for evoluir a aplicacao, a ordem mais segura e:

1. Corrigir auditoria real de usuario e caixa
2. Completar historico e fechamento de caixa
3. Extrair service layer de venda e caixa
4. Consolidar fluxo de encomenda
5. Introduzir migracoes formais de banco
6. Melhorar autenticacao e hash de senha
7. Remover classes e telas orfas

## Reference Files

Arquivos mais importantes para entendimento rapido:

- `src/main/java/br/com/churrasco/Main.java`
- `src/main/java/br/com/churrasco/util/DatabaseConnection.java`
- `src/main/java/br/com/churrasco/controller/PDVController.java`
- `src/main/java/br/com/churrasco/controller/CaixaController.java`
- `src/main/java/br/com/churrasco/controller/ConfiguracoesController.java`
- `src/main/java/br/com/churrasco/controller/RelatoriosController.java`
- `src/main/java/br/com/churrasco/dao/VendaDAO.java`
- `src/main/java/br/com/churrasco/dao/CaixaDAO.java`
- `src/main/java/br/com/churrasco/dao/UsuarioDAO.java`
- `src/main/java/br/com/churrasco/service/ImpressoraService.java`
- `src/main/java/br/com/churrasco/service/BalancaService.java`
