package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.conta.TipoOperacao;
import br.ufc.poo.corebank.domain.model.transacao.StatusTransacao;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoDeposito;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoPix;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoSaque;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoTed;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransacaoResponse(
        UUID id,
        String chaveIdempotencia,
        TipoOperacao tipoOperacao,
        StatusTransacao statusTransacao,
        BigDecimal valorOperacao,
        BigDecimal valorTarifa,
        BigDecimal valorTotalDebitadoOrigem,
        UUID contaOrigemId,
        String numeroContaOrigem,
        UUID contaDestinoId,
        String numeroContaDestino,
        String endToEndIdPix,
        String chavePixDestino,
        String codigoTerminalAtm,
        String canalOrigemDeposito,
        Instant criadaEm,
        Instant processadaEm,
        List<LancamentoContabilResponse> lancamentosPartidasDobradas
) {
    public static TransacaoResponse deDominio(Transacao transacao) {
        UUID contaOrigemId = transacao.getContaOrigem() != null ? transacao.getContaOrigem().getId() : null;
        String numeroContaOrigem = transacao.getContaOrigem() != null ? transacao.getContaOrigem().getNumeroConta() : null;
        UUID contaDestinoId = transacao.getContaDestino() != null ? transacao.getContaDestino().getId() : null;
        String numeroContaDestino = transacao.getContaDestino() != null ? transacao.getContaDestino().getNumeroConta() : null;

        String endToEndIdPix = null;
        String chavePixDestino = null;
        String codigoTerminalAtm = null;
        String canalOrigemDeposito = null;

        if (transacao instanceof TransacaoPix transacaoPix) {
            endToEndIdPix = transacaoPix.getEndToEndId();
            chavePixDestino = transacaoPix.getChavePixDestino();
        } else if (transacao instanceof TransacaoSaque transacaoSaque) {
            codigoTerminalAtm = transacaoSaque.getCodigoTerminalAtm();
        } else if (transacao instanceof TransacaoDeposito transacaoDeposito) {
            canalOrigemDeposito = transacaoDeposito.getCanalOrigemDeposito();
        } else if (transacao instanceof TransacaoTed transacaoTed && numeroContaDestino == null) {
            numeroContaDestino = transacaoTed.getCodigoBancoDestino()
                    + "/" + transacaoTed.getCodigoAgenciaDestino()
                    + "/" + transacaoTed.getNumeroContaDestino();
        }

        List<LancamentoContabilResponse> lancamentosResponse = transacao.getLancamentos().stream()
                .map(LancamentoContabilResponse::deDominio)
                .toList();

        return new TransacaoResponse(
                transacao.getId(),
                transacao.getChaveIdempotencia(),
                transacao.obterTipoOperacao(),
                transacao.getStatusTransacao(),
                transacao.getValorOperacao(),
                transacao.getValorTarifa(),
                transacao.obterValorTotalDebitadoNaOrigem(),
                contaOrigemId,
                numeroContaOrigem,
                contaDestinoId,
                numeroContaDestino,
                endToEndIdPix,
                chavePixDestino,
                codigoTerminalAtm,
                canalOrigemDeposito,
                transacao.getCriadaEm(),
                transacao.getProcessadaEm(),
                lancamentosResponse
        );
    }
}
