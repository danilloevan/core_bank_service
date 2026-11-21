package br.ufc.poo.corebank.domain;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
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
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;
import br.ufc.poo.corebank.domain.model.ledger.VerificadorPartidasDobradas;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerDominioTest {

    private Agencia criarAgenciaPadrao() {
        return new Agencia(Agencia.CODIGO_AGENCIA_MATRIZ, Agencia.NOME_AGENCIA_MATRIZ);
    }

    private Endereco criarEnderecoPadrao() {
        return new Endereco(
                "Avenida Beira Mar",
                "1500",
                "Apto 902",
                "Meireles",
                "Fortaleza",
                "CE",
                "60165-121"
        );
    }

    private Conta criarContaCorrente(Agencia agencia) {
        PessoaFisica titularPf = new PessoaFisica(
                "Lucas Mendes",
                "529.982.247-25",
                LocalDate.of(1994, 4, 12),
                "lucas.mendes@ufc.br",
                "85999112233",
                criarEnderecoPadrao()
        );
        return new ContaCorrente(agencia, titularPf);
    }

    private Conta criarContaEmpresarial(Agencia agencia) {
        PessoaJuridica titularPj = new PessoaJuridica(
                "Ceara Pagamentos Digitais S/A",
                "CearaPay",
                "11.222.333/0001-81",
                "tesouraria@cearapay.com.br",
                "8532224455",
                criarEnderecoPadrao()
        );
        return new ContaEmpresarial(agencia, titularPj);
    }

    @Test
    void deveGarantirImutabilidadeEPrecisaoMonetariaDoLancamentoContabil() {
        Agencia agencia = criarAgenciaPadrao();
        Conta contaCliente = criarContaCorrente(agencia);
        UUID transacaoId = UUID.randomUUID();

        LancamentoContabil lancamentoArredondado = new LancamentoContabil(
                transacaoId,
                contaCliente,
                TipoLancamento.CREDITO,
                new BigDecimal("100.125"),
                "Depósito inicial com arredondamento bancário HALF_EVEN"
        );

        assertEquals(new BigDecimal("100.12"), lancamentoArredondado.getValor());
        assertEquals(new BigDecimal("100.12"), lancamentoArredondado.obterValorComSinalAlgebrico());

        boolean possuiAlgumSetter = Arrays.stream(LancamentoContabil.class.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(nomeMetodo -> nomeMetodo.startsWith("set"));
        assertFalse(possuiAlgumSetter, "LancamentoContabil é append-only e não pode expor setters.");

        assertThrows(RegraNegocioException.class, () -> new LancamentoContabil(
                transacaoId,
                contaCliente,
                TipoLancamento.CREDITO,
                BigDecimal.ZERO,
                "Valor zerado inválido"
        ));

        assertThrows(RegraNegocioException.class, () -> new LancamentoContabil(
                transacaoId,
                contaCliente,
                TipoLancamento.DEBITO,
                new BigDecimal("-10.00"),
                "Valor negativo inválido"
        ));

        assertThrows(RegraNegocioException.class, () -> new LancamentoContabil(
                null,
                contaCliente,
                TipoLancamento.CREDITO,
                new BigDecimal("50.00"),
                "Transação nula inválida"
        ));
    }

    @Test
    void deveValidarPartidasDobradasSimplesECompostasComTarifa() {
        Agencia agencia = criarAgenciaPadrao();
        Conta contaOrigem = criarContaCorrente(agencia);
        Conta contaDestino = criarContaEmpresarial(agencia);
        Conta contaReceitaTarifas = new ContaTecnica(agencia, TipoContaTecnica.RECEITA_TARIFAS, "000002-0");

        UUID transacaoSimplesId = UUID.randomUUID();
        List<LancamentoContabil> partidasSimplesValidas = List.of(
                new LancamentoContabil(transacaoSimplesId, contaOrigem, TipoLancamento.DEBITO, new BigDecimal("250.00"), "PIX enviado"),
                new LancamentoContabil(transacaoSimplesId, contaDestino, TipoLancamento.CREDITO, new BigDecimal("250.00"), "PIX recebido")
        );
        assertDoesNotThrow(() -> VerificadorPartidasDobradas.validarEquilibrioPartidasDobradas(partidasSimplesValidas));

        UUID transacaoCompostaId = UUID.randomUUID();
        List<LancamentoContabil> partidasCompostasComTarifa = List.of(
                new LancamentoContabil(transacaoCompostaId, contaOrigem, TipoLancamento.DEBITO, new BigDecimal("100.00"), "TED enviada"),
                new LancamentoContabil(transacaoCompostaId, contaOrigem, TipoLancamento.DEBITO, new BigDecimal("12.50"), "Tarifa TED"),
                new LancamentoContabil(transacaoCompostaId, contaDestino, TipoLancamento.CREDITO, new BigDecimal("100.00"), "TED recebida"),
                new LancamentoContabil(transacaoCompostaId, contaReceitaTarifas, TipoLancamento.CREDITO, new BigDecimal("12.50"), "Receita tarifa TED")
        );
        assertDoesNotThrow(() -> VerificadorPartidasDobradas.validarEquilibrioPartidasDobradas(partidasCompostasComTarifa));
    }

    @Test
    void deveRejeitarPartidasDobradasDesbalanceadasOuIncompletas() {
        Agencia agencia = criarAgenciaPadrao();
        Conta contaOrigem = criarContaCorrente(agencia);
        Conta contaDestino = criarContaEmpresarial(agencia);

        UUID transacaoId = UUID.randomUUID();
        List<LancamentoContabil> partidaSemContrapartida = List.of(
                new LancamentoContabil(transacaoId, contaOrigem, TipoLancamento.CREDITO, new BigDecimal("500.00"), "Lançamento isolado")
        );
        assertThrows(RegraNegocioException.class, () ->
                VerificadorPartidasDobradas.validarEquilibrioPartidasDobradas(partidaSemContrapartida)
        );

        List<LancamentoContabil> partidasDesbalanceadas = List.of(
                new LancamentoContabil(transacaoId, contaOrigem, TipoLancamento.DEBITO, new BigDecimal("200.00"), "Débito origem"),
                new LancamentoContabil(transacaoId, contaDestino, TipoLancamento.CREDITO, new BigDecimal("190.00"), "Crédito menor")
        );
        assertThrows(RegraNegocioException.class, () ->
                VerificadorPartidasDobradas.validarEquilibrioPartidasDobradas(partidasDesbalanceadas)
        );

        List<LancamentoContabil> partidasTransacoesMisturadas = List.of(
                new LancamentoContabil(UUID.randomUUID(), contaOrigem, TipoLancamento.DEBITO, new BigDecimal("100.00"), "Débito T1"),
                new LancamentoContabil(UUID.randomUUID(), contaDestino, TipoLancamento.CREDITO, new BigDecimal("100.00"), "Crédito T2")
        );
        assertThrows(RegraNegocioException.class, () ->
                VerificadorPartidasDobradas.validarEquilibrioPartidasDobradas(partidasTransacoesMisturadas)
        );
    }

    @Test
    void deveCalcularSaldoDinamicoENaDataDeCorteSemArmazenarSaldoMutavel() {
        Agencia agencia = criarAgenciaPadrao();
        Conta contaCliente = criarContaCorrente(agencia);

        assertEquals(new BigDecimal("0.00"), VerificadorPartidasDobradas.calcularSaldoDeLancamentos(List.of()));

        Instant instanteDia1 = Instant.parse("2026-11-10T10:00:00Z");
        Instant instanteDia2 = Instant.parse("2026-11-11T14:30:00Z");
        Instant instanteDia3 = Instant.parse("2026-11-12T09:15:00Z");

        List<LancamentoContabil> historicoConta = List.of(
                new LancamentoContabil(UUID.randomUUID(), contaCliente, TipoLancamento.CREDITO, new BigDecimal("1000.00"), "Depósito inicial", instanteDia1),
                new LancamentoContabil(UUID.randomUUID(), contaCliente, TipoLancamento.CREDITO, new BigDecimal("250.50"), "PIX recebido", instanteDia2),
                new LancamentoContabil(UUID.randomUUID(), contaCliente, TipoLancamento.DEBITO, new BigDecimal("300.25"), "TED enviada", instanteDia2.plusSeconds(60)),
                new LancamentoContabil(UUID.randomUUID(), contaCliente, TipoLancamento.DEBITO, new BigDecimal("12.50"), "Tarifa TED", instanteDia3)
        );

        BigDecimal saldoFinalCalculado = VerificadorPartidasDobradas.calcularSaldoDeLancamentos(historicoConta);
        assertEquals(new BigDecimal("937.75"), saldoFinalCalculado);

        BigDecimal saldoAteFinalDia1 = VerificadorPartidasDobradas.calcularSaldoAteDataCorte(historicoConta, instanteDia1);
        assertEquals(new BigDecimal("1000.00"), saldoAteFinalDia1);

        BigDecimal saldoAntesDoDia3 = VerificadorPartidasDobradas.calcularSaldoAteDataCorte(
                historicoConta,
                Instant.parse("2026-11-11T23:59:59Z")
        );
        assertEquals(new BigDecimal("950.25"), saldoAntesDoDia3);
    }
}
