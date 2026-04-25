# Refatoracao Prioridades

## Objective

Este documento define uma ordem segura de evolucao tecnica para o projeto sem quebrar os fluxos principais do PDV.

## Principle

Priorizar primeiro o que reduz risco operacional e melhora rastreabilidade. Refatoracao estrutural grande deve acontecer somente depois que os fluxos core estiverem corretos.

## Phase 1 - Estabilizacao Operacional

Objetivo:
- corrigir o que compromete auditoria, fechamento e confianca do sistema

Itens:
1. corrigir gravacao do usuario real em venda e caixa
2. implementar `CaixaDAO.listarHistorico()`
3. completar `CaixaController.imprimirFechamento()`
4. completar `CaixaController.exibirRelatorioFinal(...)`
5. validar `HistoricoCaixasController` e `DetalheCaixaController`

Saida esperada:
- caixa auditavel
- fechamento operacional completo
- historico minimamente confiavel

## Phase 2 - Seguranca e Integridade

Objetivo:
- reduzir risco tecnico e melhorar consistencia de dados

Itens:
1. habilitar `PRAGMA foreign_keys=ON`
2. revisar queries e deletes com impacto referencial
3. migrar senha para hash seguro
4. revisar usuario admin inicial

Saida esperada:
- menor chance de corrupcao logica de dados
- autenticacao menos fragil

## Phase 3 - Extracao de Servicos de Negocio

Objetivo:
- desacoplar controllers e centralizar regra

Servicos sugeridos:
- `VendaService`
- `EncomendaService`
- `CaixaService`
- possivelmente `PagamentoService`

Escopo sugerido:
- `PDVController` deve apenas coordenar tela
- transacao e regra devem sair do controller

Saida esperada:
- menor risco de regressao
- melhor testabilidade
- leitura mais clara do negocio

## Phase 4 - Consolidacao de Fluxos e Telas

Objetivo:
- eliminar confusao entre fluxo real e codigo residual

Itens:
1. decidir destino de `Splash.fxml` e `SplashController`
2. decidir destino de `AberturaCaixa.fxml`
3. revisar `controller/ImpressoraService.java`
4. revisar `EncomendaDAO` paralelo ao fluxo principal
5. alinhar `ConfigKeys` com chaves reais

Saida esperada:
- arquitetura mais coerente
- menos ambiguidade para manutencao

## Phase 5 - Evolucao de Banco

Objetivo:
- sair do modelo de migracao artesanal

Itens:
1. criar estrategia de versao de schema
2. separar criacao de schema de ajustes incrementais
3. registrar migracoes com ordem clara

Saida esperada:
- banco mais previsivel
- menor risco em upgrades do sistema

## Phase 6 - Qualidade de Codigo e Observabilidade

Objetivo:
- melhorar manutencao cotidiana

Itens:
1. padronizar tratamento de erro
2. reduzir `catch` vazio
3. melhorar logs relevantes
4. limpar comentarios evolutivos antigos
5. completar preview de configuracoes, se ainda fizer sentido

## What Not To Refactor First

Evitar como primeira mudanca:
- renumerar IDs de venda/encomenda
- trocar stack de persistencia
- reescrever todas as telas de uma vez
- refatorar `PDVController` inteiro em uma unica entrega

## Safe Delivery Strategy

Entregar em lotes pequenos:
1. caixa e auditoria
2. seguranca/autenticacao
3. extracao de service para venda
4. extracao de service para caixa
5. limpeza de fluxo residual

Cada lote deve incluir:
- validacao manual dos fluxos impactados
- revisao de banco afetado
- atualizacao da documentacao em `docs/architecture/`
