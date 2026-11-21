package br.ufc.poo.corebank.domain.exception;

public class JanelaHorarioInvalidaException extends DominioException {

    public JanelaHorarioInvalidaException(String mensagem) {
        super(mensagem, "FORA_JANELA_HORARIO_OPERACIONAL", 422);
    }
}
