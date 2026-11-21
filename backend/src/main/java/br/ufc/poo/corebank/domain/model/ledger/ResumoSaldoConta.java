package br.ufc.poo.corebank.domain.model.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ResumoSaldoConta(
        UUID contaId,
        String numeroConta,
        BigDecimal saldoDisponivel,
        BigDecimal totalCreditos,
        BigDecimal totalDebitos,
        long quantidadeLancamentos,
        Instant consultadoEm
) {
}
