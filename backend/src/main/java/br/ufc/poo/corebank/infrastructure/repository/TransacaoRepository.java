package br.ufc.poo.corebank.infrastructure.repository;

import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.transacao.StatusTransacao;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class TransacaoRepository implements PanacheRepositoryBase<Transacao, UUID> {

    public Uni<Transacao> buscarPorId(UUID transacaoId) {
        return findById(transacaoId);
    }

    public Uni<Transacao> buscarPorChaveIdempotencia(String chaveIdempotencia) {
        return find("chaveIdempotencia", chaveIdempotencia).firstResult();
    }

    public Uni<BigDecimal> calcularTotalMovimentadoNoPeriodo(
            UUID contaOrigemId,
            Class<? extends Transacao> classeTipoTransacao,
            Instant inicioPeriodo,
            Instant fimPeriodo) {
        String consultaHql = "SELECT COALESCE(SUM(t.valorOperacao), 0) FROM "
                + classeTipoTransacao.getSimpleName()
                + " t WHERE t.contaOrigem.id = :contaOrigemId "
                + "AND t.statusTransacao = :statusConcluida "
                + "AND t.processadaEm >= :inicioPeriodo AND t.processadaEm <= :fimPeriodo";

        return getSession()
                .onItem().transformToUni(sessaoHibernate ->
                        sessaoHibernate.createSelectionQuery(consultaHql, BigDecimal.class)
                                .setParameter("contaOrigemId", contaOrigemId)
                                .setParameter("statusConcluida", StatusTransacao.CONCLUIDA)
                                .setParameter("inicioPeriodo", inicioPeriodo)
                                .setParameter("fimPeriodo", fimPeriodo)
                                .getSingleResult()
                )
                .onItem().transform(LancamentoContabil::normalizarValorMonetario);
    }
}
