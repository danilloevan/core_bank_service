package br.ufc.poo.corebank.domain;

import br.ufc.poo.corebank.domain.exception.ContaInaptaParaOperacaoException;
import br.ufc.poo.corebank.domain.exception.DominioException;
import br.ufc.poo.corebank.domain.exception.JanelaHorarioInvalidaException;
import br.ufc.poo.corebank.domain.exception.LimiteOperacionalExcedidoException;
import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.exception.SaldoInsuficienteException;
import br.ufc.poo.corebank.domain.factory.TransacaoFactory;
import br.ufc.poo.corebank.domain.model.cliente.Cliente;
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
import br.ufc.poo.corebank.domain.model.transacao.Transacao;
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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PadroesProjetoEPooDominioTest {

    private Endereco criarEnderecoPadrao() {
        return new Endereco(
                "Avenida Mister Hull",
                "2995",
                "Campus do Pici",
                "Pici",
                "Fortaleza",
                "CE",
                "60440-900"
        );
    }

    private Agencia criarAgenciaPadrao() {
        return new Agencia(Agencia.CODIGO_AGENCIA_MATRIZ, Agencia.NOME_AGENCIA_MATRIZ);
    }

    private ContaCorrente criarContaPf(Agencia agencia, String numeroConta) {
        PessoaFisica titularPf = new PessoaFisica(
                "Carlos Eduardo Lima",
                "529.982.247-25",
                LocalDate.of(1990, 6, 15),
                "carlos.lima@ufc.br",
                "85999123456",
                criarEnderecoPadrao()
        );
        return new ContaCorrente(agencia, titularPf, numeroConta);
    }

    private ContaEmpresarial criarContaPj(Agencia agencia, String numeroConta) {
        PessoaJuridica titularPj = new PessoaJuridica(
                "Inovacao Tecnologica do Ceara S/A",
                "InovaCE",
                "11.222.333/0001-81",
                "financeiro@inovace.com.br",
                "8533221100",
                criarEnderecoPadrao()
        );
        return new ContaEmpresarial(agencia, titularPj, numeroConta);
    }

    private ContextoTransacao criarContexto(
            Agencia agencia,
            BigDecimal saldoDisponivelOrigem,
            BigDecimal totalMovimentadoHoje,
            int horaDoDia,
            int minutoDoDia,
            boolean validarJanelaTed) {
        ContaTecnica contaCaixa = new ContaTecnica(agencia, TipoContaTecnica.CAIXA_TESOURARIA, "000001-0");
        ContaTecnica contaTarifas = new ContaTecnica(agencia, TipoContaTecnica.RECEITA_TARIFAS, "000002-0");
        ContaTecnica contaLiquidacao = new ContaTecnica(agencia, TipoContaTecnica.LIQUIDACAO_EXTERNA, "000003-0");
        ZonedDateTime instanteReferencia = ZonedDateTime.of(
                2026, 11, 20, horaDoDia, minutoDoDia, 0, 0,
                ContextoTransacao.FUSO_HORARIO_BANCARIO_PADRAO
        );
        return new ContextoTransacao(
                saldoDisponivelOrigem,
                totalMovimentadoHoje,
                contaCaixa,
                contaTarifas,
                contaLiquidacao,
                instanteReferencia,
                validarJanelaTed
        );
    }

    @Test
    void deveTestarEstrategiasDeTarifaPolimorficamenteComPisoETeto() {
        EstrategiaTarifa estrategiaIsenta = new TarifaIsentaStrategy();
        EstrategiaTarifa estrategiaFixa = new TarifaFixaStrategy(new BigDecimal("12.50"));
        EstrategiaTarifa estrategiaPercentual = new TarifaPercentualStrategy(
                new BigDecimal("0.50"),
                new BigDecimal("1.50"),
                new BigDecimal("15.00")
        );

        assertEquals(new BigDecimal("0.00"), estrategiaIsenta.calcularTarifa(new BigDecimal("10000.00")));
        assertEquals("ISENTA", estrategiaIsenta.obterDescricaoEstrategia());

        assertEquals(new BigDecimal("12.50"), estrategiaFixa.calcularTarifa(new BigDecimal("50.00")));
        assertTrue(estrategiaFixa.obterDescricaoEstrategia().contains("12.50"));

        // 0.50% de R$ 100,00 = R$ 0,50 -> aplica piso mínimo de R$ 1,50
        assertEquals(new BigDecimal("1.50"), estrategiaPercentual.calcularTarifa(new BigDecimal("100.00")));
        // 0.50% de R$ 1.000,00 = R$ 5,00 -> dentro da faixa
        assertEquals(new BigDecimal("5.00"), estrategiaPercentual.calcularTarifa(new BigDecimal("1000.00")));
        // 0.50% de R$ 10.000,00 = R$ 50,00 -> aplica teto máximo de R$ 15,00
        assertEquals(new BigDecimal("15.00"), estrategiaPercentual.calcularTarifa(new BigDecimal("10000.00")));
        assertEquals(new BigDecimal("0.00"), estrategiaPercentual.calcularTarifa(BigDecimal.ZERO));

        assertThrows(RegraNegocioException.class, () -> new TarifaFixaStrategy(new BigDecimal("-1.00")));
        assertThrows(RegraNegocioException.class, () ->
                new TarifaPercentualStrategy(BigDecimal.ZERO, new BigDecimal("1.50"), new BigDecimal("15.00"))
        );
        assertThrows(RegraNegocioException.class, () ->
                new TarifaPercentualStrategy(new BigDecimal("0.50"), new BigDecimal("-1.00"), new BigDecimal("15.00"))
        );
        assertThrows(RegraNegocioException.class, () ->
                new TarifaPercentualStrategy(new BigDecimal("0.50"), new BigDecimal("20.00"), new BigDecimal("15.00"))
        );
    }

    @Test
    void deveTestarCadeiaDeValidadoresPolimorficamente() {
        Agencia agencia = criarAgenciaPadrao();
        ContaCorrente contaPfOrigem = criarContaPf(agencia, "100101-1");
        ContaEmpresarial contaPjDestino = criarContaPj(agencia, "200202-2");
        ContaTecnica contaTecnica = new ContaTecnica(agencia, TipoContaTecnica.CAIXA_TESOURARIA, "000001-0");

        ValidadorTransacao validadorStatus = new ValidadorStatusConta();
        ValidadorTransacao validadorSaldo = new ValidadorSaldoDisponivel();
        ValidadorTransacao validadorLimite = new ValidadorLimiteDiario();
        ValidadorTransacao validadorJanelaTed = new ValidadorJanelaHorarioTed();

        // 1. ValidadorStatusConta: ContaTecnica não pode iniciar saque nem receber depósito direto de cliente
        TransacaoSaque saqueDeContaTecnica = TransacaoFactory.criarSaque(
                "idem-saque-tecnica",
                contaTecnica,
                new BigDecimal("100.00"),
                "ATM-01"
        );
        ContextoTransacao contextoPadrao = criarContexto(
                agencia,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                10,
                30,
                true
        );
        assertThrows(ContaInaptaParaOperacaoException.class, () ->
                validadorStatus.validar(saqueDeContaTecnica, contextoPadrao)
        );

        TransacaoDeposito depositoEmContaTecnica = TransacaoFactory.criarDeposito(
                "idem-dep-tecnica",
                contaTecnica,
                new BigDecimal("100.00"),
                "AGENCIA"
        );
        assertThrows(ContaInaptaParaOperacaoException.class, () ->
                validadorStatus.validar(depositoEmContaTecnica, contextoPadrao)
        );

        // 2. ValidadorSaldoDisponivel: considera valor principal + tarifa (R$ 500,00 TED + R$ 12,50 tarifa = R$ 512,50 exigidos)
        ContextoTransacao contextoComQuinhentosReais = criarContexto(
                agencia,
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                10,
                30,
                true
        );
        TransacaoTed tedQuinhentosReais = TransacaoFactory.criarTed(
                "idem-ted-saldo-exato",
                contaPfOrigem,
                contaPjDestino,
                new BigDecimal("500.00"),
                Conta.CODIGO_ISPB_BANCO,
                "0001",
                contaPjDestino.getNumeroConta(),
                "11222333000181"
        );
        assertThrows(SaldoInsuficienteException.class, () ->
                tedQuinhentosReais.processar(contextoComQuinhentosReais, List.of(validadorSaldo))
        );

        // 3. ValidadorLimiteDiario: Saque PF (limite R$ 2.000,00) vs Saque PJ (limite R$ 10.000,00)
        ContextoTransacao contextoSaqueAcumulado = criarContexto(
                agencia,
                new BigDecimal("20000.00"),
                new BigDecimal("1800.00"),
                11,
                0,
                false
        );
        TransacaoSaque saquePfExcedendoLimite = TransacaoFactory.criarSaque(
                "idem-saque-pf-limite",
                contaPfOrigem,
                new BigDecimal("300.00"),
                "ATM-01"
        );
        assertThrows(LimiteOperacionalExcedidoException.class, () ->
                validadorLimite.validar(saquePfExcedendoLimite, contextoSaqueAcumulado)
        );

        TransacaoSaque saquePjDentroDoLimiteEmpresarial = TransacaoFactory.criarSaque(
                "idem-saque-pj-limite",
                contaPjDestino,
                new BigDecimal("3000.00"),
                "ATM-01"
        );
        assertDoesNotThrow(() ->
                validadorLimite.validar(saquePjDentroDoLimiteEmpresarial, contextoSaqueAcumulado)
        );

        // 4. ValidadorJanelaHorarioTed: antes das 06:30 e depois das 17:00
        ContextoTransacao contextoMadrugada = criarContexto(
                agencia,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                6,
                15,
                true
        );
        assertThrows(JanelaHorarioInvalidaException.class, () ->
                validadorJanelaTed.validar(tedQuinhentosReais, contextoMadrugada)
        );
    }

    @Test
    void deveExecutarDespachoPolimorficoEmColecaoDeTransacoesDistintas() {
        Agencia agencia = criarAgenciaPadrao();
        ContaCorrente contaPf = criarContaPf(agencia, "100111-1");
        ContaEmpresarial contaPj = criarContaPj(agencia, "200222-2");
        ContextoTransacao contexto = criarContexto(
                agencia,
                new BigDecimal("15000.00"),
                BigDecimal.ZERO,
                15,
                0,
                true
        );
        List<ValidadorTransacao> validadores = TransacaoFactory.criarCadeiaValidadoresPadrao();

        List<Transacao> loteTransacoesPolimorficas = List.of(
                TransacaoFactory.criarDeposito("idem-lote-1", contaPf, new BigDecimal("2000.00"), "APP_MOBILE"),
                TransacaoFactory.criarPix("idem-lote-2", contaPf, contaPj, new BigDecimal("300.00"), "11222333000181", "PIX PF->PJ"),
                TransacaoFactory.criarPix("idem-lote-3", contaPj, contaPf, new BigDecimal("1000.00"), "52998224725", "PIX PJ->PF"),
                TransacaoFactory.criarTed("idem-lote-4", contaPf, contaPj, new BigDecimal("500.00"), Conta.CODIGO_ISPB_BANCO, "0001", contaPj.getNumeroConta(), "11222333000181"),
                TransacaoFactory.criarSaque("idem-lote-5", contaPf, new BigDecimal("200.00"), "ATM-PICI-02")
        );

        int totalLancamentosGerados = loteTransacoesPolimorficas.stream()
                .map(transacao -> transacao.processar(contexto, validadores))
                .mapToInt(List::size)
                .sum();

        // Depósito (2) + PIX PF isento (2) + PIX PJ tarifado (4) + TED tarifada (4) + Saque isento (2) = 14 lançamentos
        assertEquals(14, totalLancamentosGerados);

        // Garante que transação já concluída não pode ser reprocessada e que lista de lançamentos é imutável
        Transacao primeiraTransacao = loteTransacoesPolimorficas.get(0);
        assertThrows(RegraNegocioException.class, () -> primeiraTransacao.processar(contexto, validadores));
        assertThrows(UnsupportedOperationException.class, () -> primeiraTransacao.getLancamentos().clear());
    }

    @Test
    void deveValidarRegrasEstaticasERestricoesDeAutotransferenciaPixETed() {
        Agencia agencia = criarAgenciaPadrao();
        ContaCorrente contaPf = criarContaPf(agencia, "100333-3");
        ContextoTransacao contexto = criarContexto(
                agencia,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                14,
                0,
                false
        );

        assertTrue(TransacaoPix.isHorarioNoturno(LocalTime.of(20, 0)));
        assertTrue(TransacaoPix.isHorarioNoturno(LocalTime.of(23, 59)));
        assertTrue(TransacaoPix.isHorarioNoturno(LocalTime.of(5, 59)));
        assertFalse(TransacaoPix.isHorarioNoturno(LocalTime.of(6, 0)));
        assertFalse(TransacaoPix.isHorarioNoturno(LocalTime.of(19, 59)));

        assertTrue(TransacaoTed.isDentroDaJanelaOperacionalTed(LocalTime.of(6, 30)));
        assertTrue(TransacaoTed.isDentroDaJanelaOperacionalTed(LocalTime.of(17, 0)));
        assertFalse(TransacaoTed.isDentroDaJanelaOperacionalTed(LocalTime.of(6, 29)));
        assertFalse(TransacaoTed.isDentroDaJanelaOperacionalTed(LocalTime.of(17, 1)));

        assertEquals("100001-2", Conta.formatarNumeroContaComDigito(100001L));

        TransacaoPix pixMesmaConta = TransacaoFactory.criarPix(
                "idem-pix-auto",
                contaPf,
                contaPf,
                new BigDecimal("100.00"),
                "52998224725",
                "Autotransferência inválida"
        );
        assertThrows(RegraNegocioException.class, () ->
                pixMesmaConta.processar(contexto, TransacaoFactory.criarCadeiaValidadoresPadrao())
        );

        TransacaoTed tedMesmaConta = TransacaoFactory.criarTed(
                "idem-ted-auto",
                contaPf,
                contaPf,
                new BigDecimal("100.00"),
                Conta.CODIGO_ISPB_BANCO,
                "0001",
                contaPf.getNumeroConta(),
                "52998224725"
        );
        assertThrows(RegraNegocioException.class, () ->
                tedMesmaConta.processar(contexto, TransacaoFactory.criarCadeiaValidadoresPadrao())
        );
    }

    @Test
    void deveAuditarRequisitosEstruturaisDaRubricaDePooViaReflexao() throws NoSuchFieldException {
        // 2.d) Classes abstratas e interfaces genuínas
        assertTrue(Modifier.isAbstract(Cliente.class.getModifiers()));
        assertTrue(Modifier.isAbstract(Conta.class.getModifiers()));
        assertTrue(Modifier.isAbstract(Transacao.class.getModifiers()));
        assertTrue(Modifier.isAbstract(DominioException.class.getModifiers()));
        assertTrue(EstrategiaTarifa.class.isInterface());
        assertTrue(ValidadorTransacao.class.isInterface());

        // 2.f) Uso explícito dos 3 modificadores de acesso (public, protected, private)
        Field campoIdPrivado = Conta.class.getDeclaredField("id");
        Field campoNumeroContaProtegido = Conta.class.getDeclaredField("numeroConta");
        Field campoIspbPublicoEstatico = Conta.class.getDeclaredField("CODIGO_ISPB_BANCO");

        assertTrue(Modifier.isPrivate(campoIdPrivado.getModifiers()));
        assertTrue(Modifier.isProtected(campoNumeroContaProtegido.getModifiers()));
        assertTrue(Modifier.isPublic(campoIspbPublicoEstatico.getModifiers()));
        assertTrue(Modifier.isStatic(campoIspbPublicoEstatico.getModifiers()));

        // Regra inegociável do domínio financeiro: Conta NÃO possui atributo/coluna de saldo mutável
        boolean contaPossuiAtributoSaldo = List.of(Conta.class.getDeclaredFields()).stream()
                .anyMatch(campo -> campo.getName().toLowerCase().contains("saldo"));
        assertFalse(contaPossuiAtributoSaldo, "A entidade Conta jamais deve armazenar atributo de saldo mutável.");
    }
}
