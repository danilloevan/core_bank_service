package br.ufc.poo.corebank.domain.exception;

public class RegraNegocioException extends DominioException {

    public RegraNegocioException(String mensagem) {
        super(mensagem, "VIOLACAO_REGRA_NEGOCIO", 422);
    }

    public RegraNegocioException(String mensagem, String codigoErro) {
        super(mensagem, codigoErro, 422);
    }
}
