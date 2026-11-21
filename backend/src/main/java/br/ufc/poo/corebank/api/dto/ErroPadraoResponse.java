package br.ufc.poo.corebank.api.dto;

import java.time.Instant;

public record ErroPadraoResponse(
        String codigoErro,
        String mensagem,
        int statusHttp,
        Instant timestamp
) {
}
