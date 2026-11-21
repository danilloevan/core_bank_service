package br.ufc.poo.corebank.domain.model.transacao;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.TipoOperacao;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;
import br.ufc.poo.corebank.domain.model.ledger.VerificadorPartidasDobradas;
import br.ufc.poo.corebank.domain.strategy.EstrategiaTarifa;
import br.ufc.poo.corebank.domain.validator.ValidadorTransacao;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transacoes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_operacao", discriminatorType = DiscriminatorType.STRING, length = 30)
public abstract class Transacao {

    @Id
    private UUID id;

    @Column(name = "chave_idempotencia", nullable = false, unique = true, length = 100)
    private String chaveIdempotencia;

    @Column(name = "valor_operacao", nullable = false, precision = 19, scale = 2)
    protected BigDecimal valorOperacao;

    @Column(name = "valor_tarifa", nullable = false, precision = 19, scale = 2)
    protected BigDecimal valorTarifa;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_transacao", nullable = false, length = 20)
    protected StatusTransacao statusTransacao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_origem_id")
    protected Conta contaOrigem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_destino_id")
    protected Conta contaDestino;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "transacao_id", referencedColumnName = "id", insertable = false, updatable = false)
    private List<LancamentoContabil> lancamentos = new ArrayList<>();

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(name = "processada_em")
    private Instant processadaEm;

    @Transient
    private EstrategiaTarifa estrategiaTarifa;

    protected Transacao() {
        // Construtor exigido pelo JPA/Hibernate
    }

    protected Transacao(
            String chaveIdempotencia,
            BigDecimal valorOperacaoInformado,
            Conta contaOrigem,
            Conta contaDestino,
            EstrategiaTarifa estrategiaTarifa) {
        if (chaveIdempotencia == null || chaveIdempotencia.isBlank()) {
            throw new RegraNegocioException("A chave de idempotência da transação é obrigatória.");
        }
        if (valorOperacaoInformado == null) {
            throw new RegraNegocioException("O valor da transação é obrigatório.");
        }
        BigDecimal valorNormalizado = LancamentoContabil.normalizarValorMonetario(valorOperacaoInformado);
        if (valorNormalizado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor da transação deve ser estritamente maior que zero.");
        }

        this.id = UUID.randomUUID();
        this.chaveIdempotencia = chaveIdempotencia.trim();
        this.valorOperacao = valorNormalizado;
        this.valorTarifa = LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);
        this.statusTransacao = StatusTransacao.PENDENTE;
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.estrategiaTarifa = estrategiaTarifa;
        this.criadaEm = Instant.now();
    }

    public final List<LancamentoContabil> processar(
            ContextoTransacao contextoTransacao,
            List<ValidadorTransacao> validadores) {
        if (this.statusTransacao != StatusTransacao.PENDENTE) {
            throw new RegraNegocioException("Apenas transações com status PENDENTE podem ser processadas.");
        }
        if (contextoTransacao == null) {
            throw new RegraNegocioException("O contexto de execução da transação é obrigatório.");
        }

        this.valorTarifa = (this.estrategiaTarifa != null)
                ? LancamentoContabil.normalizarValorMonetario(this.estrategiaTarifa.calcularTarifa(this.valorOperacao))
                : LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);

        if (validadores != null) {
            for (ValidadorTransacao validadorAtual : validadores) {
                validadorAtual.validar(this, contextoTransacao);
            }
        }

        validarRegrasEspecificas(contextoTransacao);

        List<LancamentoContabil> lancamentosGerados = new ArrayList<>(gerarLancamentosEspecificos(contextoTransacao));

        if (this.valorTarifa.compareTo(BigDecimal.ZERO) > 0 && this.contaOrigem != null) {
            if (contextoTransacao.getContaReceitaTarifas() == null) {
                throw new RegraNegocioException(
                        "A Conta Técnica de Receita de Tarifas é obrigatória para liquidar tarifas da operação."
                );
            }
            String descricaoTarifa = "Tarifa bancária " + obterTipoOperacao().name();
            lancamentosGerados.add(new LancamentoContabil(
                    this.id,
                    this.contaOrigem,
                    TipoLancamento.DEBITO,
                    this.valorTarifa,
                    descricaoTarifa
            ));
            lancamentosGerados.add(new LancamentoContabil(
                    this.id,
                    contextoTransacao.getContaReceitaTarifas(),
                    TipoLancamento.CREDITO,
                    this.valorTarifa,
                    "Receita de " + descricaoTarifa
            ));
        }

        VerificadorPartidasDobradas.validarEquilibrioPartidasDobradas(lancamentosGerados);

        this.lancamentos.clear();
        this.lancamentos.addAll(lancamentosGerados);
        this.statusTransacao = StatusTransacao.CONCLUIDA;
        this.processadaEm = Instant.now();

        return getLancamentos();
    }

    public BigDecimal obterValorTotalDebitadoNaOrigem() {
        if (this.contaOrigem == null) {
            return LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);
        }
        return LancamentoContabil.normalizarValorMonetario(this.valorOperacao.add(this.valorTarifa));
    }

    public abstract TipoOperacao obterTipoOperacao();

    protected abstract void validarRegrasEspecificas(ContextoTransacao contextoTransacao);

    protected abstract List<LancamentoContabil> gerarLancamentosEspecificos(ContextoTransacao contextoTransacao);

    public UUID getId() {
        return id;
    }

    public String getChaveIdempotencia() {
        return chaveIdempotencia;
    }

    public BigDecimal getValorOperacao() {
        return valorOperacao;
    }

    public BigDecimal getValorTarifa() {
        return valorTarifa;
    }

    public StatusTransacao getStatusTransacao() {
        return statusTransacao;
    }

    public Conta getContaOrigem() {
        return contaOrigem;
    }

    public Conta getContaDestino() {
        return contaDestino;
    }

    public List<LancamentoContabil> getLancamentos() {
        return Collections.unmodifiableList(lancamentos);
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getProcessadaEm() {
        return processadaEm;
    }

    public EstrategiaTarifa getEstrategiaTarifa() {
        return estrategiaTarifa;
    }
}
