package br.ufc.poo.corebank.domain.exception;

public abstract class DominioException extends RuntimeException {

    private final String codigoErro;
    private final int statusHttpSugerido;

    protected DominioException(String mensagem, String codigoErro, int statusHttpSugerido) {
        super(mensagem);
        this.codigoErro = codigoErro;
        this.statusHttpSugerido = statusHttpSugerido;
    }

    public String getCodigoErro() {
        return codigoErro;
    }

    public int getStatusHttpSugerido() {
        return statusHttpSugerido;
    }
}
