package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LancamentoContabilResponse(
        UUID id,
        UUID transacaoId,
        UUID contaId,
        String numeroConta,
        TipoLancamento tipoLancamento,
        BigDecimal valor,
        String descricao,
        Instant criadoEm
) {
    public static LancamentoContabilResponse deDominio(LancamentoContabil lancamentoContabil) {
        return new LancamentoContabilResponse(
                lancamentoContabil.getId(),
                lancamentoContabil.getTransacaoId(),
                lancamentoContabil.getConta().getId(),
                lancamentoContabil.getConta().getNumeroConta(),
                lancamentoContabil.getTipoLancamento(),
                lancamentoContabil.getValor(),
                lancamentoContabil.getDescricao(),
                lancamentoContabil.getCriadoEm()
        );
    }
}
