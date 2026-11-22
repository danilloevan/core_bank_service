package br.ufc.poo.corebank.testsupport;

import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.transacao.StatusTransacao;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;
import br.ufc.poo.corebank.infrastructure.repository.TransacaoRepository;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Mock
@ApplicationScoped
public class InMemoryTransacaoRepository extends TransacaoRepository {

    private final InMemoryCoreBankStore bancoEmMemoria;

    public InMemoryTransacaoRepository(InMemoryCoreBankStore bancoEmMemoria) {
        this.bancoEmMemoria = bancoEmMemoria;
    }

    @Override
    public Uni<Transacao> buscarPorId(UUID transacaoId) {
        return Uni.createFrom().item(bancoEmMemoria.getTransacoesPorId().get(transacaoId));
    }

    @Override
    public Uni<Transacao> buscarPorChaveIdempotencia(String chaveIdempotencia) {
        Transacao transacaoEncontrada = bancoEmMemoria.getTransacoesPorId().values().stream()
                .filter(transacao -> transacao.getChaveIdempotencia().equals(chaveIdempotencia))
                .findFirst()
                .orElse(null);
        return Uni.createFrom().item(transacaoEncontrada);
    }

    @Override
    public Uni<BigDecimal> calcularTotalMovimentadoNoPeriodo(
            UUID contaOrigemId,
            Class<? extends Transacao> classeTipoTransacao,
            Instant inicioPeriodo,
            Instant fimPeriodo) {
        BigDecimal totalMovimentado = bancoEmMemoria.getTransacoesPorId().values().stream()
                .filter(classeTipoTransacao::isInstance)
                .filter(transacao -> transacao.getContaOrigem() != null
                        && contaOrigemId.equals(transacao.getContaOrigem().getId()))
                .filter(transacao -> transacao.getStatusTransacao() == StatusTransacao.CONCLUIDA)
                .filter(transacao -> transacao.getProcessadaEm() != null
                        && !transacao.getProcessadaEm().isBefore(inicioPeriodo)
                        && !transacao.getProcessadaEm().isAfter(fimPeriodo))
                .map(Transacao::getValorOperacao)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Uni.createFrom().item(LancamentoContabil.normalizarValorMonetario(totalMovimentado));
    }

    @Override
    public Uni<Transacao> persist(Transacao transacao) {
        InMemoryCoreBankStore.garantirIdGerado(transacao, Transacao.class);
        bancoEmMemoria.getTransacoesPorId().put(transacao.getId(), transacao);
        return Uni.createFrom().item(transacao);
    }
}
