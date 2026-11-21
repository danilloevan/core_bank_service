package br.ufc.poo.corebank.domain.model.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ItemExtratoConta(
        UUID lancamentoId,
        UUID transacaoId,
        TipoLancamento tipoLancamento,
        BigDecimal valor,
        BigDecimal valorComSinalAlgebrico,
        BigDecimal saldoAposMovimentacao,
        String descricao,
        Instant criadoEm
) {
}
