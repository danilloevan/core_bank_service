package br.ufc.poo.corebank.domain.validator;

import br.ufc.poo.corebank.domain.exception.ContaInaptaParaOperacaoException;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.TipoOperacao;
import br.ufc.poo.corebank.domain.model.transacao.ContextoTransacao;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;

public class ValidadorStatusConta implements ValidadorTransacao {

    @Override
    public void validar(Transacao transacao, ContextoTransacao contextoTransacao) {
        TipoOperacao tipoOperacao = transacao.obterTipoOperacao();
        Conta contaOrigem = transacao.getContaOrigem();
        Conta contaDestino = transacao.getContaDestino();

        if (contaOrigem != null) {
            if (!contaOrigem.isAtiva()) {
                throw new ContaInaptaParaOperacaoException(
                        "A conta de origem " + contaOrigem.getNumeroConta()
                                + " não está ativa (status atual: " + contaOrigem.getStatusConta() + ")."
                );
            }
            if (!contaOrigem.podeRealizarOperacao(tipoOperacao)) {
                throw new ContaInaptaParaOperacaoException(
                        "A conta de origem " + contaOrigem.getNumeroConta()
                                + " (" + contaOrigem.obterTipoConta() + ") não está habilitada para a operação " + tipoOperacao + "."
                );
            }
        }

        if (contaDestino != null) {
            if (!contaDestino.isAtiva()) {
                throw new ContaInaptaParaOperacaoException(
                        "A conta de destino " + contaDestino.getNumeroConta()
                                + " não está ativa (status atual: " + contaDestino.getStatusConta() + ")."
                );
            }
            if (!contaDestino.podeRealizarOperacao(TipoOperacao.DEPOSITO)) {
                throw new ContaInaptaParaOperacaoException(
                        "A conta de destino " + contaDestino.getNumeroConta()
                                + " (" + contaDestino.obterTipoConta() + ") não pode receber créditos diretos de clientes."
                );
            }
        }
    }
}
