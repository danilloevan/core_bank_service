package br.ufc.poo.corebank.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SaqueRequest(
        @NotNull(message = "O ID da conta de origem do saque é obrigatório.")
        UUID contaOrigemId,

        @NotNull(message = "O valor do saque é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor do saque deve ser de no mínimo R$ 0,01.")
        BigDecimal valor,

        @NotBlank(message = "O código do terminal ATM do saque é obrigatório.")
        String codigoTerminalAtm
) {
}
