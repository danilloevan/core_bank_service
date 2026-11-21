package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.ledger.ResumoSaldoConta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaldoResponse(
        UUID contaId,
        String numeroConta,
        BigDecimal saldoDisponivel,
        BigDecimal totalCreditos,
        BigDecimal totalDebitos,
        long quantidadeLancamentosProcessados,
        Instant consultadoEm
) {
    public static SaldoResponse deDominio(ResumoSaldoConta resumoSaldo) {
        return new SaldoResponse(
                resumoSaldo.contaId(),
                resumoSaldo.numeroConta(),
                resumoSaldo.saldoDisponivel(),
                resumoSaldo.totalCreditos(),
                resumoSaldo.totalDebitos(),
                resumoSaldo.quantidadeLancamentos(),
                resumoSaldo.consultadoEm()
        );
    }
}
