package br.ufc.poo.corebank.domain.strategy;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TarifaPercentualStrategy implements EstrategiaTarifa {

    private static final BigDecimal DIVISOR_CEM_PORCENTO = new BigDecimal("100.00");

    private final BigDecimal percentualTaxa;
    private final BigDecimal tarifaMinima;
    private final BigDecimal tarifaMaximaTeto;

    public TarifaPercentualStrategy(
            BigDecimal percentualTaxa,
            BigDecimal tarifaMinima,
            BigDecimal tarifaMaximaTeto) {
        if (percentualTaxa == null || percentualTaxa.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O percentual da tarifa deve ser estritamente positivo.");
        }
        if (tarifaMinima == null || tarifaMinima.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraNegocioException("A tarifa mínima não pode ser negativa.");
        }
        if (tarifaMaximaTeto == null || tarifaMaximaTeto.compareTo(tarifaMinima) < 0) {
            throw new RegraNegocioException("O teto máximo da tarifa deve ser maior ou igual à tarifa mínima.");
        }
        this.percentualTaxa = percentualTaxa;
        this.tarifaMinima = LancamentoContabil.normalizarValorMonetario(tarifaMinima);
        this.tarifaMaximaTeto = LancamentoContabil.normalizarValorMonetario(tarifaMaximaTeto);
    }

    @Override
    public BigDecimal calcularTarifa(BigDecimal valorOperacao) {
        if (valorOperacao == null || valorOperacao.compareTo(BigDecimal.ZERO) <= 0) {
            return LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);
        }
        BigDecimal tarifaCalculadaBruta = valorOperacao
                .multiply(percentualTaxa)
                .divide(DIVISOR_CEM_PORCENTO, LancamentoContabil.ESCALA_MONETARIA, RoundingMode.HALF_EVEN);

        if (tarifaCalculadaBruta.compareTo(tarifaMinima) < 0) {
            return tarifaMinima;
        }
        if (tarifaCalculadaBruta.compareTo(tarifaMaximaTeto) > 0) {
            return tarifaMaximaTeto;
        }
        return LancamentoContabil.normalizarValorMonetario(tarifaCalculadaBruta);
    }

    @Override
    public String obterDescricaoEstrategia() {
        return "TARIFA_PERCENTUAL (" + percentualTaxa + "% | min=" + tarifaMinima + " | max=" + tarifaMaximaTeto + ")";
    }
}
