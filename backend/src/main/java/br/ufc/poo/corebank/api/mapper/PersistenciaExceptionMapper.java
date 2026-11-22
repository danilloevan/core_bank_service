package br.ufc.poo.corebank.api.mapper;

import br.ufc.poo.corebank.api.dto.ErroPadraoResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;

import java.time.Instant;

@Provider
public class PersistenciaExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException excecaoBanco) {
        String mensagemAmigavel = traduzirMensagemConstraint(excecaoBanco);

        ErroPadraoResponse erroResponse = new ErroPadraoResponse(
                "CONFLITO_INTEGRIDADE_DADOS",
                mensagemAmigavel,
                Response.Status.CONFLICT.getStatusCode(),
                Instant.now()
        );

        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(erroResponse)
                .build();
    }

    private String traduzirMensagemConstraint(ConstraintViolationException excecaoBanco) {
        String detalheTecnico = excecaoBanco.getMessage() != null ? excecaoBanco.getMessage().toLowerCase() : "";
        if (detalheTecnico.contains("numero_conta") || detalheTecnico.contains("contas")) {
            return "Já existe uma conta cadastrada com o número informado. Tente novamente.";
        }
        if (detalheTecnico.contains("documento_fiscal") || detalheTecnico.contains("clientes")) {
            return "Já existe um cliente cadastrado com o CPF ou CNPJ informado.";
        }
        if (detalheTecnico.contains("valor_chave") || detalheTecnico.contains("chaves_pix")) {
            return "A chave PIX informada já está vinculada a outra conta.";
        }
        if (detalheTecnico.contains("chave_idempotencia") || detalheTecnico.contains("transacoes")) {
            return "Já existe uma transação registrada com esta chave de idempotência.";
        }
        return "Conflito de integridade: já existe um registro com os mesmos dados únicos informados.";
    }
}
