package br.ufc.poo.corebank.domain;

import br.ufc.poo.corebank.domain.exception.ContaInaptaParaOperacaoException;
import br.ufc.poo.corebank.domain.exception.JanelaHorarioInvalidaException;
import br.ufc.poo.corebank.domain.exception.LimiteOperacionalExcedidoException;
import br.ufc.poo.corebank.domain.exception.SaldoInsuficienteException;
import br.ufc.poo.corebank.domain.factory.TransacaoFactory;
import br.ufc.poo.corebank.domain.model.cliente.Endereco;
import br.ufc.poo.corebank.domain.model.cliente.PessoaFisica;
import br.ufc.poo.corebank.domain.model.cliente.PessoaJuridica;
import br.ufc.poo.corebank.domain.model.conta.Agencia;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.ContaCorrente;
import br.ufc.poo.corebank.domain.model.conta.ContaEmpresarial;
import br.ufc.poo.corebank.domain.model.conta.ContaTecnica;
import br.ufc.poo.corebank.domain.model.conta.TipoContaTecnica;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.transacao.ContextoTransacao;
import br.ufc.poo.corebank.domain.model.transacao.StatusTransacao;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoDeposito;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoPix;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoSaque;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoTed;
import br.ufc.poo.corebank.domain.validator.ValidadorTransacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransacaoDominioTest {

    private final List<ValidadorTransacao> validadoresPadrao = TransacaoFactory.criarCadeiaValidadoresPadrao();

    private Agencia criarAgenciaPadrao() {
        return new Agencia(Agencia.CODIGO_AGENCIA_MATRIZ, Agencia.NOME_AGENCIA_MATRIZ);
    }

    private Endereco criarEnderecoPadrao() {
        return new Endereco(
                "Avenida Santos Dumont",
                "3000",
                "Andar 10",
                "Aldeota",
                "Fortaleza",
                "CE",
                "60150-161"
        );
    }

    private ContaCorrente criarContaCorrentePf(Agencia agencia) {
        PessoaFisica titularPf = new PessoaFisica(
                "Rafael Costa",
                "529.982.247-25",
                LocalDate.of(1993, 7, 19),
                "rafael.costa@ufc.br",
                "85999445566",
                criarEnderecoPadrao()
        );
        return new ContaCorrente(agencia, titularPf);
    }

    private ContaEmpresarial criarContaEmpresarialPj(Agencia agencia) {
        PessoaJuridica titularPj = new PessoaJuridica(
                "Nordeste Logistica Integrada LTDA",
                "NorLog",
                "11.222.333/0001-81",
                "financeiro@norlog.com.br",
                "8534445566",
                criarEnderecoPadrao()
        );
        return new ContaEmpresarial(agencia, titularPj);
    }

    private ContextoTransacao criarContextoEmHorarioComercial(
            Agencia agencia,
            BigDecimal saldoDisponivelOrigem,
            BigDecimal totalMovimentadoHoje,
            boolean validarJanelaTed) {
        ContaTecnica contaCaixa = new ContaTecnica(agencia, TipoContaTecnica.CAIXA_TESOURARIA, "000001-0");
        ContaTecnica contaTarifas = new ContaTecnica(agencia, TipoContaTecnica.RECEITA_TARIFAS, "000002-0");
        ContaTecnica contaLiquidacao = new ContaTecnica(agencia, TipoContaTecnica.LIQUIDACAO_EXTERNA, "000003-0");
        ZonedDateTime horarioComercial = ZonedDateTime.of(
                2026, 11, 20, 14, 0, 0, 0,
                ContextoTransacao.FUSO_HORARIO_BANCARIO_PADRAO
        );

        return new ContextoTransacao(
                saldoDisponivelOrigem,
                totalMovimentadoHoje,
                contaCaixa,
                contaTarifas,
                contaLiquidacao,
                horarioComercial,
                validarJanelaTed
        );
    }

    @Test
    void deveProcessarDepositoComPartidaDobradaNoCaixaTesouraria() {
        Agencia agencia = criarAgenciaPadrao();
        Conta contaCliente = criarContaCorrentePf(agencia);
        ContextoTransacao contexto = criarContextoEmHorarioComercial(
                agencia,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false
        );

        TransacaoDeposito deposito = TransacaoFactory.criarDeposito(
                "idem-deposito-001",
                contaCliente,
                new BigDecimal("1500.00"),
                "TERMINAL_AUTOATENDIMENTO"
        );

        List<LancamentoContabil> lancamentos = deposito.processar(contexto, validadoresPadrao);

        assertEquals(StatusTransacao.CONCLUIDA, deposito.getStatusTransacao());
        assertEquals(new BigDecimal("0.00"), deposito.getValorTarifa());
        assertEquals(2, lancamentos.size());
    }

    @Test
    void deveProcessarPixIsentoParaPessoaFisicaETarifadoParaPessoaJuridica() {
        Agencia agencia = criarAgenciaPadrao();
        ContaCorrente contaPf = criarContaCorrentePf(agencia);
        ContaEmpresarial contaPj = criarContaEmpresarialPj(agencia);
        ContextoTransacao contextoComSaldo = criarContextoEmHorarioComercial(
                agencia,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                false
        );

        TransacaoPix pixDePfParaPj = TransacaoFactory.criarPix(
                "idem-pix-pf-001",
                contaPf,
                contaPj,
                new BigDecimal("400.00"),
                "11222333000181",
                "Pagamento de serviço"
        );
        List<LancamentoContabil> lancamentosPixPf = pixDePfParaPj.processar(contextoComSaldo, validadoresPadrao);

        assertEquals(StatusTransacao.CONCLUIDA, pixDePfParaPj.getStatusTransacao());
        assertEquals(new BigDecimal("0.00"), pixDePfParaPj.getValorTarifa());
        assertEquals(2, lancamentosPixPf.size());
        assertNotNull(pixDePfParaPj.getEndToEndId());
        assertTrue(pixDePfParaPj.getEndToEndId().startsWith("E" + Conta.CODIGO_ISPB_BANCO));

        TransacaoPix pixDePjParaPf = TransacaoFactory.criarPix(
                "idem-pix-pj-002",
                contaPj,
                contaPf,
                new BigDecimal("1000.00"),
                "52998224725",
                "Reembolso fornecedor"
        );
        List<LancamentoContabil> lancamentosPixPj = pixDePjParaPf.processar(contextoComSaldo, validadoresPadrao);

        // 0.50% de R$ 1.000,00 = R$ 5,00 de tarifa -> gera 4 lançamentos contábeis (2 do valor principal + 2 da tarifa)
        assertEquals(new BigDecimal("5.00"), pixDePjParaPf.getValorTarifa());
        assertEquals(new BigDecimal("1005.00"), pixDePjParaPf.obterValorTotalDebitadoNaOrigem());
        assertEquals(4, lancamentosPixPj.size());
    }

    @Test
    void deveBloquearPixAcimaDoLimiteNoturnoOuDoLimiteDiarioDoPerfil() {
        Agencia agencia = criarAgenciaPadrao();
        ContaCorrente contaPf = criarContaCorrentePf(agencia);
        ContaEmpresarial contaPj = criarContaEmpresarialPj(agencia);

        ContaTecnica contaCaixa = new ContaTecnica(agencia, TipoContaTecnica.CAIXA_TESOURARIA, "000001-0");
        ContaTecnica contaTarifas = new ContaTecnica(agencia, TipoContaTecnica.RECEITA_TARIFAS, "000002-0");
        ContaTecnica contaLiquidacao = new ContaTecnica(agencia, TipoContaTecnica.LIQUIDACAO_EXTERNA, "000003-0");
        ZonedDateTime horarioNoturno22h = ZonedDateTime.of(
                2026, 11, 20, 22, 30, 0, 0,
                ContextoTransacao.FUSO_HORARIO_BANCARIO_PADRAO
        );

        ContextoTransacao contextoNoturno = new ContextoTransacao(
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                contaCaixa,
                contaTarifas,
                contaLiquidacao,
                horarioNoturno22h,
                false
        );

        TransacaoPix pixNoturnoAcimaDeMilReais = TransacaoFactory.criarPix(
                "idem-pix-noturno-001",
                contaPf,
                contaPj,
                new BigDecimal("1200.00"),
                "11222333000181",
                "Transferência noturna acima do limite"
        );

        assertThrows(LimiteOperacionalExcedidoException.class, () ->
                pixNoturnoAcimaDeMilReais.processar(contextoNoturno, validadoresPadrao)
        );

        ContextoTransacao contextoDiurnoAcimaDoLimitePf = criarContextoEmHorarioComercial(
                agencia,
                new BigDecimal("10000.00"),
                new BigDecimal("4500.00"),
                false
        );

        TransacaoPix pixExcedendoLimiteDiarioPf = TransacaoFactory.criarPix(
                "idem-pix-diurno-002",
                contaPf,
                contaPj,
                new BigDecimal("600.00"),
                "11222333000181",
                "Ultrapassa R$ 5.000,00 diários de PF"
        );

        assertThrows(LimiteOperacionalExcedidoException.class, () ->
                pixExcedendoLimiteDiarioPf.processar(contextoDiurnoAcimaDoLimitePf, validadoresPadrao)
        );
    }

    @Test
    void deveProcessarTedComTarifaFixaEValidarJanelaDeHorarioQuandoAtiva() {
        Agencia agencia = criarAgenciaPadrao();
        ContaCorrente contaOrigem = criarContaCorrentePf(agencia);
        ContextoTransacao contextoDiurno = criarContextoEmHorarioComercial(
                agencia,
                new BigDecimal("2000.00"),
                BigDecimal.ZERO,
                true
        );

        TransacaoTed tedExternaValida = TransacaoFactory.criarTed(
                "idem-ted-001",
                contaOrigem,
                null,
                new BigDecimal("500.00"),
                "00000000",
                "1234",
                "98765-4",
                "11222333000181"
        );

        List<LancamentoContabil> lancamentosTed = tedExternaValida.processar(contextoDiurno, validadoresPadrao);
        assertEquals(StatusTransacao.CONCLUIDA, tedExternaValida.getStatusTransacao());
        assertEquals(new BigDecimal("12.50"), tedExternaValida.getValorTarifa());
        assertEquals(new BigDecimal("512.50"), tedExternaValida.obterValorTotalDebitadoNaOrigem());
        assertEquals(4, lancamentosTed.size());

        ContaTecnica contaCaixa = new ContaTecnica(agencia, TipoContaTecnica.CAIXA_TESOURARIA, "000001-0");
        ContaTecnica contaTarifas = new ContaTecnica(agencia, TipoContaTecnica.RECEITA_TARIFAS, "000002-0");
        ContaTecnica contaLiquidacao = new ContaTecnica(agencia, TipoContaTecnica.LIQUIDACAO_EXTERNA, "000003-0");
        ZonedDateTime horarioForaDaJanela19h = ZonedDateTime.of(
                2026, 11, 20, 19, 0, 0, 0,
                ContextoTransacao.FUSO_HORARIO_BANCARIO_PADRAO
        );

        ContextoTransacao contextoForaDeJanela = new ContextoTransacao(
                new BigDecimal("2000.00"),
                BigDecimal.ZERO,
                contaCaixa,
                contaTarifas,
                contaLiquidacao,
                horarioForaDaJanela19h,
                true
        );

        TransacaoTed tedForaDeHorario = TransacaoFactory.criarTed(
                "idem-ted-002",
                contaOrigem,
                null,
                new BigDecimal("300.00"),
                "00000000",
                "1234",
                "98765-4",
                "11222333000181"
        );

        assertThrows(JanelaHorarioInvalidaException.class, () ->
                tedForaDeHorario.processar(contextoForaDeJanela, validadoresPadrao)
        );
    }

    @Test
    void deveValidarSaqueSaldoInsuficienteEContaBloqueada() {
        Agencia agencia = criarAgenciaPadrao();
        ContaCorrente contaPf = criarContaCorrentePf(agencia);
        ContextoTransacao contextoComMilReais = criarContextoEmHorarioComercial(
                agencia,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                false
        );

        TransacaoSaque saqueValido = TransacaoFactory.criarSaque(
                "idem-saque-001",
                contaPf,
                new BigDecimal("400.00"),
                "ATM-UFC-BENFICA-01"
        );
        List<LancamentoContabil> lancamentosSaque = saqueValido.processar(contextoComMilReais, validadoresPadrao);
        assertEquals(2, lancamentosSaque.size());

        TransacaoSaque saqueAcimaDoSaldo = TransacaoFactory.criarSaque(
                "idem-saque-002",
                contaPf,
                new BigDecimal("1200.00"),
                "ATM-UFC-BENFICA-01"
        );
        assertThrows(SaldoInsuficienteException.class, () ->
                saqueAcimaDoSaldo.processar(contextoComMilReais, validadoresPadrao)
        );

        contaPf.bloquearConta();
        TransacaoSaque saqueEmContaBloqueada = TransacaoFactory.criarSaque(
                "idem-saque-003",
                contaPf,
                new BigDecimal("100.00"),
                "ATM-UFC-BENFICA-01"
        );
        assertThrows(ContaInaptaParaOperacaoException.class, () ->
                saqueEmContaBloqueada.processar(contextoComMilReais, validadoresPadrao)
        );
    }
}
