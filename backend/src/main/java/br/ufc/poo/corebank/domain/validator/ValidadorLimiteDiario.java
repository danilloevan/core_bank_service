package br.ufc.poo.corebank.domain.validator;

import br.ufc.poo.corebank.domain.exception.LimiteOperacionalExcedidoException;
import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.TipoOperacao;
import br.ufc.poo.corebank.domain.model.transacao.ContextoTransacao;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;

import java.math.BigDecimal;

public class ValidadorLimiteDiario implements ValidadorTransacao {

    @Override
    public void validar(Transacao transacao, ContextoTransacao contextoTransacao) {
        Conta contaOrigem = transacao.getContaOrigem();
        if (contaOrigem == null) {
            return;
        }

        TipoOperacao tipoOperacao = transacao.obterTipoOperacao();
        BigDecimal totalAcumuladoComNovaOperacao = contextoTransacao.getTotalMovimentadoNoDiaNaOperacao()
                .add(transacao.getValorOperacao());

        if (tipoOperacao == TipoOperacao.PIX && contaOrigem.getTitular() != null) {
            Cliente titularConta = contaOrigem.getTitular();
            BigDecimal limiteDiarioPixDoTitular = titularConta.obterLimiteDiarioPixPadrao();
            if (totalAcumuladoComNovaOperacao.compareTo(limiteDiarioPixDoTitular) > 0) {
                throw new LimiteOperacionalExcedidoException(
                        "Limite diário de PIX excedido para o perfil " + titularConta.obterTipoCliente()
                                + ". Limite diário: R$ " + limiteDiarioPixDoTitular
                                + " | Total projetado no dia: R$ " + totalAcumuladoComNovaOperacao + "."
                );
            }
        }

        if (tipoOperacao == TipoOperacao.SAQUE) {
            BigDecimal limiteSaqueDiarioDaConta = contaOrigem.obterLimiteSaqueDiario();
            if (totalAcumuladoComNovaOperacao.compareTo(limiteSaqueDiarioDaConta) > 0) {
                throw new LimiteOperacionalExcedidoException(
                        "Limite diário de saque excedido para " + contaOrigem.obterTipoConta()
                                + ". Limite diário: R$ " + limiteSaqueDiarioDaConta
                                + " | Total projetado no dia: R$ " + totalAcumuladoComNovaOperacao + "."
                );
            }
        }
    }
}
