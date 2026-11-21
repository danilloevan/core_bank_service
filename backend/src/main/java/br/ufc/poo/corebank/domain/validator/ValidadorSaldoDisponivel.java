package br.ufc.poo.corebank.domain.validator;

import br.ufc.poo.corebank.domain.exception.SaldoInsuficienteException;
import br.ufc.poo.corebank.domain.model.transacao.ContextoTransacao;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;

import java.math.BigDecimal;

public class ValidadorSaldoDisponivel implements ValidadorTransacao {

    @Override
    public void validar(Transacao transacao, ContextoTransacao contextoTransacao) {
        if (transacao.getContaOrigem() == null) {
            return;
        }

        BigDecimal valorTotalExigidoNaOrigem = transacao.obterValorTotalDebitadoNaOrigem();
        BigDecimal saldoDisponivelNoLedger = contextoTransacao.getSaldoDisponivelContaOrigem();

        if (saldoDisponivelNoLedger.compareTo(valorTotalExigidoNaOrigem) < 0) {
            throw new SaldoInsuficienteException(
                    "Saldo insuficiente no Ledger para a conta " + transacao.getContaOrigem().getNumeroConta()
                            + ". Saldo disponível: R$ " + saldoDisponivelNoLedger
                            + " | Valor exigido (operação + tarifa): R$ " + valorTotalExigidoNaOrigem + "."
            );
        }
    }
}
