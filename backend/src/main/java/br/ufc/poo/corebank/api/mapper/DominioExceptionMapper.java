package br.ufc.poo.corebank.api.mapper;

import br.ufc.poo.corebank.api.dto.ErroPadraoResponse;
import br.ufc.poo.corebank.domain.exception.DominioException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;

@Provider
public class DominioExceptionMapper implements ExceptionMapper<DominioException> {

    @Override
    public Response toResponse(DominioException excecaoDominio) {
        ErroPadraoResponse erroResponse = new ErroPadraoResponse(
                excecaoDominio.getCodigoErro(),
                excecaoDominio.getMessage(),
                excecaoDominio.getStatusHttpSugerido(),
                Instant.now()
        );

        return Response.status(excecaoDominio.getStatusHttpSugerido())
                .type(MediaType.APPLICATION_JSON)
                .entity(erroResponse)
                .build();
    }
}
