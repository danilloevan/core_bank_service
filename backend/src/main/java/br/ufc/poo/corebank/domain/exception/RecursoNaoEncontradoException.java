package br.ufc.poo.corebank.domain.exception;

public class RecursoNaoEncontradoException extends DominioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem, "RECURSO_NAO_ENCONTRADO", 404);
    }
}
