# Core Bank Service — Mini Motor Bancário Reativo (UFC / POO)

> **Disciplina:** SMD0017 — Programação Orientada a Objetos (UFC / IUVI — 2026.1)  
> **Professor:** Gilvan Maia  
> **Stack:** Java 21 + Quarkus 3.40 + SmallRye Mutiny (`Uni`/`Multi`) + Hibernate Reactive + PostgreSQL 16

O **Core Bank Service** é um back-end bancário REST reativo construído em **Java 21** que aplica de forma rigorosa os pilares de **Programação Orientada a Objetos** (abstração, encapsulamento, herança genuína, polimorfismo, composição, agregação, associação e dependência) e **Padrões de Projeto GoF** (**Template Method**, **Strategy**, **Chain of Responsibility**, **Factory Method** e **Repository**) a um motor contábil de **Partidas Dobradas (Double-Entry Ledger)**.

---

## Entregáveis Oficiais do Trabalho Final (SOLAR)

| Entregável Exigido no Edital | Arquivo no Repositório | Descrição |
| :--- | :--- | :--- |
| **1. Arquivo da Equipe** | [`EQUIPE.txt`](EQUIPE.txt) | Componentes da equipe, papéis e mapeamento detalhado de quem implementou cada módulo. |
| **2. Diagrama de Classes UML** | [`docs/diagrama-classes.png`](docs/diagrama-classes.png)<br>[`docs/diagrama-classes.svg`](docs/diagrama-classes.svg)<br>[`docs/diagrama-classes.puml`](docs/diagrama-classes.puml)<br>[`docs/DIAGRAMA_CLASSES.md`](docs/DIAGRAMA_CLASSES.md) | Diagrama de Classes UML com 100% de fidelidade ao código-fonte entregue (em PNG de alta resolução, SVG, PlantUML e Mermaid). |
| **3. Manual de Instruções** | [`MANUAL_EXECUCAO.md`](MANUAL_EXECUCAO.md) | Passo a passo claro e objetivo para rodar os testes, iniciar o banco PostgreSQL + Quarkus e testar os endpoints via Swagger UI ou cURL. |
| **4. Auditoria da Rubrica de POO** | [`docs/AUDITORIA_CRITERIOS_POO.md`](docs/AUDITORIA_CRITERIOS_POO.md) | Mapeamento item a item (`2.a` a `2.g`) indicando classe, linha e método onde cada critério da prova é atendido. |
| **5. Código-Fonte Completo** | [`backend/src/`](backend/src/) | Código-fonte Java 21 + Quarkus Reativo organizado em camadas (`domain`, `application`, `infrastructure`, `api`) + 34 testes automatizados. |

---

## Destaques Arquiteturais e de POO

1. **Zero Coluna de Saldo Mutável (Ledger Imutável Append-Only):**
   - Nenhuma entidade `Conta` armazena atributo `BigDecimal saldo`.
   - O saldo é sempre calculado dinamicamente pela soma algébrica dos lançamentos contábeis no Livro-Razão:
     $$\text{Saldo} = \sum \text{Créditos} - \sum \text{Débitos}$$
2. **Partidas Dobradas Obrigatórias (`VerificadorPartidasDobradas`):**
   - Toda operação financeira (`TransacaoDeposito`, `TransacaoPix`, `TransacaoTed`, `TransacaoSaque`) gera lançamentos contábeis balanceados entre contas de clientes e contas técnicas internas do banco (`CAIXA_TESOURARIA`, `RECEITA_TARIFAS`, `LIQUIDACAO_EXTERNA`).
3. **4 Hierarquias de Herança Genuínas e 7 Pontos de Polimorfismo:**
   - `Cliente` $\rightarrow$ `PessoaFisica`, `PessoaJuridica`
   - `Conta` $\rightarrow$ `ContaCorrente`, `ContaEmpresarial`, `ContaTecnica`
   - `Transacao` $\rightarrow$ `TransacaoDeposito`, `TransacaoPix`, `TransacaoTed`, `TransacaoSaque`
   - `DominioException` $\rightarrow$ 8 subclasses de exceções de negócio mapeadas para HTTP (`DominioExceptionMapper`)

---

## Execução Rápida (Quickstart)

### 1. Rodar todos os 34 testes automatizados (não requer Docker):
```bash
cd backend
./mvnw test
```

### 2. Subir PostgreSQL e iniciar a API Quarkus:
```bash
cd backend
docker compose up -d
./mvnw quarkus:dev
```
Acesse o **Swagger UI** em: `http://localhost:8080/swagger-ui`

Consulte o [**Manual de Execução Completo (`MANUAL_EXECUCAO.md`)**](MANUAL_EXECUCAO.md) para exemplos detalhados de requisições cURL para todas as operações.
