package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.conta.StatusConta;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusContaRequest(
        @NotNull(message = "O novo status da conta é obrigatório (ATIVA, BLOQUEADA ou ENCERRADA).")
        StatusConta novoStatus
) {
}
