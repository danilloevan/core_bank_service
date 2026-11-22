package br.ufc.poo.corebank.testsupport;

import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;
import br.ufc.poo.corebank.domain.model.ledger.VerificadorPartidasDobradas;
import br.ufc.poo.corebank.infrastructure.repository.LedgerRepository;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Mock
@ApplicationScoped
public class InMemoryLedgerRepository extends LedgerRepository {

    private final InMemoryCoreBankStore bancoEmMemoria;

    public InMemoryLedgerRepository(InMemoryCoreBankStore bancoEmMemoria) {
        this.bancoEmMemoria = bancoEmMemoria;
    }

    @Override
    public Uni<BigDecimal> calcularSaldoAtualPorContaId(UUID contaId) {
        List<LancamentoContabil> lancamentosDaConta = listarLancamentosDaContaInterno(contaId);
        return Uni.createFrom().item(VerificadorPartidasDobradas.calcularSaldoDeLancamentos(lancamentosDaConta));
    }

    @Override
    public Uni<BigDecimal> calcularSaldoAteInstante(UUID contaId, Instant instanteCorte) {
        List<LancamentoContabil> lancamentosDaConta = listarLancamentosDaContaInterno(contaId);
        return Uni.createFrom().item(VerificadorPartidasDobradas.calcularSaldoAteDataCorte(lancamentosDaConta, instanteCorte));
    }

    @Override
    public Uni<BigDecimal> calcularSaldoAntesDeInstante(UUID contaId, Instant instanteInicioPeriodo) {
        List<LancamentoContabil> lancamentosAnteriores = listarLancamentosDaContaInterno(contaId).stream()
                .filter(lancamento -> lancamento.getCriadoEm().isBefore(instanteInicioPeriodo))
                .toList();
        return Uni.createFrom().item(VerificadorPartidasDobradas.calcularSaldoDeLancamentos(lancamentosAnteriores));
    }

    @Override
    public Uni<BigDecimal> somarPorContaETipo(UUID contaId, TipoLancamento tipoLancamento) {
        BigDecimal soma = listarLancamentosDaContaInterno(contaId).stream()
                .filter(lancamento -> lancamento.getTipoLancamento() == tipoLancamento)
                .map(LancamentoContabil::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Uni.createFrom().item(LancamentoContabil.normalizarValorMonetario(soma));
    }

    @Override
    public Uni<Long> contarLancamentosPorContaId(UUID contaId) {
        long quantidade = listarLancamentosDaContaInterno(contaId).size();
        return Uni.createFrom().item(quantidade);
    }

    @Override
    public Uni<List<LancamentoContabil>> listarPorContaIdOrdenadoPorData(UUID contaId) {
        return Uni.createFrom().item(listarLancamentosDaContaInterno(contaId));
    }

    @Override
    public Uni<List<LancamentoContabil>> listarPorContaEPeriodo(
            UUID contaId,
            Instant instanteInicioOpcional,
            Instant instanteFimOpcional,
            TipoLancamento filtroTipoLancamentoOpcional) {
        List<LancamentoContabil> filtrados = listarLancamentosDaContaInterno(contaId).stream()
                .filter(lancamento -> instanteInicioOpcional == null || !lancamento.getCriadoEm().isBefore(instanteInicioOpcional))
                .filter(lancamento -> instanteFimOpcional == null || !lancamento.getCriadoEm().isAfter(instanteFimOpcional))
                .filter(lancamento -> filtroTipoLancamentoOpcional == null || lancamento.getTipoLancamento() == filtroTipoLancamentoOpcional)
                .toList();
        return Uni.createFrom().item(filtrados);
    }

    @Override
    public Uni<List<LancamentoContabil>> listarPorTransacaoId(UUID transacaoId) {
        List<LancamentoContabil> lancamentosDaTransacao = bancoEmMemoria.getLancamentosContabeis().stream()
                .filter(lancamento -> transacaoId.equals(lancamento.getTransacaoId()))
                .toList();
        return Uni.createFrom().item(lancamentosDaTransacao);
    }

    @Override
    public Uni<LancamentoContabil> persist(LancamentoContabil lancamento) {
        InMemoryCoreBankStore.garantirIdGerado(lancamento, LancamentoContabil.class);
        bancoEmMemoria.getLancamentosContabeis().add(lancamento);
        return Uni.createFrom().item(lancamento);
    }

    @Override
    public Uni<Void> persist(Iterable<LancamentoContabil> lancamentos) {
        for (LancamentoContabil lancamentoAtual : lancamentos) {
            InMemoryCoreBankStore.garantirIdGerado(lancamentoAtual, LancamentoContabil.class);
            bancoEmMemoria.getLancamentosContabeis().add(lancamentoAtual);
        }
        return Uni.createFrom().voidItem();
    }

    private List<LancamentoContabil> listarLancamentosDaContaInterno(UUID contaId) {
        return bancoEmMemoria.getLancamentosContabeis().stream()
                .filter(lancamento -> lancamento.getConta() != null && contaId.equals(lancamento.getConta().getId()))
                .sorted(Comparator.comparing(LancamentoContabil::getCriadoEm))
                .toList();
    }
}
