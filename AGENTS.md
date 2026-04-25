# Project Instructions

Quando precisar consultar documentacao de bibliotecas, APIs, configuracao, instalacao ou exemplos de uso, use o `context7` para buscar a documentacao atualizada antes de responder ou implementar.

Se a tarefa envolver uma biblioteca especifica, prefira usar o `context7` explicitamente.

Para o desenvolvimento desta aplicacao, trate o uso do `context7` como padrao sempre que a tarefa envolver dependencias externas, frameworks, plugins, ferramentas de build, configuracao de ambiente, integracoes ou mudancas guiadas por documentacao.

Antes de implementar, corrigir ou configurar algo relacionado a Java, JavaFX, Maven, SQLite, JDBC, Lombok ou bibliotecas de terceiros, consulte primeiro a documentacao atualizada via `context7`.

Quando a documentacao consultada impactar a implementacao, mencione de forma breve na resposta qual biblioteca ou tecnologia foi verificada.

Antes de alterar fluxos principais, regras de negocio, banco, controllers, DAOs ou integracoes de hardware, consulte primeiro os arquivos em `docs/architecture/` para entender a arquitetura real, os fluxos criticos e as regras implicitas do sistema.

Use `docs/architecture/ARCHITECTURE_MAP.md` como mapa principal do projeto, `docs/architecture/FLOW_SEQUENCES.md` para validar impactos em fluxos e `docs/architecture/DATABASE_MAP.md` para qualquer mudanca que toque persistencia, relatorios ou estoque.

Quando a tarefa envolver manutencao, estabilizacao, correcao estrutural ou planejamento tecnico, consulte tambem `docs/architecture/TECH_DEBT.md`, `docs/architecture/REFATORACAO_PRIORIDADES.md` e `docs/architecture/SCREEN_MATRIX.md`.
