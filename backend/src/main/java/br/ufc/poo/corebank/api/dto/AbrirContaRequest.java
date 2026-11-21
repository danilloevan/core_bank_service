package br.ufc.poo.corebank.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AbrirContaRequest(
        @NotNull(message = "O ID do cliente titular é obrigatório.")
        UUID clienteTitularId,

        String codigoAgencia
) {
}
