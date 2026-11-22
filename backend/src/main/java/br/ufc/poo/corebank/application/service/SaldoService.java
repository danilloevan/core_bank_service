package br.ufc.poo.corebank.application.service;

import br.ufc.poo.corebank.domain.exception.RecursoNaoEncontradoException;
import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.ledger.ResumoSaldoConta;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;
import br.ufc.poo.corebank.domain.model.ledger.VerificadorPartidasDobradas;
import br.ufc.poo.corebank.infrastructure.repository.ContaRepository;
import br.ufc.poo.corebank.infrastructure.repository.LedgerRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SaldoService {

        private final ContaRepository contaRepository;
        private final LedgerRepository ledgerRepository;

        public SaldoService(ContaRepository contaRepository, LedgerRepository ledgerRepository) {
                this.contaRepository = contaRepository;
                this.ledgerRepository = ledgerRepository;
        }

        @WithSession
        public Uni<BigDecimal> calcularSaldoDisponivel(UUID contaId) {
                return verificarContaExistente(contaId)
                                .onItem().transformToUni(contaEncontrada -> ledgerRepository
                                                .calcularSaldoAtualPorContaId(contaEncontrada.getId()));
        }

        @WithSession
        public Uni<BigDecimal> calcularSaldoNaDataCorte(UUID contaId, Instant instanteCorte) {
                if (instanteCorte == null) {
                        return Uni.createFrom().failure(new RegraNegocioException("A data de corte é obrigatória."));
                }
                return verificarContaExistente(contaId)
                                .onItem().transformToUni(contaEncontrada -> ledgerRepository
                                                .calcularSaldoAteInstante(contaEncontrada.getId(), instanteCorte));
        }

        @WithSession
        public Uni<ResumoSaldoConta> consultarResumoSaldo(UUID contaId) {
                return verificarContaExistente(contaId)
                                .onItem()
                                .transformToUni(contaEncontrada -> ledgerRepository
                                                .somarPorContaETipo(contaId, TipoLancamento.CREDITO)
                                                .onItem()
                                                .transformToUni(totalCreditosBruto -> ledgerRepository
                                                                .somarPorContaETipo(contaId, TipoLancamento.DEBITO)
                                                                .onItem()
                                                                .transformToUni(totalDebitosBruto -> ledgerRepository
                                                                                .contarLancamentosPorContaId(contaId)
                                                                                .onItem()
                                                                                .transform(quantidadeLancamentos -> {
                                                                                        BigDecimal totalCreditos = LancamentoContabil
                                                                                                        .normalizarValorMonetario(
                                                                                                                        totalCreditosBruto);
                                                                                        BigDecimal totalDebitos = LancamentoContabil
                                                                                                        .normalizarValorMonetario(
                                                                                                                        totalDebitosBruto);
                                                                                        BigDecimal saldoDisponivel = LancamentoContabil
                                                                                                        .normalizarValorMonetario(
                                                                                                                        totalCreditos.subtract(
                                                                                                                                        totalDebitos));

                                                                                        return new ResumoSaldoConta(
                                                                                                        contaEncontrada.getId(),
                                                                                                        contaEncontrada.getNumeroConta(),
                                                                                                        saldoDisponivel,
                                                                                                        totalCreditos,
                                                                                                        totalDebitos,
                                                                                                        quantidadeLancamentos,
                                                                                                        Instant.now());
                                                                                }))));
        }

        @WithTransaction
        public Uni<List<LancamentoContabil>> registrarPartidasDobradas(
                        List<LancamentoContabil> lancamentosParaPersistir) {
                VerificadorPartidasDobradas.validarEquilibrioPartidasDobradas(lancamentosParaPersistir);
                return ledgerRepository.persist(lancamentosParaPersistir)
                                .replaceWith(lancamentosParaPersistir);
        }

        private Uni<br.ufc.poo.corebank.domain.model.conta.Conta> verificarContaExistente(UUID contaId) {
                return contaRepository.buscarPorId(contaId)
                                .onItem().ifNull().failWith(() -> new RecursoNaoEncontradoException(
                                                "Conta bancária não encontrada para o ID: " + contaId));
        }
}
