package br.ufc.poo.corebank.domain.validator;

import br.ufc.poo.corebank.domain.exception.JanelaHorarioInvalidaException;
import br.ufc.poo.corebank.domain.model.conta.TipoOperacao;
import br.ufc.poo.corebank.domain.model.transacao.ContextoTransacao;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoTed;

import java.time.LocalTime;

public class ValidadorJanelaHorarioTed implements ValidadorTransacao {

    @Override
    public void validar(Transacao transacao, ContextoTransacao contextoTransacao) {
        if (transacao.obterTipoOperacao() != TipoOperacao.TED) {
            return;
        }
        if (!contextoTransacao.isValidarJanelaHorarioTed()) {
            return;
        }

        LocalTime horarioReferencia = contextoTransacao.getDataHoraReferencia().toLocalTime();
        if (!TransacaoTed.isDentroDaJanelaOperacionalTed(horarioReferencia)) {
            throw new JanelaHorarioInvalidaException(
                    "Operações TED só podem ser liquidadas entre "
                            + TransacaoTed.HORARIO_ABERTURA_JANELA_TED + " e "
                            + TransacaoTed.HORARIO_LIMITE_TED + " (horário da tentativa: " + horarioReferencia + ")."
            );
        }
    }
}
