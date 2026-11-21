package br.ufc.poo.corebank.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferenciaTedRequest(
        @NotNull(message = "O ID da conta de origem da TED é obrigatório.")
        UUID contaOrigemId,

        @NotBlank(message = "O código do banco ou ISPB de destino da TED é obrigatório.")
        String codigoBancoDestino,

        @NotBlank(message = "O código da agência de destino da TED é obrigatório.")
        String codigoAgenciaDestino,

        @NotBlank(message = "O número da conta de destino da TED é obrigatório.")
        String numeroContaDestino,

        @NotBlank(message = "O CPF ou CNPJ do favorecido da TED é obrigatório.")
        String documentoFavorecido,

        @NotNull(message = "O valor da TED é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor da TED deve ser de no mínimo R$ 0,01.")
        BigDecimal valor
) {
}
