package br.ufc.poo.corebank.domain.validator;

import br.ufc.poo.corebank.domain.model.transacao.ContextoTransacao;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;

public interface ValidadorTransacao {

    void validar(Transacao transacao, ContextoTransacao contextoTransacao);
}
