package br.ufc.poo.corebank.domain.model.conta;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("CONTA_TECNICA")
public class ContaTecnica extends Conta {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conta_tecnica", unique = true, length = 30)
    private TipoContaTecnica tipoContaTecnica;

    protected ContaTecnica() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public ContaTecnica(Agencia agencia, TipoContaTecnica tipoContaTecnica, String numeroContaTecnica) {
        super(agencia, null, numeroContaTecnica);
        if (tipoContaTecnica == null) {
            throw new RegraNegocioException("O tipo da conta técnica interna do banco é obrigatório.");
        }
        this.tipoContaTecnica = tipoContaTecnica;
    }

    @Override
    public TipoConta obterTipoConta() {
        return TipoConta.CONTA_TECNICA;
    }

    @Override
    public boolean podeRealizarOperacao(TipoOperacao tipoOperacao) {
        // Contas técnicas servem apenas como contrapartida contábil de partidas dobradas,
        // não realizando saques ou transferências diretas de clientes.
        return false;
    }

    @Override
    public BigDecimal obterLimiteSaqueDiario() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal obterTarifaManutencaoMensal() {
        return BigDecimal.ZERO;
    }

    public TipoContaTecnica getTipoContaTecnica() {
        return tipoContaTecnica;
    }
}
