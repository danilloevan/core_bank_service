package br.ufc.poo.corebank.domain.strategy;

import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;

import java.math.BigDecimal;

public class TarifaIsentaStrategy implements EstrategiaTarifa {

    @Override
    public BigDecimal calcularTarifa(BigDecimal valorOperacao) {
        return LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);
    }

    @Override
    public String obterDescricaoEstrategia() {
        return "ISENTA";
    }
}
