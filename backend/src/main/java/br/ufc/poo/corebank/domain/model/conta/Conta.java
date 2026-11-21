package br.ufc.poo.corebank.domain.model.conta;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Entity
@Table(name = "contas")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_conta", discriminatorType = DiscriminatorType.STRING, length = 30)
public abstract class Conta {

    public static final String CODIGO_ISPB_BANCO = "12345678";
    private static final AtomicLong SEQUENCIAL_NUMERO_CONTA = new AtomicLong(100000L);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_conta", nullable = false, unique = true, length = 20)
    protected String numeroConta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_conta", nullable = false, length = 20)
    protected StatusConta statusConta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agencia_id", nullable = false)
    private Agencia agencia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "titular_id")
    private Cliente titular;

    @Column(name = "aberta_em", nullable = false, updatable = false)
    private Instant abertaEm;

    protected Conta() {
        // Construtor exigido pelo JPA/Hibernate
    }

    protected Conta(Agencia agencia, Cliente titular, String numeroContaPersonalizado) {
        if (agencia == null) {
            throw new RegraNegocioException("Toda conta bancária deve estar vinculada a uma agência.");
        }
        this.agencia = agencia;
        this.titular = titular;
        this.numeroConta = (numeroContaPersonalizado != null && !numeroContaPersonalizado.isBlank())
                ? numeroContaPersonalizado.trim()
                : gerarNovoNumeroContaComDigito();
        this.statusConta = StatusConta.ATIVA;
        this.abertaEm = Instant.now();
    }

    public static String gerarNovoNumeroContaComDigito() {
        long proximoSequencial = SEQUENCIAL_NUMERO_CONTA.incrementAndGet();
        return formatarNumeroContaComDigito(proximoSequencial);
    }

    public static String formatarNumeroContaComDigito(long numeroBase) {
        if (numeroBase <= 0) {
            throw new RegraNegocioException("O número base da conta deve ser positivo.");
        }
        String digitosBase = String.format("%06d", numeroBase);
        int somaPonderada = 0;
        int pesoMultiplicador = 2;
        for (int indice = digitosBase.length() - 1; indice >= 0; indice--) {
            int digitoAtual = Character.getNumericValue(digitosBase.charAt(indice));
            somaPonderada += digitoAtual * pesoMultiplicador;
            pesoMultiplicador = (pesoMultiplicador == 9) ? 2 : pesoMultiplicador + 1;
        }
        int restoDivisao = somaPonderada % 11;
        int digitoVerificador = (restoDivisao == 0 || restoDivisao == 1) ? 0 : (11 - restoDivisao);
        return digitosBase + "-" + digitoVerificador;
    }

    protected void validarCompatibilidadeTitular(Cliente titularInformado, TipoConta tipoContaEsperado) {
        if (titularInformado == null) {
            throw new RegraNegocioException("O cliente titular é obrigatório para abertura de " + tipoContaEsperado + ".");
        }
        if (!titularInformado.isAtivo()) {
            throw new RegraNegocioException("Não é possível abrir conta para um cliente com cadastro inativo.");
        }
        if (!titularInformado.podeSerTitularDe(tipoContaEsperado)) {
            throw new RegraNegocioException(
                    "Cliente do tipo " + titularInformado.obterTipoCliente()
                            + " não pode ser titular de uma " + tipoContaEsperado + "."
            );
        }
    }

    public void bloquearConta() {
        if (this.statusConta == StatusConta.ENCERRADA) {
            throw new RegraNegocioException("Não é permitido bloquear uma conta já encerrada.");
        }
        this.statusConta = StatusConta.BLOQUEADA;
    }

    public void ativarConta() {
        if (this.statusConta == StatusConta.ENCERRADA) {
            throw new RegraNegocioException("Não é permitido reativar uma conta já encerrada.");
        }
        this.statusConta = StatusConta.ATIVA;
    }

    public void encerrarConta(BigDecimal saldoAtualCalculadoNoLedger) {
        if (this.statusConta == StatusConta.ENCERRADA) {
            throw new RegraNegocioException("A conta já se encontra encerrada.");
        }
        if (saldoAtualCalculadoNoLedger == null || saldoAtualCalculadoNoLedger.compareTo(BigDecimal.ZERO) != 0) {
            throw new RegraNegocioException(
                    "A conta só pode ser encerrada quando o saldo calculado no Ledger for exatamente R$ 0,00."
            );
        }
        this.statusConta = StatusConta.ENCERRADA;
    }

    public boolean isAtiva() {
        return this.statusConta == StatusConta.ATIVA;
    }

    public abstract TipoConta obterTipoConta();

    public abstract boolean podeRealizarOperacao(TipoOperacao tipoOperacao);

    public abstract BigDecimal obterLimiteSaqueDiario();

    public abstract BigDecimal obterTarifaManutencaoMensal();

    public UUID getId() {
        return id;
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public StatusConta getStatusConta() {
        return statusConta;
    }

    public Agencia getAgencia() {
        return agencia;
    }

    public Cliente getTitular() {
        return titular;
    }

    public Instant getAbertaEm() {
        return abertaEm;
    }
}
