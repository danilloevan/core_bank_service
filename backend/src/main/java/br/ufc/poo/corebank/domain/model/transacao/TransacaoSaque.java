package br.ufc.poo.corebank.domain.model.transacao;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.TipoOperacao;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;
import br.ufc.poo.corebank.domain.strategy.EstrategiaTarifa;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.util.List;

@Entity
@DiscriminatorValue("SAQUE")
public class TransacaoSaque extends Transacao {

    @Column(name = "codigo_terminal_atm", length = 50)
    private String codigoTerminalAtm;

    protected TransacaoSaque() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public TransacaoSaque(
            String chaveIdempotencia,
            Conta contaOrigem,
            BigDecimal valorSaque,
            String codigoTerminalAtm,
            EstrategiaTarifa estrategiaTarifa) {
        super(chaveIdempotencia, valorSaque, contaOrigem, null, estrategiaTarifa);
        if (contaOrigem == null) {
            throw new RegraNegocioException("A conta de origem do saque é obrigatória.");
        }
        if (codigoTerminalAtm == null || codigoTerminalAtm.isBlank()) {
            throw new RegraNegocioException("O código do terminal ATM do saque é obrigatório.");
        }
        this.codigoTerminalAtm = codigoTerminalAtm.trim();
    }

    @Override
    public TipoOperacao obterTipoOperacao() {
        return TipoOperacao.SAQUE;
    }

    @Override
    protected void validarRegrasEspecificas(ContextoTransacao contextoTransacao) {
        if (contextoTransacao.getContaCaixaTesouraria() == null) {
            throw new RegraNegocioException(
                    "A Conta Técnica de Caixa/Tesouraria é obrigatória como contrapartida contábil do saque."
            );
        }
    }

    @Override
    protected List<LancamentoContabil> gerarLancamentosEspecificos(ContextoTransacao contextoTransacao) {
        LancamentoContabil debitoContaCliente = new LancamentoContabil(
                getId(),
                contaOrigem,
                TipoLancamento.DEBITO,
                valorOperacao,
                "Saque em terminal ATM [" + codigoTerminalAtm + "]"
        );

        LancamentoContabil creditoCaixaTesouraria = new LancamentoContabil(
                getId(),
                contextoTransacao.getContaCaixaTesouraria(),
                TipoLancamento.CREDITO,
                valorOperacao,
                "Baixa de numerário ATM [" + codigoTerminalAtm + "] conta " + contaOrigem.getNumeroConta()
        );

        return List.of(debitoContaCliente, creditoCaixaTesouraria);
    }

    public String getCodigoTerminalAtm() {
        return codigoTerminalAtm;
    }
}
