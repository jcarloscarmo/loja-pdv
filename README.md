<h1 align="center">
  🥩 PDV Churrascaria - Sistema de Gestão Desktop
</h1>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img alt="JavaFX" src="https://img.shields.io/badge/JavaFX-GUI-blue?style=for-the-badge&logo=java&logoColor=white" />
  <img alt="SQLite" src="https://img.shields.io/badge/SQLite-Database-003B57?style=for-the-badge&logo=sqlite&logoColor=white" />
  <img alt="Maven" src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" />
</p>



---

## 💻 Sobre o projeto

O **PDV Churrascaria** é uma solução Desktop robusta desenvolvida para atender as necessidades específicas de comércios que operam com venda por peso (KG) e unidade (UN).

Diferente de sistemas web tradicionais, este projeto foca na **velocidade de operação** e **integração com hardware**. O sistema foi desenhado com foco em UX (Experiência do Usuário) para ser operado quase 100% via teclado numérico, agilizando o fluxo em horários de pico.

Além da venda, o sistema oferece um controle financeiro rigoroso com gestão de sessões de caixa (Abertura, Sangria e Fechamento).

---

## ⚙️ Funcionalidades

### 🛒 Frente de Caixa (PDV)
- **Venda Híbrida:** Suporte fluido para itens pesáveis (ex: Picanha 0,500kg) e unitários (ex: Bebidas).
- **Smart Input:** Máscaras automáticas de valores e pesos (digite `1200` e o sistema entende `1,200 kg`).
- **Prévia de Valor no Peso:** Ao pesar ou digitar o peso de um item KG, o sistema exibe a previsão do valor antes de adicionar ao carrinho.
- **Numpad Only:** Atalhos de teclado (`+`, `-`, `/`, `Enter`) permitindo operação completa apenas com a mão direita.
- **Cupom Visual:** Geração de pré-visualização de recibo térmico ("Amarelinho") na tela para conferência.
- **Encomendas:** Permite reservar itens do carrinho como encomenda pendente com reserva imediata de estoque.

### 💰 Gestão Financeira
- **Controle de Sessão:** O sistema obriga a abertura de caixa com fundo de troco (suprimento).
- **Múltiplos Pagamentos:** Uma única venda pode ser paga com Dinheiro + Pix + Cartão.
- **Fechamento Cego:** O operador informa a contagem da gaveta e o sistema aponta sobras ou quebras de caixa automaticamente.
- **Fluxo de Caixa:** Visão mensal com receitas, despesas, saldo diário e acumulado.
- **Auditoria:** Estrutura de auditoria e histórico de caixas em evolução.

### 📦 Back Office
- **CRUD de Produtos:** Cadastro completo com controle de estoque e precificação.
- **Relatórios Diários:** Totalizadores por dia, formas de pagamento, descontos, custo e lucro.
- **Ranking de Produtos:** Visão por período com faturamento, custo e margem por item.
- **Configurações:** Dados da empresa, impressora, balança e usuários.

---

## 🛠 Tecnologias Utilizadas

- **Java 21 LTS:** Utilizando recursos modernos da linguagem.
- **JavaFX:** Para construção de uma interface gráfica rica, responsiva e nativa.
- **SQLite:** Banco de dados relacional embarcado (serverless), ideal para aplicações locais sem necessidade de instalação complexa.
- **JDBC:** Controle manual de transações e conexões para máxima performance.
- **Maven:** Gerenciamento de dependências e build.
- **Lombok:** Redução de boilerplate code.
- **escpos-coffee:** Impressão térmica ESC/POS.
- **jSerialComm:** Integração com porta serial para balança.

---

## 🧭 Arquitetura e Documentação

O projeto agora possui uma base de documentação técnica em `docs/architecture/` para facilitar manutenção e evolução.

Arquivos principais:

- `docs/architecture/ARCHITECTURE_MAP.md`: mapa geral da arquitetura real do sistema.
- `docs/architecture/FLOW_SEQUENCES.md`: fluxos críticos de login, PDV, encomenda, caixa e relatórios.
- `docs/architecture/DATABASE_MAP.md`: mapa do banco SQLite e regras implícitas de persistência.
- `docs/architecture/TECH_DEBT.md`: dívida técnica priorizada.
- `docs/architecture/REFATORACAO_PRIORIDADES.md`: ordem segura de refatoração.
- `docs/architecture/SCREEN_MATRIX.md`: matriz tela -> controller -> dependências.

Essa documentação foi criada para servir como referência antes de alterar regras de negócio, banco, controllers, DAOs ou integrações de hardware.

---

## 🗄️ Banco de Dados

O sistema utiliza um modelo relacional robusto para garantir a integridade financeira:

* **`caixas`**: Controla as sessões (Abertura/Fechamento, Saldo Inicial/Final).
* **`vendas`**: Cabeçalho da transação, vinculada a um Caixa e a um Usuário.
* **`itens_venda`**: Produtos, quantidades e valor histórico do momento da venda.
* **`pagamentos_venda`**: Permite N formas de pagamento para uma única venda (Normalização financeira).
* **`encomendas`** e **`itens_encomenda`**: Reservas pendentes com baixa e restauração de estoque.
* **`despesas`**: Despesas operacionais usadas no fluxo de caixa e DRE simplificado.
* **`produtos`** e **`usuarios`**: Cadastros base.
* **`configuracoes`**: Configuração de empresa, hardware e preferências do cupom.

> **Destaque:** O sistema possui um mecanismo de **Auto-Migration**. Ao iniciar, ele verifica a estrutura do banco SQLite e cria/atualiza as tabelas automaticamente se necessário.

---

## 📌 Estado Atual

O projeto esta em evolucao pratica e voltado para estudo, validacao de fluxo e melhorias incrementais.

Ja existem:

- documentacao de arquitetura persistente no repositorio
- navegacao principal estabilizada para manter janelas maximizadas
- previa de valor para produtos vendidos por peso no PDV
- integracao com impressora e balanca por configuracao local

Pontos ainda em evolucao:

- historico completo de caixas
- consolidacao de regras de negocio fora dos controllers
- endurecimento de autenticacao e auditoria

---

## 🚀 Como executar

### Pré-requisitos
* Java 21 JDK instalado.
* Maven instalado.

### Passos

```bash
# 1. Clone o repositório
$ git clone https://github.com/jcarloscarmo/loja-pdv.git

# 2. Acesse a pasta
$ cd loja-pdv

# 3. Instale as dependências
$ mvn clean install

# 4. Execute a aplicação
$ mvn javafx:run

Nota: O arquivo `pdv.db` será criado automaticamente na raiz do projeto na primeira execução.

Arquivos locais ignorados pelo Git:

- `pdv.db`
- `pdv.db-shm`
- `pdv.db-wal`
- `sistema.log`
```

## 👨‍💻 Autor

<div align="center"> <img style="border-radius: 50%;" src="https://github.com/jcarloscarmo.png" width="100px;" alt="Foto de Perfil José Carlos"/> <br /> <sub><b>José Carlos</b></sub> <br /> <br />

<a href="https://github.com/jcarloscarmo" title="GitHub"> <img src="https://img.shields.io/badge/-GitHub-black?style=flat-square&logo=github" /> </a> <a href="https://www.linkedin.com/in/jcarloscarmo" title="LinkedIn"> <img src="https://img.shields.io/badge/-LinkedIn-blue?style=flat-square&logo=linkedin" /> </a>

<br /> <br /> 👋 Feito com carinho para estudos! Entre em contato! </div>
