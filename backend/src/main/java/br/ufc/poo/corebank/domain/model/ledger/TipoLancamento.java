package br.ufc.poo.corebank.domain.model.ledger;

import java.math.BigDecimal;

public enum TipoLancamento {
    CREDITO,
    DEBITO;

    public BigDecimal aplicarSinalAlgebrico(BigDecimal valorAbsoluto) {
        if (valorAbsoluto == null) {
            return BigDecimal.ZERO;
        }
        return this == CREDITO ? valorAbsoluto : valorAbsoluto.negate();
    }
}
