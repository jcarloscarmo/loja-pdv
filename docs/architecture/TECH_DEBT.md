# Tech Debt

## Purpose

Este arquivo registra a divida tecnica relevante do projeto por severidade, impacto e recomendacao de tratamento.

## Critical

### 1. Auditoria incorreta de usuario em venda e caixa

Sintoma:
- o sistema salva `usuario_id = 1` em pontos criticos

Locais observados:
- `src/main/java/br/com/churrasco/dao/VendaDAO.java`
- `src/main/java/br/com/churrasco/dao/CaixaDAO.java`

Impacto:
- historico de operacao fica incorreto
- rastreabilidade real por operador fica comprometida
- relatorios e auditorias futuras podem ficar inviaveis

Recomendacao:
- passar `Sessao.getUsuario().getId()` para o fluxo de abertura de caixa e gravacao de venda
- remover IDs fixos hardcoded

### 2. Senhas em texto puro

Sintoma:
- autenticacao por comparacao direta entre nome e senha
- admin padrao criado como `ADMIN / admin`

Locais observados:
- `DatabaseConnection`
- `UsuarioDAO`

Impacto:
- vulnerabilidade grave de seguranca
- risco operacional alto em ambiente real

Recomendacao:
- migrar para hash de senha com algoritmo adequado
- prever rotina de migracao do admin inicial

### 3. Fechamento de caixa incompleto

Sintoma:
- `imprimirFechamento()` vazio
- `exibirRelatorioFinal(...)` vazio

Local observado:
- `src/main/java/br/com/churrasco/controller/CaixaController.java`

Impacto:
- fluxo de fechamento existe mas nao fecha a experiencia completa
- comprovacao e operacao final do caixa ficam incompletas

Recomendacao:
- implementar impressao real com `ImpressoraService`
- implementar relatorio final em modal ou cupom de conferencia

### 4. Historico de caixas nao implementado no DAO

Sintoma:
- `CaixaDAO.listarHistorico()` retorna lista vazia

Impacto:
- modulo de auditoria tende a nao funcionar corretamente

Recomendacao:
- implementar consulta real com filtros por data/status
- validar compatibilidade com `HistoricoCaixasController` e `DetalheCaixaController`

## High

### 5. `PDVController` grande demais

Sintoma:
- mistura UI, carrinho, estoque, encomenda, balanca, pagamento, impressao, caixa e navegacao

Impacto:
- alto custo de manutencao
- maior risco de regressao em qualquer alteracao

Recomendacao:
- extrair services de negocio para venda, encomenda e caixa
- deixar o controller como coordenador de tela

### 6. Regras de negocio espalhadas entre controller e DAO

Sintoma:
- parte da regra esta no controller, parte no DAO e parte em utils/models

Impacto:
- comportamento real dificil de prever
- mudancas exigem leitura ampla do sistema

Recomendacao:
- consolidar regras em services de dominio

### 7. Fluxos e telas orfas

Sintoma:
- `Splash.fxml` e `SplashController` nao sao fluxo real
- `AberturaCaixa.fxml` nao e usada no fluxo atual

Impacto:
- confusao para manutencao
- entendimento incorreto do sistema por novos desenvolvedores

Recomendacao:
- decidir se volta a usar ou remover/arquivar

### 8. Classe duplicada ou mal localizada

Sintoma:
- existencia de `controller/ImpressoraService.java`

Impacto:
- risco de import errado
- confusao de arquitetura

Recomendacao:
- remover, renomear ou mover caso esteja sem uso

### 9. Integridade referencial nao garantida explicitamente no SQLite

Sintoma:
- nao foi encontrado `PRAGMA foreign_keys=ON`

Impacto:
- risco de registros inconsistentes

Recomendacao:
- habilitar `foreign_keys` em toda conexao
- validar impacto nas operacoes atuais

## Medium

### 10. Migracao de banco artesanal

Sintoma:
- uso de `ALTER TABLE` dentro de `try/catch` vazio

Impacto:
- schema pouco rastreavel
- dificil saber em que estado o banco deve estar

Recomendacao:
- criar estrategia de versionamento de schema

### 11. SQL nao idiomatico para SQLite

Sintoma:
- uso de `CAST(codigo AS UNSIGNED)`

Impacto:
- comportamento dependente de interpretacao do SQLite

Recomendacao:
- revisar ordenacao numerica de codigos

### 12. Tratamento de erro inconsistente

Sintoma:
- muitos `catch` vazios ou apenas `printStackTrace()`

Impacto:
- troubleshooting mais dificil
- erros silenciosos em producao

Recomendacao:
- padronizar logs e erros de UI

### 13. Abertura e fechamento de porta serial em loop

Sintoma:
- `BalancaService.lerPeso()` abre e fecha a porta a cada leitura

Impacto:
- possivel instabilidade/performance ruim

Recomendacao:
- avaliar conexao persistente enquanto o campo de peso estiver em foco

### 14. Acoplamento entre IDs de venda e encomenda

Sintoma:
- mesma estrategia de ID para `vendas` e `encomendas`

Impacto:
- mais complexidade no DAO
- maior cuidado em qualquer refatoracao de numeracao

Recomendacao:
- manter por enquanto, mas documentar como regra estrutural

## Low

### 15. Comentarios evolutivos e codigo historico espalhado

Sintoma:
- comentarios como `CORRECAO`, `NOVO`, `MANTIDO`

Impacto:
- polui leitura do codigo

Recomendacao:
- limpar comentarios apos estabilizacao

### 16. Preview de configuracoes incompleto

Sintoma:
- `atualizarPreview()` sem logica real

Impacto:
- UX incompleta, mas sem bloquear negocio

Recomendacao:
- completar apenas apos estabilizar fluxos core

## Recommended Debt Order

1. Auditoria correta de usuario e caixa
2. Historico e fechamento de caixa
3. Seguranca de senha
4. Service layer de venda/caixa/encomenda
5. Integridade e migracao de banco
6. Limpeza estrutural de classes e telas orfas
