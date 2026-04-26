# Database Map

## Overview

O banco da aplicacao e SQLite e fica em `pdv.db` em um diretorio persistente do usuario, atualmente `%APPDATA%/PDVChurrasco`.

A criacao de tabelas e alteracoes de schema sao centralizadas em:
- `src/main/java/br/com/churrasco/util/DatabaseConnection.java`

Modelo de evolucao atual:
- criacao de tabelas com `CREATE TABLE IF NOT EXISTS`
- ajustes simples com `ALTER TABLE ...` dentro de `try/catch`
- sem versionamento formal de schema

## PRAGMAs configurados

Na primeira conexao da aplicacao:
- `PRAGMA journal_mode=WAL`
- `PRAGMA synchronous=NORMAL`

Observacao:
- nao foi encontrado `PRAGMA foreign_keys=ON`

## Tables

### `usuarios`

Campos principais:
- `id`
- `nome`
- `senha`
- `perfil`

Uso:
- autenticacao
- autorizacao administrativa
- cadastro de usuarios na tela de configuracoes

Regras observadas:
- admin padrao `ADMIN` sem senha e criado se a tabela estiver vazia
- senha e armazenada em texto puro

## `produtos`

Campos principais:
- `id`
- `codigo`
- `nome`
- `preco_custo`
- `preco_venda`
- `unidade`
- `estoque`

Uso:
- cadastro de produtos
- busca por codigo no PDV
- controle de estoque

Regras observadas:
- `unidade` distingue itens por `KG` e `UN`
- estoque e ajustado diretamente por venda e encomenda

## `caixas`

Campos principais:
- `id`
- `usuario_id`
- `data_abertura`
- `data_fechamento`
- `saldo_inicial`
- `saldo_final`
- `saldo_informado`
- `diferenca`
- `status`

Uso:
- controle de abertura e fechamento de caixa
- vinculo das vendas ao caixa aberto

Regras observadas:
- apenas um caixa aberto e esperado por vez
- `CaixaDAO` faz tentativa de fechamento compulsorio de caixas antigos
- abertura usa `usuario_id = 1`, nao o usuario da sessao

## `vendas`

Campos principais:
- `id`
- `data_hora`
- `valor_total`
- `desconto`
- `forma_pagamento`
- `usuario_id`
- `caixa_id`

Uso:
- cabecalho de venda
- origem dos relatorios diarios e financeiros

Regras observadas:
- `valor_total` e o valor liquido, apos desconto
- `desconto` e guardado separadamente
- `usuario_id` esta sendo salvo com valor fixo `1` no fluxo atual
- `caixa_id` e associado ao caixa aberto no momento da venda

## `itens_venda`

Campos principais:
- `id`
- `venda_id`
- `produto_id`
- `quantidade`
- `valor_unitario`
- `custo_unitario`
- `total_item`

Uso:
- itens que compoem a venda
- base do custo e margem nos relatorios

Regras observadas:
- `custo_unitario` e congelado no momento da venda
- baixa de estoque e feita a partir desses itens

## `pagamentos_venda`

Campos principais:
- `id`
- `venda_id`
- `tipo`
- `valor`

Uso:
- multipagamento
- relatorios por meio de pagamento
- calculo do saldo em dinheiro do caixa

Regras observadas:
- uma venda pode ter varios pagamentos
- `forma_pagamento` em `vendas` funciona como resumo

## `configuracoes`

Campos principais:
- `chave`
- `valor`

Uso:
- configuracoes gerais da aplicacao
- dados da empresa
- impressora
- balanca
- flags de exibicao no cupom

Chaves reais observadas no codigo:
- `empresa_nome`
- `empresa_cnpj`
- `empresa_endereco`
- `empresa_telefone`
- `rodape_cupom`
- `print_cnpj`
- `print_endereco`
- `print_telefone`
- `print_datahora`
- `impressora_nome`
- `balanca_porta`
- `balanca_velocidade`

Ponto de atencao:
- `ConfigKeys` nao bate com essas chaves reais

## `encomendas`

