# To Do

## Prioridade Imediata

- Validar o fluxo completo de promocoes no PDV com testes manuais reais.
- Cadastrar promocoes simples e mistas e confirmar que o desconto aparece corretamente no carrinho.
- Finalizar vendas com:
  - promocao sem desconto manual
  - promocao com desconto manual
  - multiplos pagamentos
  - combo repetido no mesmo carrinho
- Conferir cupom impresso e visualizacao da confirmacao para garantir que `DESC. PROMO` e `DESC. MANUAL` aparecem corretamente.
- Conferir relatorios diarios e ranking para validar totais de descontos apos vendas com promocao.

## Correcoes de Risco Alto

- Corrigir auditoria de usuario em `VendaDAO` e `CaixaDAO` para parar de salvar `usuario_id = 1`.
- Implementar `CaixaDAO.listarHistorico()` para destravar a auditoria de caixas.
- Habilitar `PRAGMA foreign_keys=ON` em `DatabaseConnection` e revisar impacto nas operacoes atuais.
- Revisar o cadastro inicial de usuario admin e a autenticacao ainda em texto puro.

## Melhorias no Modulo de Promocoes

- Definir e implementar prioridade entre promocoes conflitantes que usam os mesmos produtos.
- Adicionar validacao para impedir cadastro de promocoes duplicadas ou conflitantes na V1.
- Melhorar a UX da tela `Promocoes.fxml`:
  - destaque visual do combo montado
  - validacao de periodo invalido
  - aviso quando `preco_combo` for maior que o preco base
  - acao de ativar/inativar sem precisar editar tudo
- Adicionar filtro por status e periodo na lista de promocoes.
- Adicionar exclusao logica ou inativacao preferencial em vez de exclusao fisica, se isso fizer sentido para auditoria.
- Avaliar exibir no PDV uma area mais clara com:
  - subtotal bruto
  - desconto promocional
  - total parcial

## Evolucoes Tecnicas do Calculo

- Criar testes automatizados para `PromocaoService` cobrindo:
  - combo simples
  - combo misto
  - combo repetido
  - sobra de itens no carrinho
  - promocao inativa
  - promocao fora da vigencia
  - conflito entre promocoes
- Isolar mais regra de venda fora do `PDVController` para reduzir risco de regressao.
- Avaliar criar um `VendaService` para centralizar:
  - subtotal
  - desconto promocional
  - desconto manual
  - total final
  - persistencia da venda

## Cupom e Relatorios

- Melhorar o cupom para listar os nomes das promocoes aplicadas com mais contexto.
- Validar se o ranking de produtos deve continuar usando faturamento bruto por item ou se deve haver rateio futuro de desconto promocional por item.
- Adicionar nos relatorios visoes separadas para:
  - desconto promocional total
  - desconto manual total
  - vendas com promocao
- Avaliar uma tela futura de auditoria de promocoes aplicadas por periodo.

## Banco e Migracao

- Criar estrategia de migracao versionada do banco para parar de depender de `ALTER TABLE` com `try/catch` vazio.
- Normalizar o armazenamento de datas de promocoes para manter apenas `yyyy-MM-dd`.
- Considerar rotina de saneamento para registros antigos com datas em epoch no banco de promocoes.

## Qualidade de Codigo

- Padronizar tratamento de erro em controllers e DAOs.
- Reduzir `catch` vazio e `printStackTrace()` espalhados pelo projeto.
- Revisar imports e classes com dependencias em Lombok para facilitar build e manutencao.
- Atualizar a documentacao em `docs/architecture/` sempre que a regra de promocao mudar.

## Sugestao de Ordem

1. Validacao manual completa do modulo de promocoes.
2. Correcao de auditoria de usuario em venda e caixa.
3. Historico de caixas.
4. Testes automatizados do `PromocaoService`.
5. Tratamento de conflitos entre promocoes.
6. Melhorias visuais e operacionais da tela de promocoes.
7. Refatoracao gradual do fluxo de venda para service layer.
