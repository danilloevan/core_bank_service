package br.ufc.poo.corebank.domain.exception;

public class ContaInaptaParaOperacaoException extends DominioException {

    public ContaInaptaParaOperacaoException(String mensagem) {
        super(mensagem, "CONTA_INAPTA_PARA_OPERACAO", 422);
    }
}
