package br.ufc.poo.corebank.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CadastrarPessoaFisicaRequest(
        @NotBlank(message = "O nome completo é obrigatório.")
        String nomeCompleto,

        @NotBlank(message = "O CPF é obrigatório.")
        String cpf,

        @NotNull(message = "A data de nascimento é obrigatória.")
        LocalDate dataNascimento,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        String email,

        @NotBlank(message = "O telefone é obrigatório.")
        String telefone,

        @NotNull(message = "O endereço é obrigatório.")
        @Valid
        EnderecoRequest endereco
) {
}
