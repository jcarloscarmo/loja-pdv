<h1 align="center">
  🛒 PDV Loja - Sistema de Ponto de Venda
</h1>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=java&logoColor=white" />
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-3.0-green?style=for-the-badge&logo=spring&logoColor=white" />
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" />
</p>

<p align="center">
 <a href="#-sobre-o-projeto">Sobre</a> •
 <a href="#-funcionalidades">Funcionalidades</a> •
 <a href="#-fluxo-da-aplicação">Fluxo</a> •
 <a href="#-tecnologias">Tecnologias</a> •
 <a href="#-como-executar">Como Executar</a> •
 <a href="#-autor">Autor</a>
</p>

---

## 💻 Sobre o projeto

O **PDV Loja** é uma API desenvolvida para simular a operação de um caixa de supermercado ou loja. O foco principal é o gerenciamento correto de valores monetários e o controle transacional de estoque.

Este projeto é meu portfólio de estudos em Java, demonstrando a aplicação prática de conceitos de Engenharia de Software e arquitetura de sistemas.

---

## ⚙️ Funcionalidades

### 📦 Estoque & Produtos
- ✅ **CRUD Completo:** Cadastro, leitura, atualização e remoção de produtos.
- 📉 **Baixa Automática:** O estoque é descontado automaticamente ao finalizar uma venda.
- 🚫 **Bloqueio de Venda:** O sistema impede vendas se o estoque for insuficiente.

### 💰 Vendas & Caixa
- 🛒 **Carrinho de Compras:** Adição dinâmica de itens.
- 🧮 **Cálculos Precisos:** Uso de `BigDecimal` para garantir que R$ 0,10 + R$ 0,20 seja exatamente R$ 0,30.
- 🧾 **Histórico:** Consulta de vendas realizadas e seus detalhes.

---

## 🔄 Fluxo da Aplicação

Como a aplicação é focada no Back-end, este é o fluxo lógico dos dados:

```mermaid
%% Exemplo visual do fluxo (O GitHub renderiza isso nativamente)
graph LR
    A[👤 Cliente] -->|Escolhe Itens| B(🛒 Carrinho)
    B -->|Checkout| C{🏧 Sistema PDV}
    C -->|Verifica Estoque| D[(🗄️ Banco de Dados)]
    D -->|Estoque OK?| C
    C -->|Confirma Venda| E[🧾 Nota Fiscal Gerada]
    C -->|Atualiza Saldo| D
