package br.ufc.poo.corebank.domain.model.conta;

import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("CONTA_CORRENTE")
public class ContaCorrente extends Conta {

    public static final BigDecimal LIMITE_SAQUE_DIARIO_PF = new BigDecimal("2000.00");
    public static final BigDecimal TARIFA_MANUTENCAO_MENSAL_PF = new BigDecimal("15.00");

    protected ContaCorrente() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public ContaCorrente(Agencia agencia, Cliente titularPessoaFisica) {
        this(agencia, titularPessoaFisica, null);
    }

    public ContaCorrente(Agencia agencia, Cliente titularPessoaFisica, String numeroContaPersonalizado) {
        super(agencia, titularPessoaFisica, numeroContaPersonalizado);
        validarCompatibilidadeTitular(titularPessoaFisica, TipoConta.CONTA_CORRENTE);
    }

    @Override
    public TipoConta obterTipoConta() {
        return TipoConta.CONTA_CORRENTE;
    }

    @Override
    public boolean podeRealizarOperacao(TipoOperacao tipoOperacao) {
        if (!isAtiva() || tipoOperacao == null) {
            return false;
        }
        return true;
    }

    @Override
    public BigDecimal obterLimiteSaqueDiario() {
        return LIMITE_SAQUE_DIARIO_PF;
    }

    @Override
    public BigDecimal obterTarifaManutencaoMensal() {
        return TARIFA_MANUTENCAO_MENSAL_PF;
    }
}
