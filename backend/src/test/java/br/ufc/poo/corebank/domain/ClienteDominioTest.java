package br.ufc.poo.corebank.domain;

import br.ufc.poo.corebank.domain.exception.DocumentoInvalidoException;
import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import br.ufc.poo.corebank.domain.model.cliente.Endereco;
import br.ufc.poo.corebank.domain.model.cliente.PessoaFisica;
import br.ufc.poo.corebank.domain.model.cliente.PessoaJuridica;
import br.ufc.poo.corebank.domain.model.cliente.TipoCliente;
import br.ufc.poo.corebank.domain.model.conta.TipoConta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteDominioTest {

    private Endereco criarEnderecoPadrao() {
        return new Endereco(
                "Avenida da Universidade",
                "2853",
                "Bloco Didático",
                "Benfica",
                "Fortaleza",
                "CE",
                "60020-181"
        );
    }

    @Test
    void deveValidarDigitosDeCpfEstaticamenteEPolimorficamente() {
        assertTrue(PessoaFisica.validarDigitosCpf("529.982.247-25"));
        assertFalse(PessoaFisica.validarDigitosCpf("529.982.247-00"));
        assertFalse(PessoaFisica.validarDigitosCpf("111.111.111-11"));
        assertFalse(PessoaFisica.validarDigitosCpf("12345"));

        Cliente clientePessoaFisica = new PessoaFisica(
                "Maria Oliveira",
                "529.982.247-25",
                LocalDate.of(1998, 5, 14),
                "maria.oliveira@ufc.br",
                "(85) 99988-7766",
                criarEnderecoPadrao()
        );

        assertEquals(TipoCliente.PESSOA_FISICA, clientePessoaFisica.obterTipoCliente());
        assertEquals("52998224725", clientePessoaFisica.getDocumentoFiscal());
        assertTrue(clientePessoaFisica.validarDocumentoFiscal());
        assertTrue(clientePessoaFisica.isIsentoTarifaPix());
        assertEquals(new BigDecimal("5000.00"), clientePessoaFisica.obterLimiteDiarioPixPadrao());
        assertTrue(clientePessoaFisica.podeSerTitularDe(TipoConta.CONTA_CORRENTE));
        assertFalse(clientePessoaFisica.podeSerTitularDe(TipoConta.CONTA_EMPRESARIAL));
    }

    @Test
    void deveValidarDigitosDeCnpjEstaticamenteEPolimorficamente() {
        assertTrue(PessoaJuridica.validarDigitosCnpj("11.222.333/0001-81"));
        assertFalse(PessoaJuridica.validarDigitosCnpj("11.222.333/0001-99"));
        assertFalse(PessoaJuridica.validarDigitosCnpj("00.000.000/0000-00"));

        Cliente clientePessoaJuridica = new PessoaJuridica(
                "Tecnologia Bancária Nordeste LTDA",
                "TechBank NE",
                "11.222.333/0001-81",
                "contato@techbank.com.br",
                "8533445566",
                criarEnderecoPadrao()
        );

        assertEquals(TipoCliente.PESSOA_JURIDICA, clientePessoaJuridica.obterTipoCliente());
        assertEquals("11222333000181", clientePessoaJuridica.getDocumentoFiscal());
        assertTrue(clientePessoaJuridica.validarDocumentoFiscal());
        assertFalse(clientePessoaJuridica.isIsentoTarifaPix());
        assertEquals(new BigDecimal("50000.00"), clientePessoaJuridica.obterLimiteDiarioPixPadrao());
        assertTrue(clientePessoaJuridica.podeSerTitularDe(TipoConta.CONTA_EMPRESARIAL));
        assertFalse(clientePessoaJuridica.podeSerTitularDe(TipoConta.CONTA_CORRENTE));
    }

    @Test
    void deveDemonstrarPolimorfismoEmListaDeClientes() {
        Cliente clientePf = new PessoaFisica(
                "João Carlos",
                "529.982.247-25",
                LocalDate.of(1995, 3, 20),
                "joao@ufc.br",
                "85988776655",
                criarEnderecoPadrao()
        );
        Cliente clientePj = new PessoaJuridica(
                "Comércio Digital Fortaleza S/A",
                "FortalCom",
                "11.222.333/0001-81",
                "financeiro@fortalcom.com.br",
                "8532221100",
                criarEnderecoPadrao()
        );

        List<Cliente> carteiraClientes = List.of(clientePf, clientePj);

        BigDecimal somaLimitesPix = carteiraClientes.stream()
                .map(Cliente::obterLimiteDiarioPixPadrao)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("55000.00"), somaLimitesPix);
    }

    @Test
    void deveRejeitarCpfInvalidoEMenorDeIdade() {
        Endereco endereco = criarEnderecoPadrao();

        assertThrows(DocumentoInvalidoException.class, () -> new PessoaFisica(
                "Pedro Silva",
                "529.982.247-99",
                LocalDate.of(1990, 1, 10),
                "pedro@ufc.br",
                "85999990000",
                endereco
        ));

        assertThrows(RegraNegocioException.class, () -> new PessoaFisica(
                "Menor de Idade",
                "529.982.247-25",
                LocalDate.now().minusYears(16),
                "menor@ufc.br",
                "85999990000",
                endereco
        ));
    }
}
