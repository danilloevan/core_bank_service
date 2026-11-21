package br.ufc.poo.corebank.infrastructure.repository;

import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LedgerRepository implements PanacheRepositoryBase<LancamentoContabil, UUID> {

    public Uni<BigDecimal> calcularSaldoAtualPorContaId(UUID contaId) {
        return getSession()
                .onItem().transformToUni(sessaoHibernate ->
                        sessaoHibernate.createSelectionQuery(
                                        "SELECT COALESCE(SUM(CASE WHEN l.tipoLancamento = :tipoCredito THEN l.valor ELSE -l.valor END), 0) "
                                                + "FROM LancamentoContabil l WHERE l.conta.id = :contaId",
                                        BigDecimal.class)
                                .setParameter("tipoCredito", TipoLancamento.CREDITO)
                                .setParameter("contaId", contaId)
                                .getSingleResult()
                )
                .onItem().transform(LancamentoContabil::normalizarValorMonetario);
    }

    public Uni<BigDecimal> calcularSaldoAteInstante(UUID contaId, Instant instanteCorte) {
        return getSession()
                .onItem().transformToUni(sessaoHibernate ->
                        sessaoHibernate.createSelectionQuery(
                                        "SELECT COALESCE(SUM(CASE WHEN l.tipoLancamento = :tipoCredito THEN l.valor ELSE -l.valor END), 0) "
                                                + "FROM LancamentoContabil l WHERE l.conta.id = :contaId AND l.criadoEm <= :instanteCorte",
                                        BigDecimal.class)
                                .setParameter("tipoCredito", TipoLancamento.CREDITO)
                                .setParameter("contaId", contaId)
                                .setParameter("instanteCorte", instanteCorte)
                                .getSingleResult()
                )
                .onItem().transform(LancamentoContabil::normalizarValorMonetario);
    }

    public Uni<BigDecimal> somarPorContaETipo(UUID contaId, TipoLancamento tipoLancamento) {
        return getSession()
                .onItem().transformToUni(sessaoHibernate ->
                        sessaoHibernate.createSelectionQuery(
                                        "SELECT COALESCE(SUM(l.valor), 0) "
                                                + "FROM LancamentoContabil l WHERE l.conta.id = :contaId AND l.tipoLancamento = :tipoLancamento",
                                        BigDecimal.class)
                                .setParameter("contaId", contaId)
                                .setParameter("tipoLancamento", tipoLancamento)
                                .getSingleResult()
                )
                .onItem().transform(LancamentoContabil::normalizarValorMonetario);
    }

    public Uni<Long> contarLancamentosPorContaId(UUID contaId) {
        return count("conta.id", contaId);
    }

    public Uni<List<LancamentoContabil>> listarPorContaIdOrdenadoPorData(UUID contaId) {
        return list("conta.id = ?1 order by criadoEm asc", contaId);
    }

    public Uni<List<LancamentoContabil>> listarPorTransacaoId(UUID transacaoId) {
        return list("transacaoId", transacaoId);
    }
}
