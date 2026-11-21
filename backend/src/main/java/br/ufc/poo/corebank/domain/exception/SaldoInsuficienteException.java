package br.ufc.poo.corebank.domain.exception;

public class SaldoInsuficienteException extends DominioException {

    public SaldoInsuficienteException(String mensagem) {
        super(mensagem, "SALDO_INSUFICIENTE", 422);
    }
}
