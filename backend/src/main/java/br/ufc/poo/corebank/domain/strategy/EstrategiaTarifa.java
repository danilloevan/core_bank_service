package br.ufc.poo.corebank.domain.strategy;

import java.math.BigDecimal;

public interface EstrategiaTarifa {

    BigDecimal calcularTarifa(BigDecimal valorOperacao);

    String obterDescricaoEstrategia();
}
