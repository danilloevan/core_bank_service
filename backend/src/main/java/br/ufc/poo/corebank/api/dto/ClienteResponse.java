package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import br.ufc.poo.corebank.domain.model.cliente.PessoaFisica;
import br.ufc.poo.corebank.domain.model.cliente.PessoaJuridica;
import br.ufc.poo.corebank.domain.model.cliente.TipoCliente;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        TipoCliente tipoCliente,
        String nome,
        String documentoFiscal,
        String razaoSocial,
        String nomeFantasia,
        LocalDate dataNascimento,
        String email,
        String telefone,
        BigDecimal limiteDiarioPixPadrao,
        boolean isentoTarifaPix,
        boolean ativo,
        Instant criadoEm,
        EnderecoResponse endereco
) {
    public static ClienteResponse deDominio(Cliente cliente) {
        String razaoSocial = null;
        String nomeFantasia = null;
        LocalDate dataNascimento = null;

        if (cliente instanceof PessoaFisica pessoaFisica) {
            dataNascimento = pessoaFisica.getDataNascimento();
        } else if (cliente instanceof PessoaJuridica pessoaJuridica) {
            razaoSocial = pessoaJuridica.getRazaoSocial();
            nomeFantasia = pessoaJuridica.getNomeFantasia();
        }

        return new ClienteResponse(
                cliente.getId(),
                cliente.obterTipoCliente(),
                cliente.getNome(),
                cliente.getDocumentoFiscal(),
                razaoSocial,
                nomeFantasia,
                dataNascimento,
                cliente.getEmail(),
                cliente.getTelefone(),
                cliente.obterLimiteDiarioPixPadrao(),
                cliente.isIsentoTarifaPix(),
                cliente.isAtivo(),
                cliente.getCriadoEm(),
                EnderecoResponse.deDominio(cliente.getEndereco())
        );
    }
}