Campos principais:
- `id`
- `nome_cliente`
- `data_retirada`
- `valor_total`
- `status`

Uso:
- cabecalho de pedidos pendentes

Regras observadas:
- compartilha o mesmo espaco de IDs com `vendas`
- `status` esperado: `PENDENTE`

## `itens_encomenda`

Campos principais:
- `id`
- `encomenda_id`
- `produto_id`
- `quantidade`
- `valor_unitario`
- `total_item`

Uso:
- itens reservados da encomenda

Regras observadas:
- ao salvar encomenda, estoque ja e baixado
- ao cancelar encomenda, estoque e restaurado
- ao converter encomenda em venda, estoque e restaurado antes da venda final baixar novamente

## `despesas`

Campos principais:
- `id`
- `descricao`
- `valor`
- `data_pagamento`
- `categoria`
- `observacao`

Uso:
- despesas operacionais
- fluxo de caixa mensal
- DRE simplificado

## Implicit Relationships

Relacionamentos observados no uso do sistema:
- `vendas.usuario_id -> usuarios.id`
- `vendas.caixa_id -> caixas.id`
- `itens_venda.venda_id -> vendas.id`
- `itens_venda.produto_id -> produtos.id`
- `pagamentos_venda.venda_id -> vendas.id`
- `caixas.usuario_id -> usuarios.id`
- `itens_encomenda.encomenda_id -> encomendas.id`
- `itens_encomenda.produto_id -> produtos.id`

Observacao:
- embora algumas FKs estejam declaradas, a efetividade depende do SQLite estar com `foreign_keys` habilitado

## ID Strategy

### Vendas e encomendas

O sistema usa a mesma estrategia de numeracao para ambos:
- `VendaDAO.getProximoIdVenda(Connection)`
- consulta `MAX(id)` sobre `vendas UNION ALL encomendas`

Impacto:
- numero do pedido e numero da venda competem pelo mesmo contador
- isso simplifica o fluxo visual do PDV, mas aumenta o acoplamento entre os dois conceitos

## Write Flows by Table

### Abrir caixa
- insere em `caixas`

### Fechar caixa
- atualiza `caixas`

### Salvar venda
- insere em `vendas`
- insere em `itens_venda`
- insere em `pagamentos_venda`
- atualiza `produtos.estoque`

### Salvar encomenda
- insere em `encomendas`
- insere em `itens_encomenda`
- atualiza `produtos.estoque`

### Cancelar encomenda
- atualiza `produtos.estoque`
- remove `itens_encomenda`
- remove `encomendas`

### Configuracoes
- `INSERT OR REPLACE` em `configuracoes`

### Cadastro de usuarios
- CRUD em `usuarios`

## Business Rules Anchored in Database

### Estoque
- venda baixa estoque
- encomenda tambem baixa estoque
- cancelamento de encomenda devolve estoque

### Caixa
- venda deve estar associada ao caixa aberto
- saldo em dinheiro do caixa depende de `pagamentos_venda` filtrados por `DINHEIRO`

### Desconto
- desconto e salvo em `vendas.desconto`
- valor liquido continua em `vendas.valor_total`

### Relatorios
- custo e margem dependem de `itens_venda.custo_unitario`

## Known Gaps

1. `usuario_id` nao representa corretamente o operador real em todos os fluxos
2. senhas em texto puro
3. sem migracoes versionadas
4. sem `foreign_keys=ON` explicito
5. uso de `CAST(... AS UNSIGNED)` em consultas de produto nao e ideal para SQLite
6. historico de caixas esta incompleto no DAO

## Safe Change Guide

Ao mudar schema ou regras que tocam o banco, revisar sempre:
- `DatabaseConnection`
- `VendaDAO`
- `CaixaDAO`
- `FinanceiroDAO`
- `RelatorioDAO`
- `ConfigDAO`
- models correspondentes

Nao alterar o significado de `valor_total`, `desconto`, `custo_unitario` ou a estrategia de IDs compartilhados entre `vendas` e `encomendas` sem revisar todos os relatorios e o fluxo do PDV.
