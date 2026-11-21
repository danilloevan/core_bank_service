package br.ufc.poo.corebank.domain.model.ledger;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lancamentos_contabeis")
public class LancamentoContabil {

    public static final int ESCALA_MONETARIA = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transacao_id", nullable = false, updatable = false)
    private UUID transacaoId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_id", nullable = false, updatable = false)
    private Conta conta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_lancamento", nullable = false, updatable = false, length = 15)
    private TipoLancamento tipoLancamento;

    @Column(name = "valor", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "descricao", nullable = false, updatable = false, length = 255)
    private String descricao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected LancamentoContabil() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public LancamentoContabil(
            UUID transacaoId,
            Conta conta,
            TipoLancamento tipoLancamento,
            BigDecimal valorInformado,
            String descricao) {
        this(transacaoId, conta, tipoLancamento, valorInformado, descricao, Instant.now());
    }

    public LancamentoContabil(
            UUID transacaoId,
            Conta conta,
            TipoLancamento tipoLancamento,
            BigDecimal valorInformado,
            String descricao,
            Instant criadoEm) {
        validarCamposObrigatorios(transacaoId, conta, tipoLancamento, valorInformado, descricao, criadoEm);

        BigDecimal valorNormalizado = normalizarValorMonetario(valorInformado);
        if (valorNormalizado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor do lançamento contábil deve ser estritamente maior que zero.");
        }

        this.transacaoId = transacaoId;
        this.conta = conta;
        this.tipoLancamento = tipoLancamento;
        this.valor = valorNormalizado;
        this.descricao = descricao.trim();
        this.criadoEm = criadoEm;
    }

    public static BigDecimal normalizarValorMonetario(BigDecimal valorBruto) {
        if (valorBruto == null) {
            return BigDecimal.ZERO.setScale(ESCALA_MONETARIA, RoundingMode.HALF_EVEN);
        }
        return valorBruto.setScale(ESCALA_MONETARIA, RoundingMode.HALF_EVEN);
    }

    private void validarCamposObrigatorios(
            UUID transacaoIdInformado,
            Conta contaInformada,
            TipoLancamento tipoLancamentoInformado,
            BigDecimal valorInformado,
            String descricaoInformada,
            Instant criadoEmInformado) {
        if (transacaoIdInformado == null) {
            throw new RegraNegocioException("O identificador da transação do lançamento é obrigatório.");
        }
        if (contaInformada == null) {
            throw new RegraNegocioException("A conta movimentada pelo lançamento é obrigatória.");
        }
        if (tipoLancamentoInformado == null) {
            throw new RegraNegocioException("O tipo de lançamento contábil (CREDITO ou DEBITO) é obrigatório.");
        }
        if (valorInformado == null) {
            throw new RegraNegocioException("O valor do lançamento contábil é obrigatório.");
        }
        if (descricaoInformada == null || descricaoInformada.isBlank()) {
            throw new RegraNegocioException("A descrição do lançamento contábil é obrigatória.");
        }
        if (criadoEmInformado == null) {
            throw new RegraNegocioException("O instante de criação do lançamento contábil é obrigatório.");
        }
    }

    public BigDecimal obterValorComSinalAlgebrico() {
        return tipoLancamento.aplicarSinalAlgebrico(valor);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransacaoId() {
        return transacaoId;
    }

    public Conta getConta() {
        return conta;
    }

    public TipoLancamento getTipoLancamento() {
        return tipoLancamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
