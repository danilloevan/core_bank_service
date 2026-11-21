package br.ufc.poo.corebank.application.service;

import br.ufc.poo.corebank.domain.exception.RecursoNaoEncontradoException;
import br.ufc.poo.corebank.domain.factory.TransacaoFactory;
import br.ufc.poo.corebank.domain.model.conta.ChavePix;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.ContaTecnica;
import br.ufc.poo.corebank.domain.model.conta.TipoContaTecnica;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.transacao.ContextoTransacao;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoDeposito;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoPix;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoSaque;
import br.ufc.poo.corebank.domain.model.transacao.TransacaoTed;
import br.ufc.poo.corebank.domain.validator.ValidadorTransacao;
import br.ufc.poo.corebank.infrastructure.bootstrap.CoreBankBootstrap;
import br.ufc.poo.corebank.infrastructure.repository.ChavePixRepository;
import br.ufc.poo.corebank.infrastructure.repository.ContaRepository;
import br.ufc.poo.corebank.infrastructure.repository.LedgerRepository;
import br.ufc.poo.corebank.infrastructure.repository.TransacaoRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MotorTransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final ContaRepository contaRepository;
    private final ChavePixRepository chavePixRepository;
    private final LedgerRepository ledgerRepository;
    private final CoreBankBootstrap coreBankBootstrap;
    private final boolean validarJanelaHorarioTed;
    private final List<ValidadorTransacao> cadeiaValidadoresPadrao;

    public MotorTransacaoService(
            TransacaoRepository transacaoRepository,
            ContaRepository contaRepository,
            ChavePixRepository chavePixRepository,
            LedgerRepository ledgerRepository,
            CoreBankBootstrap coreBankBootstrap,
            @ConfigProperty(name = "corebank.ted.validar-janela-horario", defaultValue = "false")
            boolean validarJanelaHorarioTed) {
        this.transacaoRepository = transacaoRepository;
        this.contaRepository = contaRepository;
        this.chavePixRepository = chavePixRepository;
        this.ledgerRepository = ledgerRepository;
        this.coreBankBootstrap = coreBankBootstrap;
        this.validarJanelaHorarioTed = validarJanelaHorarioTed;
        this.cadeiaValidadoresPadrao = TransacaoFactory.criarCadeiaValidadoresPadrao();
    }

    @WithTransaction
    public Uni<Transacao> realizarDeposito(
            String chaveIdempotenciaOpcional,
            UUID contaDestinoId,
            BigDecimal valorDeposito,
            String canalOrigemDeposito) {
        String chaveIdempotencia = resolverChaveIdempotencia(chaveIdempotenciaOpcional);

        return transacaoRepository.buscarPorChaveIdempotencia(chaveIdempotencia)
                .onItem().ifNotNull().transformToUni(transacaoExistente -> Uni.createFrom().item(transacaoExistente))
                .onItem().ifNull().switchTo(() ->
                        coreBankBootstrap.garantirInfraestruturaContabilBasica()
                                .onItem().transformToUni(agenciaMatriz ->
                                        buscarContaComBloqueioObrigatoria(contaDestinoId, "Conta de destino não encontrada: " + contaDestinoId)
                                )
                                .onItem().transformToUni(contaDestino ->
                                        montarContextoParaContaOrigem(null, TransacaoDeposito.class)
                                                .onItem().transformToUni(contextoTransacao -> {
                                                    TransacaoDeposito novoDeposito = TransacaoFactory.criarDeposito(
                                                            chaveIdempotencia,
                                                            contaDestino,
                                                            valorDeposito,
                                                            canalOrigemDeposito
                                                    );
                                                    return executarEPersistirTransacao(novoDeposito, contextoTransacao);
                                                })
                                )
                );
    }

    @WithTransaction
    public Uni<Transacao> realizarTransferenciaPix(
            String chaveIdempotenciaOpcional,
            UUID contaOrigemId,
            String chavePixDestino,
            BigDecimal valorPix,
            String descricaoPix) {
        String chaveIdempotencia = resolverChaveIdempotencia(chaveIdempotenciaOpcional);

        return transacaoRepository.buscarPorChaveIdempotencia(chaveIdempotencia)
                .onItem().ifNotNull().transformToUni(transacaoExistente -> Uni.createFrom().item(transacaoExistente))
                .onItem().ifNull().switchTo(() ->
                        coreBankBootstrap.garantirInfraestruturaContabilBasica()
                                .onItem().transformToUni(agenciaMatriz ->
                                        buscarContaComBloqueioObrigatoria(contaOrigemId, "Conta de origem não encontrada: " + contaOrigemId)
                                )
                                .onItem().transformToUni(contaOrigem ->
                                        chavePixRepository.buscarPorValorChave(chavePixDestino != null ? chavePixDestino.trim() : "")
                                                .onItem().transform(ChavePix::getConta)
                                                .onItem().transformToUni(contaDestinoEncontrada ->
                                                        montarContextoParaContaOrigem(contaOrigem.getId(), TransacaoPix.class)
                                                                .onItem().transformToUni(contextoTransacao -> {
                                                                    TransacaoPix novoPix = TransacaoFactory.criarPix(
                                                                            chaveIdempotencia,
                                                                            contaOrigem,
                                                                            contaDestinoEncontrada,
                                                                            valorPix,
                                                                            chavePixDestino,
                                                                            descricaoPix
                                                                    );
                                                                    return executarEPersistirTransacao(novoPix, contextoTransacao);
                                                                })
                                                )
                                )
                );
    }

    @WithTransaction
    public Uni<Transacao> realizarTransferenciaTed(
            String chaveIdempotenciaOpcional,
            UUID contaOrigemId,
            String codigoBancoDestino,
            String codigoAgenciaDestino,
            String numeroContaDestino,
            String documentoFavorecido,
            BigDecimal valorTed) {
        String chaveIdempotencia = resolverChaveIdempotencia(chaveIdempotenciaOpcional);

        return transacaoRepository.buscarPorChaveIdempotencia(chaveIdempotencia)
                .onItem().ifNotNull().transformToUni(transacaoExistente -> Uni.createFrom().item(transacaoExistente))
                .onItem().ifNull().switchTo(() ->
                        coreBankBootstrap.garantirInfraestruturaContabilBasica()
                                .onItem().transformToUni(agenciaMatriz ->
                                        buscarContaComBloqueioObrigatoria(contaOrigemId, "Conta de origem não encontrada: " + contaOrigemId)
                                )
                                .onItem().transformToUni(contaOrigem ->
                                        resolverContaDestinoInternaTed(codigoBancoDestino, numeroContaDestino)
                                                .onItem().transformToUni(contaDestinoInternaOpcional ->
                                                        montarContextoParaContaOrigem(contaOrigem.getId(), TransacaoTed.class)
                                                                .onItem().transformToUni(contextoTransacao -> {
                                                                    TransacaoTed novaTed = TransacaoFactory.criarTed(
                                                                            chaveIdempotencia,
                                                                            contaOrigem,
                                                                            contaDestinoInternaOpcional,
                                                                            valorTed,
                                                                            codigoBancoDestino,
                                                                            codigoAgenciaDestino,
                                                                            numeroContaDestino,
                                                                            documentoFavorecido
                                                                    );
                                                                    return executarEPersistirTransacao(novaTed, contextoTransacao);
                                                                })
                                                )
                                )
                );
    }

    @WithTransaction
    public Uni<Transacao> realizarSaque(
            String chaveIdempotenciaOpcional,
            UUID contaOrigemId,
            BigDecimal valorSaque,
            String codigoTerminalAtm) {
        String chaveIdempotencia = resolverChaveIdempotencia(chaveIdempotenciaOpcional);

        return transacaoRepository.buscarPorChaveIdempotencia(chaveIdempotencia)
                .onItem().ifNotNull().transformToUni(transacaoExistente -> Uni.createFrom().item(transacaoExistente))
                .onItem().ifNull().switchTo(() ->
                        coreBankBootstrap.garantirInfraestruturaContabilBasica()
                                .onItem().transformToUni(agenciaMatriz ->
                                        buscarContaComBloqueioObrigatoria(contaOrigemId, "Conta de origem não encontrada: " + contaOrigemId)
                                )
                                .onItem().transformToUni(contaOrigem ->
                                        montarContextoParaContaOrigem(contaOrigem.getId(), TransacaoSaque.class)
                                                .onItem().transformToUni(contextoTransacao -> {
                                                    TransacaoSaque novoSaque = TransacaoFactory.criarSaque(
                                                            chaveIdempotencia,
                                                            contaOrigem,
                                                            valorSaque,
                                                            codigoTerminalAtm
                                                    );
                                                    return executarEPersistirTransacao(novoSaque, contextoTransacao);
                                                })
                                )
                );
    }

    @WithSession
    public Uni<Transacao> buscarTransacaoPorId(UUID transacaoId) {
        return transacaoRepository.buscarPorId(transacaoId)
                .onItem().ifNull().failWith(() ->
                        new RecursoNaoEncontradoException("Transação bancária não encontrada para o ID: " + transacaoId)
                );
    }

    private Uni<Transacao> executarEPersistirTransacao(
            Transacao transacaoParaProcessar,
            ContextoTransacao contextoTransacao) {
        List<LancamentoContabil> lancamentosGerados = transacaoParaProcessar.processar(
                contextoTransacao,
                cadeiaValidadoresPadrao
        );
        return transacaoRepository.persist(transacaoParaProcessar)
                .onItem().transformToUni(transacaoPersistida ->
                        ledgerRepository.persist(lancamentosGerados)
                                .replaceWith(transacaoPersistida)
                );
    }

    private Uni<Conta> buscarContaComBloqueioObrigatoria(UUID contaId, String mensagemErroNaoEncontrada) {
        return contaRepository.buscarPorIdComBloqueioPessimista(contaId)
                .onItem().ifNull().failWith(() -> new RecursoNaoEncontradoException(mensagemErroNaoEncontrada));
    }

    private Uni<Conta> resolverContaDestinoInternaTed(String codigoBancoDestino, String numeroContaDestino) {
        if (codigoBancoDestino != null && Conta.CODIGO_ISPB_BANCO.equals(codigoBancoDestino.trim())) {
            return contaRepository.buscarPorNumeroConta(numeroContaDestino != null ? numeroContaDestino.trim() : "");
        }
        return Uni.createFrom().nullItem();
    }

    private Uni<ContextoTransacao> montarContextoParaContaOrigem(
            UUID contaOrigemIdOpcional,
            Class<? extends Transacao> classeTipoTransacao) {
        ZonedDateTime agoraNoFusoBancario = ZonedDateTime.now(ContextoTransacao.FUSO_HORARIO_BANCARIO_PADRAO);
        LocalDate dataAtual = agoraNoFusoBancario.toLocalDate();
        Instant inicioDoDia = dataAtual.atStartOfDay(ContextoTransacao.FUSO_HORARIO_BANCARIO_PADRAO).toInstant();
        Instant fimDoDia = dataAtual.plusDays(1).atStartOfDay(ContextoTransacao.FUSO_HORARIO_BANCARIO_PADRAO).toInstant();

        Uni<BigDecimal> saldoOrigemUni = (contaOrigemIdOpcional != null)
                ? ledgerRepository.calcularSaldoAtualPorContaId(contaOrigemIdOpcional)
                : Uni.createFrom().item(LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO));

        Uni<BigDecimal> totalMovimentadoHojeUni = (contaOrigemIdOpcional != null)
                ? transacaoRepository.calcularTotalMovimentadoNoPeriodo(
                        contaOrigemIdOpcional,
                        classeTipoTransacao,
                        inicioDoDia,
                        fimDoDia
                )
                : Uni.createFrom().item(LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO));

        Uni<ContaTecnica> contaCaixaUni = contaRepository.buscarContaTecnicaPorTipo(TipoContaTecnica.CAIXA_TESOURARIA);
        Uni<ContaTecnica> contaTarifasUni = contaRepository.buscarContaTecnicaPorTipo(TipoContaTecnica.RECEITA_TARIFAS);
        Uni<ContaTecnica> contaLiquidacaoUni = contaRepository.buscarContaTecnicaPorTipo(TipoContaTecnica.LIQUIDACAO_EXTERNA);

        return Uni.combine().all().unis(
                        saldoOrigemUni,
                        totalMovimentadoHojeUni,
                        contaCaixaUni,
                        contaTarifasUni,
                        contaLiquidacaoUni
                ).asTuple()
                .onItem().transform(insumosCombinados -> new ContextoTransacao(
                        insumosCombinados.getItem1(),
                        insumosCombinados.getItem2(),
                        insumosCombinados.getItem3(),
                        insumosCombinados.getItem4(),
                        insumosCombinados.getItem5(),
                        agoraNoFusoBancario,
                        validarJanelaHorarioTed
                ));
    }

    private String resolverChaveIdempotencia(String chaveIdempotenciaInformada) {
        if (chaveIdempotenciaInformada == null || chaveIdempotenciaInformada.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return chaveIdempotenciaInformada.trim();
    }
}
