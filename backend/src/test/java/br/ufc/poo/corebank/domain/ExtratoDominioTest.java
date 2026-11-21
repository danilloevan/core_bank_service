package br.ufc.poo.corebank.domain;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.export.ExportadorExtrato;
import br.ufc.poo.corebank.domain.export.ExportadorExtratoCsv;
import br.ufc.poo.corebank.domain.export.ExportadorExtratoTexto;
import br.ufc.poo.corebank.domain.model.cliente.Endereco;
import br.ufc.poo.corebank.domain.model.cliente.PessoaFisica;
import br.ufc.poo.corebank.domain.model.conta.Agencia;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.ContaCorrente;
import br.ufc.poo.corebank.domain.model.ledger.ExtratoConta;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtratoDominioTest {

    private Conta criarContaPadrao() {
        Agencia agencia = new Agencia(Agencia.CODIGO_AGENCIA_MATRIZ, Agencia.NOME_AGENCIA_MATRIZ);
        Endereco endereco = new Endereco(
                "Rua Silva Paulet",
                "800",
                "Apto 301",
                "Meireles",
                "Fortaleza",
                "CE",
                "60120-020"
        );
        PessoaFisica titular = new PessoaFisica(
                "Fernanda Lima",
                "529.982.247-25",
                LocalDate.of(1996, 11, 5),
                "fernanda.lima@ufc.br",
                "85988771122",
                endereco
        );
        return new ContaCorrente(agencia, titular, "100500-7");
    }

    @Test
    void deveConstruirExtratoComSaldoAnteriorESaldoProgressivoPorLancamento() {
        Conta conta = criarContaPadrao();
        Instant inicioPeriodo = Instant.parse("2026-11-15T00:00:00Z");
        Instant fimPeriodo = Instant.parse("2026-11-20T23:59:59Z");

        BigDecimal saldoAnterior = new BigDecimal("500.00");

        List<LancamentoContabil> lancamentosDoPeriodo = List.of(
                new LancamentoContabil(
                        UUID.randomUUID(),
                        conta,
                        TipoLancamento.CREDITO,
                        new BigDecimal("1200.00"),
                        "Depósito recebido",
                        Instant.parse("2026-11-16T10:00:00Z")
                ),
                new LancamentoContabil(
                        UUID.randomUUID(),
                        conta,
                        TipoLancamento.DEBITO,
                        new BigDecimal("350.00"),
                        "PIX enviado",
                        Instant.parse("2026-11-17T15:30:00Z")
                ),
                new LancamentoContabil(
                        UUID.randomUUID(),
                        conta,
                        TipoLancamento.DEBITO,
                        new BigDecimal("100.00"),
                        "Saque ATM",
                        Instant.parse("2026-11-18T18:00:00Z")
                )
        );

        ExtratoConta extrato = ExtratoConta.construirExtrato(
                conta,
                inicioPeriodo,
                fimPeriodo,
                saldoAnterior,
                lancamentosDoPeriodo
        );

        assertEquals(new BigDecimal("500.00"), extrato.getSaldoAnteriorAoPeriodo());
        assertEquals(new BigDecimal("1200.00"), extrato.getTotalEntradasCreditos());
        assertEquals(new BigDecimal("450.00"), extrato.getTotalSaidasDebitos());
        assertEquals(new BigDecimal("1250.00"), extrato.getSaldoFinalDoPeriodo());
        assertEquals(3, extrato.getMovimentacoes().size());

        // Saldo progressivo item a item: 500 + 1200 = 1700 -> 1700 - 350 = 1350 -> 1350 - 100 = 1250
        assertEquals(new BigDecimal("1700.00"), extrato.getMovimentacoes().get(0).saldoAposMovimentacao());
        assertEquals(new BigDecimal("1350.00"), extrato.getMovimentacoes().get(1).saldoAposMovimentacao());
        assertEquals(new BigDecimal("1250.00"), extrato.getMovimentacoes().get(2).saldoAposMovimentacao());
    }

    @Test
    void deveExportarExtratoPolimorficamenteEmCsvETexto() {
        Conta conta = criarContaPadrao();
        List<LancamentoContabil> lancamentos = List.of(
                new LancamentoContabil(
                        UUID.randomUUID(),
                        conta,
                        TipoLancamento.CREDITO,
                        new BigDecimal("800.00"),
                        "PIX recebido cliente",
                        Instant.parse("2026-11-19T12:00:00Z")
                )
        );

        ExtratoConta extrato = ExtratoConta.construirExtrato(
                conta,
                null,
                null,
                BigDecimal.ZERO,
                lancamentos
        );

        List<ExportadorExtrato> exportadoresPolimorficos = List.of(
                new ExportadorExtratoCsv(),
                new ExportadorExtratoTexto()
        );

        String saidaCsv = exportadoresPolimorficos.get(0).exportar(extrato);
        String saidaTexto = exportadoresPolimorficos.get(1).exportar(extrato);

        assertEquals("csv", exportadoresPolimorficos.get(0).obterExtensaoArquivo());
        assertTrue(saidaCsv.contains("data_hora;transacao_id;tipo_lancamento;descricao;valor;valor_com_sinal;saldo_apos_movimentacao"));
        assertTrue(saidaCsv.contains("PIX recebido cliente;800.00;800.00;800.00"));

        assertEquals("txt", exportadoresPolimorficos.get(1).obterExtensaoArquivo());
        assertTrue(saidaTexto.contains("CORE BANK SERVICE - EXTRATO BANCARIO"));
        assertTrue(saidaTexto.contains("SALDO FINAL       : R$ 800.00"));
    }

    @Test
    void deveRejeitarFiltroDePeriodoComDataInicialPosteriorADataFinal() {
        Conta conta = criarContaPadrao();
        Instant dataInicialMaior = Instant.parse("2026-11-25T00:00:00Z");
        Instant dataFinalMenor = Instant.parse("2026-11-20T00:00:00Z");

        assertThrows(RegraNegocioException.class, () ->
                ExtratoConta.construirExtrato(conta, dataInicialMaior, dataFinalMenor, BigDecimal.ZERO, List.of())
        );
    }
}
