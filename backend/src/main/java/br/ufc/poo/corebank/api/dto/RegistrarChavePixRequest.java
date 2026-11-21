package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.conta.TipoChavePix;
import jakarta.validation.constraints.NotNull;

public record RegistrarChavePixRequest(
        @NotNull(message = "O tipo da chave PIX é obrigatório (CPF, CNPJ, EMAIL, TELEFONE ou ALEATORIA).")
        TipoChavePix tipoChave,

        String valorChave
) {
}
