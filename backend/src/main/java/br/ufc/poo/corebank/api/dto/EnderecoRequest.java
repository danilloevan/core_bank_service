package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.cliente.Endereco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank(message = "O logradouro é obrigatório.")
        String logradouro,

        @NotBlank(message = "O número é obrigatório.")
        String numero,

        String complemento,

        @NotBlank(message = "O bairro é obrigatório.")
        String bairro,

        @NotBlank(message = "A cidade é obrigatória.")
        String cidade,

        @NotBlank(message = "A UF é obrigatória.")
        @Size(min = 2, max = 2, message = "A UF deve ter exatamente 2 caracteres.")
        String uf,

        @NotBlank(message = "O CEP é obrigatório.")
        String cep
) {
    public Endereco paraDominio() {
        return new Endereco(logradouro, numero, complemento, bairro, cidade, uf, cep);
    }
}
