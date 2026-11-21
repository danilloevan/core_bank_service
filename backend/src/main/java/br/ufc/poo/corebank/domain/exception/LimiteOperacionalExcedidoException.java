package br.ufc.poo.corebank.domain.exception;

public class LimiteOperacionalExcedidoException extends DominioException {

    public LimiteOperacionalExcedidoException(String mensagem) {
        super(mensagem, "LIMITE_OPERACIONAL_EXCEDIDO", 422);
    }
}
