package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.ledger.ItemExtratoConta;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ItemExtratoResponse(
        UUID lancamentoId,
        UUID transacaoId,
        TipoLancamento tipoLancamento,
        BigDecimal valor,
        BigDecimal valorComSinalAlgebrico,
        BigDecimal saldoAposMovimentacao,
        String descricao,
        Instant criadoEm
) {
    public static ItemExtratoResponse deDominio(ItemExtratoConta itemExtrato) {
        return new ItemExtratoResponse(
                itemExtrato.lancamentoId(),
                itemExtrato.transacaoId(),
                itemExtrato.tipoLancamento(),
                itemExtrato.valor(),
                itemExtrato.valorComSinalAlgebrico(),
                itemExtrato.saldoAposMovimentacao(),
                itemExtrato.descricao(),
                itemExtrato.criadoEm()
        );
    }
}
