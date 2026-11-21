package br.ufc.poo.corebank.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CadastrarPessoaJuridicaRequest(
        @NotBlank(message = "A razão social é obrigatória.")
        String razaoSocial,

        String nomeFantasia,

        @NotBlank(message = "O CNPJ é obrigatório.")
        String cnpj,

        @NotBlank(message = "O e-mail corporativo é obrigatório.")
        @Email(message = "O e-mail corporativo informado é inválido.")
        String email,

        @NotBlank(message = "O telefone comercial é obrigatório.")
        String telefone,

        @NotNull(message = "O endereço da sede é obrigatório.")
        @Valid
        EnderecoRequest endereco
) {
}
