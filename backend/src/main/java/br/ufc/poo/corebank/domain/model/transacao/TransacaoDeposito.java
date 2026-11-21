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
@DiscriminatorValue("DEPOSITO")
public class TransacaoDeposito extends Transacao {

    @Column(name = "canal_origem_deposito", length = 80)
    private String canalOrigemDeposito;

    protected TransacaoDeposito() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public TransacaoDeposito(
            String chaveIdempotencia,
            Conta contaDestino,
            BigDecimal valorDeposito,
            String canalOrigemDeposito,
            EstrategiaTarifa estrategiaTarifa) {
        super(chaveIdempotencia, valorDeposito, null, contaDestino, estrategiaTarifa);
        if (contaDestino == null) {
            throw new RegraNegocioException("A conta de destino do depósito é obrigatória.");
        }
        if (canalOrigemDeposito == null || canalOrigemDeposito.isBlank()) {
            throw new RegraNegocioException("O canal de origem do depósito é obrigatório.");
        }
        this.canalOrigemDeposito = canalOrigemDeposito.trim();
    }

    @Override
    public TipoOperacao obterTipoOperacao() {
        return TipoOperacao.DEPOSITO;
    }

    @Override
    protected void validarRegrasEspecificas(ContextoTransacao contextoTransacao) {
        if (contextoTransacao.getContaCaixaTesouraria() == null) {
            throw new RegraNegocioException(
                    "A Conta Técnica de Caixa/Tesouraria é obrigatória como contrapartida do depósito."
            );
        }
    }

    @Override
    protected List<LancamentoContabil> gerarLancamentosEspecificos(ContextoTransacao contextoTransacao) {
        String descricaoContabil = "Depósito via " + canalOrigemDeposito
                + " para conta " + contaDestino.getNumeroConta();

        LancamentoContabil debitoCaixaTesouraria = new LancamentoContabil(
                getId(),
                contextoTransacao.getContaCaixaTesouraria(),
                TipoLancamento.DEBITO,
                valorOperacao,
                "Contrapartida de " + descricaoContabil
        );

        LancamentoContabil creditoContaCliente = new LancamentoContabil(
                getId(),
                contaDestino,
                TipoLancamento.CREDITO,
                valorOperacao,
                descricaoContabil
        );

        return List.of(debitoCaixaTesouraria, creditoContaCliente);
    }

    public String getCanalOrigemDeposito() {
        return canalOrigemDeposito;
    }
}
