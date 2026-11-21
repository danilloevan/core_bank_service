package br.ufc.poo.corebank.domain.strategy;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;

import java.math.BigDecimal;

public class TarifaFixaStrategy implements EstrategiaTarifa {

    private final BigDecimal valorTarifaFixa;

    public TarifaFixaStrategy(BigDecimal valorTarifaFixa) {
        if (valorTarifaFixa == null || valorTarifaFixa.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraNegocioException("O valor da tarifa fixa não pode ser nulo nem negativo.");
        }
        this.valorTarifaFixa = LancamentoContabil.normalizarValorMonetario(valorTarifaFixa);
    }

    @Override
    public BigDecimal calcularTarifa(BigDecimal valorOperacao) {
        return valorTarifaFixa;
    }

    @Override
    public String obterDescricaoEstrategia() {
        return "TARIFA_FIXA (" + valorTarifaFixa + ")";
    }

    public BigDecimal getValorTarifaFixa() {
        return valorTarifaFixa;
    }
}
