package br.ufc.poo.corebank.testsupport;

import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import br.ufc.poo.corebank.domain.model.conta.Agencia;
import br.ufc.poo.corebank.domain.model.conta.ChavePix;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;
import jakarta.enterprise.context.ApplicationScoped;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class InMemoryCoreBankStore {

    private final Map<UUID, Agencia> agenciasPorId = new ConcurrentHashMap<>();
    private final Map<UUID, Cliente> clientesPorId = new ConcurrentHashMap<>();
    private final Map<UUID, Conta> contasPorId = new ConcurrentHashMap<>();
    private final Map<UUID, ChavePix> chavesPixPorId = new ConcurrentHashMap<>();
    private final Map<UUID, Transacao> transacoesPorId = new ConcurrentHashMap<>();
    private final List<LancamentoContabil> lancamentosContabeis = new CopyOnWriteArrayList<>();

    public void limparTudo() {
        agenciasPorId.clear();
        clientesPorId.clear();
        contasPorId.clear();
        chavesPixPorId.clear();
        transacoesPorId.clear();
        lancamentosContabeis.clear();
    }

    public Map<UUID, Agencia> getAgenciasPorId() {
        return agenciasPorId;
    }

    public Map<UUID, Cliente> getClientesPorId() {
        return clientesPorId;
    }

    public Map<UUID, Conta> getContasPorId() {
        return contasPorId;
    }

    public Map<UUID, ChavePix> getChavesPixPorId() {
        return chavesPixPorId;
    }

    public Map<UUID, Transacao> getTransacoesPorId() {
        return transacoesPorId;
    }

    public List<LancamentoContabil> getLancamentosContabeis() {
        return lancamentosContabeis;
    }

    public static UUID garantirIdGerado(Object entidade, Class<?> classeDeclaranteDoId) {
        try {
            Field campoId = classeDeclaranteDoId.getDeclaredField("id");
            campoId.setAccessible(true);
            UUID idAtual = (UUID) campoId.get(entidade);
            if (idAtual == null) {
                idAtual = UUID.randomUUID();
                campoId.set(entidade, idAtual);
            }
            return idAtual;
        } catch (ReflectiveOperationException excecaoReflexao) {
            throw new IllegalStateException("Falha ao atribuir UUID em memória para entidade de teste.", excecaoReflexao);
        }
    }
}
