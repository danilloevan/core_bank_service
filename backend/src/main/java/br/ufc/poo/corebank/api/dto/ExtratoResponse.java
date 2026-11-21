package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.ledger.ExtratoConta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExtratoResponse(
        UUID contaId,
        String codigoAgencia,
        String numeroConta,
        String nomeTitular,
        String documentoTitular,
        Instant dataInicioFiltro,
        Instant dataFimFiltro,
        BigDecimal saldoAnteriorAoPeriodo,
        BigDecimal totalEntradasCreditos,
        BigDecimal totalSaidasDebitos,
        BigDecimal saldoFinalDoPeriodo,
        int quantidadeMovimentacoes,
        Instant emitidoEm,
        List<ItemExtratoResponse> movimentacoes
) {
    public static ExtratoResponse deDominio(ExtratoConta extratoConta) {
        List<ItemExtratoResponse> itensResponse = extratoConta.getMovimentacoes().stream()
                .map(ItemExtratoResponse::deDominio)
                .toList();

        return new ExtratoResponse(
                extratoConta.getContaId(),
                extratoConta.getCodigoAgencia(),
                extratoConta.getNumeroConta(),
                extratoConta.getNomeTitular(),
                extratoConta.getDocumentoTitular(),
                extratoConta.getDataInicioFiltro(),
                extratoConta.getDataFimFiltro(),
                extratoConta.getSaldoAnteriorAoPeriodo(),
                extratoConta.getTotalEntradasCreditos(),
                extratoConta.getTotalSaidasDebitos(),
                extratoConta.getSaldoFinalDoPeriodo(),
                itensResponse.size(),
                extratoConta.getEmitidoEm(),
                itensResponse
        );
    }
}
