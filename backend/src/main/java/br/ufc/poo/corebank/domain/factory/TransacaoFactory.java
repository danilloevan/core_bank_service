package br.ufc.poo.corebank.domain.factory;

import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoDeposito;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoPix;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoSaque;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoTed;
import br.ufc.poo.corebank.domain.strategy.EstrategiaTarifa;
import br.ufc.poo.corebank.domain.strategy.TarifaFixaStrategy;
import br.ufc.poo.corebank.domain.strategy.TarifaIsentaStrategy;
import br.ufc.poo.corebank.domain.strategy.TarifaPercentualStrategy;
import br.ufc.poo.corebank.domain.validator.ValidadorJanelaHorarioTed;
import br.ufc.poo.corebank.domain.validator.ValidadorLimiteDiario;
import br.ufc.poo.corebank.domain.validator.ValidadorSaldoDisponivel;
import br.ufc.poo.corebank.domain.validator.ValidadorStatusConta;
import br.ufc.poo.corebank.domain.validator.ValidadorTransacao;

import java.math.BigDecimal;
import java.util.List;

public final class TransacaoFactory {

    public static final BigDecimal PERCENTUAL_TARIFA_PIX_PJ = new BigDecimal("0.50");
    public static final BigDecimal TARIFA_MINIMA_PIX_PJ = new BigDecimal("1.50");
    public static final BigDecimal TARIFA_MAXIMA_PIX_PJ = new BigDecimal("15.00");

    private TransacaoFactory() {
        // Fábrica estática de domínio
    }

    public static TransacaoDeposito criarDeposito(
            String chaveIdempotencia,
            Conta contaDestino,
            BigDecimal valorDeposito,
            String canalOrigemDeposito) {
        return new TransacaoDeposito(
                chaveIdempotencia,
                contaDestino,
                valorDeposito,
                canalOrigemDeposito,
                new TarifaIsentaStrategy()
        );
    }

    public static TransacaoPix criarPix(
            String chaveIdempotencia,
            Conta contaOrigem,
            Conta contaDestinoOpcional,
            BigDecimal valorPix,
            String chavePixDestino,
            String descricaoPix) {
        EstrategiaTarifa estrategiaTarifaPix = selecionarEstrategiaTarifaPix(contaOrigem);
        return new TransacaoPix(
                chaveIdempotencia,
                contaOrigem,
                contaDestinoOpcional,
                valorPix,
                chavePixDestino,
                descricaoPix,
                estrategiaTarifaPix
        );
    }

    public static TransacaoTed criarTed(
            String chaveIdempotencia,
            Conta contaOrigem,
            Conta contaDestinoInternaOpcional,
            BigDecimal valorTed,
            String codigoBancoDestino,
            String codigoAgenciaDestino,
            String numeroContaDestino,
            String documentoFavorecido) {
        EstrategiaTarifa estrategiaTarifaTed = new TarifaFixaStrategy(TransacaoTed.TARIFA_FIXA_TED_PADRAO);
        return new TransacaoTed(
                chaveIdempotencia,
                contaOrigem,
                contaDestinoInternaOpcional,
                valorTed,
                codigoBancoDestino,
                codigoAgenciaDestino,
                numeroContaDestino,
                documentoFavorecido,
                estrategiaTarifaTed
        );
    }

    public static TransacaoSaque criarSaque(
            String chaveIdempotencia,
            Conta contaOrigem,
            BigDecimal valorSaque,
            String codigoTerminalAtm) {
        return new TransacaoSaque(
                chaveIdempotencia,
                contaOrigem,
                valorSaque,
                codigoTerminalAtm,
                new TarifaIsentaStrategy()
        );
    }

    public static List<ValidadorTransacao> criarCadeiaValidadoresPadrao() {
        return List.of(
                new ValidadorStatusConta(),
                new ValidadorSaldoDisponivel(),
                new ValidadorLimiteDiario(),
                new ValidadorJanelaHorarioTed()
        );
    }

    private static EstrategiaTarifa selecionarEstrategiaTarifaPix(Conta contaOrigem) {
        Cliente titularOrigem = contaOrigem != null ? contaOrigem.getTitular() : null;
        if (titularOrigem != null && titularOrigem.isIsentoTarifaPix()) {
            return new TarifaIsentaStrategy();
        }
        return new TarifaPercentualStrategy(
                PERCENTUAL_TARIFA_PIX_PJ,
                TARIFA_MINIMA_PIX_PJ,
                TARIFA_MAXIMA_PIX_PJ
        );
    }
}
