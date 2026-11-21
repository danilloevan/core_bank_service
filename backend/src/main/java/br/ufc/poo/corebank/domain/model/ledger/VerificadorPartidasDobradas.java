package br.ufc.poo.corebank.domain.model.ledger;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class VerificadorPartidasDobradas {

    private static final String CODIGO_ERRO_PARTIDAS_DESBALANCEADAS = "PARTIDAS_DOBRADAS_DESBALANCEADAS";

    private VerificadorPartidasDobradas() {
        // Classe utilitária de domínio
    }

    public static void validarEquilibrioPartidasDobradas(List<LancamentoContabil> lancamentosDaTransacao) {
        if (lancamentosDaTransacao == null || lancamentosDaTransacao.size() < 2) {
            throw new RegraNegocioException(
                    "Uma transação em partidas dobradas exige pelo menos 2 lançamentos contábeis (débito e crédito).",
                    CODIGO_ERRO_PARTIDAS_DESBALANCEADAS
            );
        }

        UUID transacaoIdReferencia = lancamentosDaTransacao.get(0).getTransacaoId();
        BigDecimal totalCreditos = LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);
        BigDecimal totalDebitos = LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);

        for (LancamentoContabil lancamentoAtual : lancamentosDaTransacao) {
            if (lancamentoAtual == null) {
                throw new RegraNegocioException(
                        "A lista de partidas dobradas não pode conter lançamentos nulos.",
                        CODIGO_ERRO_PARTIDAS_DESBALANCEADAS
                );
            }
            if (!transacaoIdReferencia.equals(lancamentoAtual.getTransacaoId())) {
                throw new RegraNegocioException(
                        "Todos os lançamentos de uma partida dobrada devem pertencer à mesma transação.",
                        CODIGO_ERRO_PARTIDAS_DESBALANCEADAS
                );
            }
            if (lancamentoAtual.getTipoLancamento() == TipoLancamento.CREDITO) {
                totalCreditos = totalCreditos.add(lancamentoAtual.getValor());
            } else {
                totalDebitos = totalDebitos.add(lancamentoAtual.getValor());
            }
        }

        if (totalCreditos.compareTo(BigDecimal.ZERO) <= 0 || totalCreditos.compareTo(totalDebitos) != 0) {
            throw new RegraNegocioException(
                    "Desbalanceamento contábil detectado: total de créditos (" + totalCreditos
                            + ") diverge do total de débitos (" + totalDebitos + ").",
                    CODIGO_ERRO_PARTIDAS_DESBALANCEADAS
            );
        }
    }

    public static BigDecimal calcularSaldoDeLancamentos(List<LancamentoContabil> lancamentosDaConta) {
        if (lancamentosDaConta == null || lancamentosDaConta.isEmpty()) {
            return LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);
        }
        BigDecimal saldoAcumulado = LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);
        for (LancamentoContabil lancamentoAtual : lancamentosDaConta) {
            saldoAcumulado = saldoAcumulado.add(lancamentoAtual.obterValorComSinalAlgebrico());
        }
        return LancamentoContabil.normalizarValorMonetario(saldoAcumulado);
    }

    public static BigDecimal calcularSaldoAteDataCorte(
            List<LancamentoContabil> lancamentosDaConta,
            Instant dataCorteInclusiva) {
        if (lancamentosDaConta == null || lancamentosDaConta.isEmpty()) {
            return LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);
        }
        if (dataCorteInclusiva == null) {
            throw new RegraNegocioException("A data de corte para cálculo do saldo é obrigatória.");
        }
        List<LancamentoContabil> lancamentosAteCorte = lancamentosDaConta.stream()
                .filter(lancamento -> !lancamento.getCriadoEm().isAfter(dataCorteInclusiva))
                .toList();
        return calcularSaldoDeLancamentos(lancamentosAteCorte);
    }
}
