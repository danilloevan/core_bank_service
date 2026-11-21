package br.ufc.poo.corebank.domain;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.cliente.Endereco;
import br.ufc.poo.corebank.domain.model.cliente.PessoaFisica;
import br.ufc.poo.corebank.domain.model.cliente.PessoaJuridica;
import br.ufc.poo.corebank.domain.model.conta.Agencia;
import br.ufc.poo.corebank.domain.model.conta.ChavePix;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.ContaCorrente;
import br.ufc.poo.corebank.domain.model.conta.ContaEmpresarial;
import br.ufc.poo.corebank.domain.model.conta.ContaTecnica;
import br.ufc.poo.corebank.domain.model.conta.StatusConta;
import br.ufc.poo.corebank.domain.model.conta.TipoChavePix;
import br.ufc.poo.corebank.domain.model.conta.TipoContaTecnica;
import br.ufc.poo.corebank.domain.model.conta.TipoOperacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContaDominioTest {

    private Endereco criarEnderecoPadrao() {
        return new Endereco(
                "Rua Barbosa de Freitas",
                "1200",
                "Sala 401",
                "Aldeota",
                "Fortaleza",
                "CE",
                "60170-021"
        );
    }

    private PessoaFisica criarPessoaFisicaPadrao() {
        return new PessoaFisica(
                "Ana Beatriz Souza",
                "529.982.247-25",
                LocalDate.of(1992, 8, 25),
                "ana.souza@ufc.br",
                "85988112233",
                criarEnderecoPadrao()
        );
    }

    private PessoaJuridica criarPessoaJuridicaPadrao() {
        return new PessoaJuridica(
                "Solucoes Financeiras Ceara LTDA",
                "CearaFin",
                "11.222.333/0001-81",
                "contato@cearafin.com.br",
                "8531112233",
                criarEnderecoPadrao()
        );
    }

    @Test
    void deveCriarContasPolimorficamenteEAgregarNaAgencia() {
        Agencia agenciaMatriz = new Agencia(Agencia.CODIGO_AGENCIA_MATRIZ, Agencia.NOME_AGENCIA_MATRIZ);
        PessoaFisica titularPf = criarPessoaFisicaPadrao();
        PessoaJuridica titularPj = criarPessoaJuridicaPadrao();

        Conta contaCorrente = new ContaCorrente(agenciaMatriz, titularPf);
        Conta contaEmpresarial = new ContaEmpresarial(agenciaMatriz, titularPj);
        Conta contaTecnica = new ContaTecnica(agenciaMatriz, TipoContaTecnica.CAIXA_TESOURARIA, "000001-0");

        agenciaMatriz.adicionarConta(contaCorrente);
        agenciaMatriz.adicionarConta(contaEmpresarial);
        agenciaMatriz.adicionarConta(contaTecnica);

        assertEquals(3, agenciaMatriz.getContas().size());
        assertEquals("12345678", Conta.CODIGO_ISPB_BANCO);

        assertTrue(contaCorrente.podeRealizarOperacao(TipoOperacao.PIX));
        assertTrue(contaEmpresarial.podeRealizarOperacao(TipoOperacao.TED));
        assertFalse(contaTecnica.podeRealizarOperacao(TipoOperacao.SAQUE));

        assertEquals(new BigDecimal("2000.00"), contaCorrente.obterLimiteSaqueDiario());
        assertEquals(new BigDecimal("10000.00"), contaEmpresarial.obterLimiteSaqueDiario());
        assertEquals(BigDecimal.ZERO, contaTecnica.obterLimiteSaqueDiario());
    }

    @Test
    void deveImpedirTitularIncompativelComSubclasseDeConta() {
        Agencia agenciaMatriz = new Agencia("0001", "Agência Matriz");
        PessoaFisica titularPf = criarPessoaFisicaPadrao();
        PessoaJuridica titularPj = criarPessoaJuridicaPadrao();

        assertThrows(RegraNegocioException.class, () -> new ContaEmpresarial(agenciaMatriz, titularPf));
        assertThrows(RegraNegocioException.class, () -> new ContaCorrente(agenciaMatriz, titularPj));
    }

    @Test
    void deveControlarCicloDeVidaDaContaEExigirSaldoZeroParaEncerramento() {
        Agencia agenciaMatriz = new Agencia("0001", "Agência Matriz");
        Conta contaCorrente = new ContaCorrente(agenciaMatriz, criarPessoaFisicaPadrao());

        contaCorrente.bloquearConta();
        assertEquals(StatusConta.BLOQUEADA, contaCorrente.getStatusConta());
        assertFalse(contaCorrente.podeRealizarOperacao(TipoOperacao.PIX));

        contaCorrente.ativarConta();
        assertEquals(StatusConta.ATIVA, contaCorrente.getStatusConta());

        assertThrows(RegraNegocioException.class, () ->
                contaCorrente.encerrarConta(new BigDecimal("150.00"))
        );

        contaCorrente.encerrarConta(new BigDecimal("0.00"));
        assertEquals(StatusConta.ENCERRADA, contaCorrente.getStatusConta());
    }

    @Test
    void deveRegistrarChavesPixRespeitandoRegrasDoTitular() {
        Agencia agenciaMatriz = new Agencia("0001", "Agência Matriz");
        PessoaFisica titularPf = criarPessoaFisicaPadrao();
        Conta contaCorrente = new ContaCorrente(agenciaMatriz, titularPf);

        ChavePix chaveCpf = new ChavePix(TipoChavePix.CPF, "529.982.247-25", contaCorrente);
        assertEquals("52998224725", chaveCpf.getValorChave());

        ChavePix chaveAleatoria = new ChavePix(TipoChavePix.ALEATORIA, null, contaCorrente);
        assertNotNull(chaveAleatoria.getValorChave());

        assertThrows(RegraNegocioException.class, () ->
                new ChavePix(TipoChavePix.CPF, "123.456.789-01", contaCorrente)
        );

        assertThrows(RegraNegocioException.class, () ->
                new ChavePix(TipoChavePix.CNPJ, "11.222.333/0001-81", contaCorrente)
        );
    }
}
