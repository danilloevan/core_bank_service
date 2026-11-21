package br.ufc.poo.corebank.domain.model.transacao;

import br.ufc.poo.corebank.domain.model.conta.ContaTecnica;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ContextoTransacao {

    public static final ZoneId FUSO_HORARIO_BANCARIO_PADRAO = ZoneId.of("America/Fortaleza");

    private final BigDecimal saldoDisponivelContaOrigem;
    private final BigDecimal totalMovimentadoNoDiaNaOperacao;
    private final ContaTecnica contaCaixaTesouraria;
    private final ContaTecnica contaReceitaTarifas;
    private final ContaTecnica contaLiquidacaoExterna;
    private final ZonedDateTime dataHoraReferencia;
    private final boolean validarJanelaHorarioTed;

    public ContextoTransacao(
            BigDecimal saldoDisponivelContaOrigem,
            BigDecimal totalMovimentadoNoDiaNaOperacao,
            ContaTecnica contaCaixaTesouraria,
            ContaTecnica contaReceitaTarifas,
            ContaTecnica contaLiquidacaoExterna,
            ZonedDateTime dataHoraReferencia,
            boolean validarJanelaHorarioTed) {
        this.saldoDisponivelContaOrigem = LancamentoContabil.normalizarValorMonetario(saldoDisponivelContaOrigem);
        this.totalMovimentadoNoDiaNaOperacao = LancamentoContabil.normalizarValorMonetario(totalMovimentadoNoDiaNaOperacao);
        this.contaCaixaTesouraria = contaCaixaTesouraria;
        this.contaReceitaTarifas = contaReceitaTarifas;
        this.contaLiquidacaoExterna = contaLiquidacaoExterna;
        this.dataHoraReferencia = dataHoraReferencia != null
                ? dataHoraReferencia
                : ZonedDateTime.now(FUSO_HORARIO_BANCARIO_PADRAO);
        this.validarJanelaHorarioTed = validarJanelaHorarioTed;
    }

    public BigDecimal getSaldoDisponivelContaOrigem() {
        return saldoDisponivelContaOrigem;
    }

    public BigDecimal getTotalMovimentadoNoDiaNaOperacao() {
        return totalMovimentadoNoDiaNaOperacao;
    }

    public ContaTecnica getContaCaixaTesouraria() {
        return contaCaixaTesouraria;
    }

    public ContaTecnica getContaReceitaTarifas() {
        return contaReceitaTarifas;
    }

    public ContaTecnica getContaLiquidacaoExterna() {
        return contaLiquidacaoExterna;
    }

    public ZonedDateTime getDataHoraReferencia() {
        return dataHoraReferencia;
    }

    public boolean isValidarJanelaHorarioTed() {
        return validarJanelaHorarioTed;
    }
}
