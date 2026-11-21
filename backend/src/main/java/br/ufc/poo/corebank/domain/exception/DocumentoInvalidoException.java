package br.ufc.poo.corebank.domain.exception;

public class DocumentoInvalidoException extends DominioException {

    public DocumentoInvalidoException(String mensagem) {
        super(mensagem, "DOCUMENTO_INVALIDO", 422);
    }
}
