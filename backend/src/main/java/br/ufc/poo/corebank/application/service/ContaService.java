package br.ufc.poo.corebank.application.service;

import br.ufc.poo.corebank.domain.exception.ConflitoNegocioException;
import br.ufc.poo.corebank.domain.exception.RecursoNaoEncontradoException;
import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import br.ufc.poo.corebank.domain.model.cliente.TipoCliente;
import br.ufc.poo.corebank.domain.model.conta.Agencia;
import br.ufc.poo.corebank.domain.model.conta.ChavePix;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.ContaCorrente;
import br.ufc.poo.corebank.domain.model.conta.ContaEmpresarial;
import br.ufc.poo.corebank.domain.model.conta.StatusConta;
import br.ufc.poo.corebank.domain.model.conta.TipoChavePix;
import br.ufc.poo.corebank.infrastructure.repository.AgenciaRepository;
import br.ufc.poo.corebank.infrastructure.repository.ChavePixRepository;
import br.ufc.poo.corebank.infrastructure.repository.ClienteRepository;
import br.ufc.poo.corebank.infrastructure.repository.ContaRepository;
import br.ufc.poo.corebank.infrastructure.repository.LedgerRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ContaService {

    private final ContaRepository contaRepository;
    private final ClienteRepository clienteRepository;
    private final AgenciaRepository agenciaRepository;
    private final ChavePixRepository chavePixRepository;
    private final LedgerRepository ledgerRepository;

    public ContaService(
            ContaRepository contaRepository,
            ClienteRepository clienteRepository,
            AgenciaRepository agenciaRepository,
            ChavePixRepository chavePixRepository,
            LedgerRepository ledgerRepository) {
        this.contaRepository = contaRepository;
        this.clienteRepository = clienteRepository;
        this.agenciaRepository = agenciaRepository;
        this.chavePixRepository = chavePixRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @WithTransaction
    public Uni<Conta> abrirContaParaCliente(UUID clienteTitularId, String codigoAgenciaOpcional) {
        return clienteRepository.buscarPorId(clienteTitularId)
                .onItem().ifNull().failWith(() ->
                        new RecursoNaoEncontradoException("Cliente titular não encontrado para o ID: " + clienteTitularId)
                )
                .onItem().transformToUni(clienteTitular ->
                        resolverAgencia(codigoAgenciaOpcional)
                                .onItem().transformToUni(agenciaDestino ->
                                        gerarNumeroContaDisponivel()
                                                .onItem().transformToUni(numeroContaDisponivel -> {
                                                    Conta novaConta = instanciarContaPolimorficamentePorPerfilCliente(
                                                            agenciaDestino,
                                                            clienteTitular,
                                                            numeroContaDisponivel
                                                    );
                                                    agenciaDestino.adicionarConta(novaConta);
                                                    return contaRepository.persist(novaConta);
                                                })
                                )
                );
    }

    private Uni<String> gerarNumeroContaDisponivel() {
        String numeroCandidato = Conta.gerarNovoNumeroContaComDigito();
        return contaRepository.existePorNumeroConta(numeroCandidato)
                .onItem().transformToUni(jaExiste -> {
                    if (Boolean.TRUE.equals(jaExiste)) {
                        return gerarNumeroContaDisponivel();
                    }
                    return Uni.createFrom().item(numeroCandidato);
                });
    }

    private Conta instanciarContaPolimorficamentePorPerfilCliente(
            Agencia agencia,
            Cliente clienteTitular,
            String numeroConta) {
        if (clienteTitular.obterTipoCliente() == TipoCliente.PESSOA_FISICA) {
            return new ContaCorrente(agencia, clienteTitular, numeroConta);
        }
        if (clienteTitular.obterTipoCliente() == TipoCliente.PESSOA_JURIDICA) {
            return new ContaEmpresarial(agencia, clienteTitular, numeroConta);
        }
        throw new RegraNegocioException("Tipo de cliente não suportado para abertura de conta: " + clienteTitular.obterTipoCliente());
    }

    private Uni<Agencia> resolverAgencia(String codigoAgenciaOpcional) {
        if (codigoAgenciaOpcional == null || codigoAgenciaOpcional.isBlank()
                || Agencia.CODIGO_AGENCIA_MATRIZ.equals(codigoAgenciaOpcional.trim())) {
            return agenciaRepository.obterOuCriarAgenciaMatriz();
        }
        String codigoLimpo = codigoAgenciaOpcional.trim();
        return agenciaRepository.buscarPorCodigo(codigoLimpo)
                .onItem().ifNull().switchTo(() -> {
                    Agencia novaAgencia = new Agencia(codigoLimpo, "Agência Digital " + codigoLimpo);
                    return agenciaRepository.persist(novaAgencia);
                });
    }

    @WithSession
    public Uni<Conta> buscarContaPorId(UUID contaId) {
        return contaRepository.buscarPorId(contaId)
                .onItem().ifNull().failWith(() ->
                        new RecursoNaoEncontradoException("Conta bancária não encontrada para o ID: " + contaId)
                );
    }

    @WithSession
    public Uni<List<Conta>> listarContasPorTitular(UUID clienteTitularId) {
        return contaRepository.listarContasPorTitularId(clienteTitularId);
    }

    @WithTransaction
    public Uni<Conta> alterarStatusConta(UUID contaId, StatusConta novoStatusDesejado) {
        if (novoStatusDesejado == null) {
            return Uni.createFrom().failure(new RegraNegocioException("O novo status da conta é obrigatório."));
        }
        return ledgerRepository.calcularSaldoAtualPorContaId(contaId)
                .onItem().transformToUni(saldoAtualNoLedger ->
                        alterarStatusConta(contaId, novoStatusDesejado, saldoAtualNoLedger)
                );
    }

    @WithTransaction
    public Uni<Conta> alterarStatusConta(UUID contaId, StatusConta novoStatusDesejado, BigDecimal saldoAtualNoLedger) {
        if (novoStatusDesejado == null) {
            return Uni.createFrom().failure(new RegraNegocioException("O novo status da conta é obrigatório."));
        }
        return buscarContaPorId(contaId)
                .onItem().invoke(contaEncontrada -> {
                    switch (novoStatusDesejado) {
                        case ATIVA -> contaEncontrada.ativarConta();
                        case BLOQUEADA -> contaEncontrada.bloquearConta();
                        case ENCERRADA -> contaEncontrada.encerrarConta(saldoAtualNoLedger);
                    }
                });
    }

    @WithTransaction
    public Uni<ChavePix> registrarChavePix(UUID contaId, TipoChavePix tipoChave, String valorChaveInformado) {
        return buscarContaPorId(contaId)
                .onItem().transformToUni(contaEncontrada -> {
                    ChavePix novaChavePix = new ChavePix(tipoChave, valorChaveInformado, contaEncontrada);
                    return chavePixRepository.existePorValorChave(novaChavePix.getValorChave())
                            .onItem().transformToUni(chaveJaRegistrada -> {
                                if (Boolean.TRUE.equals(chaveJaRegistrada)) {
                                    return Uni.createFrom().failure(
                                            new ConflitoNegocioException(
                                                    "A chave PIX '" + novaChavePix.getValorChave() + "' já está vinculada a uma conta."
                                            )
                                    );
                                }
                                return chavePixRepository.persist(novaChavePix);
                            });
                });
    }

    @WithSession
    public Uni<List<ChavePix>> listarChavesPixDaConta(UUID contaId) {
        return buscarContaPorId(contaId)
                .onItem().transformToUni(contaEncontrada ->
                        chavePixRepository.listarPorContaId(contaEncontrada.getId())
                );
    }
}
