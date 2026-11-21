package br.ufc.poo.corebank.domain.model.conta;

import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("CONTA_EMPRESARIAL")
public class ContaEmpresarial extends Conta {

    public static final BigDecimal LIMITE_SAQUE_DIARIO_PJ = new BigDecimal("10000.00");
    public static final BigDecimal TARIFA_MANUTENCAO_MENSAL_PJ = new BigDecimal("49.90");

    protected ContaEmpresarial() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public ContaEmpresarial(Agencia agencia, Cliente titularPessoaJuridica) {
        this(agencia, titularPessoaJuridica, null);
    }

    public ContaEmpresarial(Agencia agencia, Cliente titularPessoaJuridica, String numeroContaPersonalizado) {
        super(agencia, titularPessoaJuridica, numeroContaPersonalizado);
        validarCompatibilidadeTitular(titularPessoaJuridica, TipoConta.CONTA_EMPRESARIAL);
    }

    @Override
    public TipoConta obterTipoConta() {
        return TipoConta.CONTA_EMPRESARIAL;
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
        return LIMITE_SAQUE_DIARIO_PJ;
    }

    @Override
    public BigDecimal obterTarifaManutencaoMensal() {
        return TARIFA_MANUTENCAO_MENSAL_PJ;
    }
}
