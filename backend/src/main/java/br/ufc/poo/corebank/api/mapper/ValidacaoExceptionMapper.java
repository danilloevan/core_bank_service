package br.ufc.poo.corebank.api.mapper;

import br.ufc.poo.corebank.api.dto.ErroPadraoResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.stream.Collectors;

@Provider
public class ValidacaoExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException excecaoValidacao) {
        String mensagemConsolidada = excecaoValidacao.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(" | "));

        ErroPadraoResponse erroResponse = new ErroPadraoResponse(
                "ERRO_VALIDACAO_ENTRADA",
                mensagemConsolidada.isBlank() ? "Dados de entrada inválidos." : mensagemConsolidada,
                Response.Status.BAD_REQUEST.getStatusCode(),
                Instant.now()
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(erroResponse)
                .build();
    }
}
