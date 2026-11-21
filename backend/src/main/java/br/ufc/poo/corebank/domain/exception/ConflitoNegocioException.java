package br.ufc.poo.corebank.domain.exception;

public class ConflitoNegocioException extends DominioException {

    public ConflitoNegocioException(String mensagem) {
        super(mensagem, "CONFLITO_NEGOCIO", 409);
    }
}
