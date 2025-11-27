<h1 align="center">
  🥩 PDV Churrascaria - Sistema de Gestão Desktop
</h1>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img alt="JavaFX" src="https://img.shields.io/badge/JavaFX-GUI-blue?style=for-the-badge&logo=java&logoColor=white" />
  <img alt="SQLite" src="https://img.shields.io/badge/SQLite-Database-003B57?style=for-the-badge&logo=sqlite&logoColor=white" />
  <img alt="Maven" src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" />
</p>

<p align="center">
  <a href="#-sobre-o-projeto">Sobre</a> •
  <a href="#-funcionalidades">Funcionalidades</a> •
  <a href="#-tecnologias">Tecnologias</a> •
  <a href="#-banco-de-dados">Estrutura de Dados</a> •
  <a href="#-autor">Autor</a>
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
- **Numpad Only:** Atalhos de teclado (`+`, `-`, `/`, `Enter`) permitindo operação completa apenas com a mão direita.
- **Cupom Visual:** Geração de pré-visualização de recibo térmico ("Amarelinho") na tela para conferência.

### 💰 Gestão Financeira
- **Controle de Sessão:** O sistema obriga a abertura de caixa com fundo de troco (suprimento).
- **Múltiplos Pagamentos:** Uma única venda pode ser paga com Dinheiro + Pix + Cartão.
- **Fechamento Cego:** O operador informa a contagem da gaveta e o sistema aponta sobras ou quebras de caixa automaticamente.
- **Auditoria:** Histórico completo de fechamentos passados e detalhamento de vendas.

### 📦 Back Office
- **CRUD de Produtos:** Cadastro completo com controle de estoque e precificação.
- **Relatórios:** Dashboard com totalizadores do dia (faturamento bruto e por tipo de pagamento).

---

## 🛠 Tecnologias Utilizadas

- **Java 21 LTS:** Utilizando recursos modernos da linguagem.
- **JavaFX:** Para construção de uma interface gráfica rica, responsiva e nativa.
- **SQLite:** Banco de dados relacional embarcado (serverless), ideal para aplicações locais sem necessidade de instalação complexa.
- **JDBC:** Controle manual de transações e conexões para máxima performance.
- **Maven:** Gerenciamento de dependências e build.
- **Lombok:** Redução de boilerplate code.

---

## 🗄️ Banco de Dados

O sistema utiliza um modelo relacional robusto para garantir a integridade financeira:

* **`caixas`**: Controla as sessões (Abertura/Fechamento, Saldo Inicial/Final).
* **`vendas`**: Cabeçalho da transação, vinculada a um Caixa e a um Usuário.
* **`itens_venda`**: Produtos, quantidades e valor histórico do momento da venda.
* **`pagamentos_venda`**: Permite N formas de pagamento para uma única venda (Normalização financeira).
* **`produtos`** e **`usuarios`**: Cadastros base.

> **Destaque:** O sistema possui um mecanismo de **Auto-Migration**. Ao iniciar, ele verifica a estrutura do banco SQLite e cria/atualiza as tabelas automaticamente se necessário.

---

## 🚀 Como executar

### Pré-requisitos
* Java 21 JDK instalado.
* Maven instalado.

### Passos

```bash
# 1. Clone o repositório
$ git clone [https://github.com/jcarloscarmo/pdv-churrascaria.git](https://github.com/jcarloscarmo/pdv-churrascaria.git)

# 2. Acesse a pasta
$ cd pdv-churrascaria

# 3. Instale as dependências
$ mvn clean install

# 4. Execute a aplicação
$ mvn javafx:run

Nota: O arquivo pdv.db será criado automaticamente na raiz do projeto na primeira execução.

## 👨‍💻 Autor

<div align="center"> <img style="border-radius: 50%;" src="https://github.com/jcarloscarmo.png" width="100px;" alt="Foto de Perfil José Carlos"/> <br /> <sub><b>José Carlos</b></sub> <br /> <br />

<a href="https://github.com/jcarloscarmo" title="GitHub"> <img src="https://img.shields.io/badge/-GitHub-black?style=flat-square&logo=github" /> </a> <a href="https://www.linkedin.com/in/jcarloscarmo" title="LinkedIn"> <img src="https://img.shields.io/badge/-LinkedIn-blue?style=flat-square&logo=linkedin" /> </a>

<br /> <br /> 👋 Feito com carinho para estudos! Entre em contato! </div>
