package br.ufc.poo.corebank.application.service;

import br.ufc.poo.corebank.domain.exception.RecursoNaoEncontradoException;
import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.export.ExportadorExtrato;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.ledger.ExtratoConta;
import br.ufc.poo.corebank.domain.model.ledger.ItemExtratoConta;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;
import br.ufc.poo.corebank.domain.model.transacao.ContextoTransacao;
import br.ufc.poo.corebank.infrastructure.repository.ContaRepository;
import br.ufc.poo.corebank.infrastructure.repository.LedgerRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ExtratoService {

    private final ContaRepository contaRepository;
    private final LedgerRepository ledgerRepository;

    public ExtratoService(ContaRepository contaRepository, LedgerRepository ledgerRepository) {
        this.contaRepository = contaRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @WithSession
    public Uni<ExtratoConta> emitirExtrato(
            UUID contaId,
            LocalDate dataInicioOpcional,
            LocalDate dataFimOpcional,
            TipoLancamento filtroTipoLancamentoOpcional) {
        if (dataInicioOpcional != null && dataFimOpcional != null && dataInicioOpcional.isAfter(dataFimOpcional)) {
            return Uni.createFrom().failure(
                    new RegraNegocioException("A data inicial do filtro de extrato não pode ser posterior à data final.")
            );
        }

        Instant instanteInicio = (dataInicioOpcional != null)
                ? dataInicioOpcional.atStartOfDay(ContextoTransacao.FUSO_HORARIO_BANCARIO_PADRAO).toInstant()
                : null;
        Instant instanteFim = (dataFimOpcional != null)
                ? dataFimOpcional.atTime(LocalTime.MAX).atZone(ContextoTransacao.FUSO_HORARIO_BANCARIO_PADRAO).toInstant()
                : null;

        return buscarContaObrigatoria(contaId)
                .onItem().transformToUni(contaEncontrada -> {
                    Uni<BigDecimal> saldoAnteriorUni = (instanteInicio != null)
                            ? ledgerRepository.calcularSaldoAntesDeInstante(contaEncontrada.getId(), instanteInicio)
                            : Uni.createFrom().item(LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO));

                    return saldoAnteriorUni.onItem().transformToUni(saldoAnterior ->
                            ledgerRepository.listarPorContaEPeriodo(
                                            contaEncontrada.getId(),
                                            instanteInicio,
                                            instanteFim,
                                            filtroTipoLancamentoOpcional
                                    )
                                    .onItem().transform(lancamentosNoPeriodo ->
                                            ExtratoConta.construirExtrato(
                                                    contaEncontrada,
                                                    instanteInicio,
                                                    instanteFim,
                                                    saldoAnterior,
                                                    lancamentosNoPeriodo
                                            )
                                    )
                    );
                });
    }

    public Multi<ItemExtratoConta> streamItensExtrato(
            UUID contaId,
            LocalDate dataInicioOpcional,
            LocalDate dataFimOpcional,
            TipoLancamento filtroTipoLancamentoOpcional) {
        return emitirExtrato(contaId, dataInicioOpcional, dataFimOpcional, filtroTipoLancamentoOpcional)
                .onItem().transformToMulti(extratoEmitido ->
                        Multi.createFrom().iterable(extratoEmitido.getMovimentacoes())
                );
    }

    @WithSession
    public Uni<String> exportarExtrato(
            UUID contaId,
            LocalDate dataInicioOpcional,
            LocalDate dataFimOpcional,
            TipoLancamento filtroTipoLancamentoOpcional,
            ExportadorExtrato exportadorExtrato) {
        if (exportadorExtrato == null) {
            return Uni.createFrom().failure(new RegraNegocioException("O exportador de extrato é obrigatório."));
        }
        return emitirExtrato(contaId, dataInicioOpcional, dataFimOpcional, filtroTipoLancamentoOpcional)
                .onItem().transform(exportadorExtrato::exportar);
    }

    private Uni<Conta> buscarContaObrigatoria(UUID contaId) {
        return contaRepository.buscarPorId(contaId)
                .onItem().ifNull().failWith(() ->
                        new RecursoNaoEncontradoException("Conta bancária não encontrada para o ID: " + contaId)
                );
    }
}
