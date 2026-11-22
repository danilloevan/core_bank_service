package br.ufc.poo.corebank.testsupport;

import br.ufc.poo.corebank.domain.model.conta.ChavePix;
import br.ufc.poo.corebank.infrastructure.repository.ChavePixRepository;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@Mock
@ApplicationScoped
public class InMemoryChavePixRepository extends ChavePixRepository {

    private final InMemoryCoreBankStore bancoEmMemoria;

    public InMemoryChavePixRepository(InMemoryCoreBankStore bancoEmMemoria) {
        this.bancoEmMemoria = bancoEmMemoria;
    }

    @Override
    public Uni<ChavePix> buscarPorValorChave(String valorChave) {
        ChavePix chaveEncontrada = bancoEmMemoria.getChavesPixPorId().values().stream()
                .filter(chave -> chave.getValorChave().equals(valorChave))
                .findFirst()
                .orElse(null);
        return Uni.createFrom().item(chaveEncontrada);
    }

    @Override
    public Uni<Boolean> existePorValorChave(String valorChave) {
        boolean existe = bancoEmMemoria.getChavesPixPorId().values().stream()
                .anyMatch(chave -> chave.getValorChave().equals(valorChave));
        return Uni.createFrom().item(existe);
    }

    @Override
    public Uni<List<ChavePix>> listarPorContaId(UUID contaId) {
        List<ChavePix> chavesDaConta = bancoEmMemoria.getChavesPixPorId().values().stream()
                .filter(chave -> contaId.equals(chave.getConta().getId()))
                .toList();
        return Uni.createFrom().item(chavesDaConta);
    }

    @Override
    public Uni<ChavePix> persist(ChavePix chavePix) {
        InMemoryCoreBankStore.garantirIdGerado(chavePix, ChavePix.class);
        bancoEmMemoria.getChavesPixPorId().put(chavePix.getId(), chavePix);
        return Uni.createFrom().item(chavePix);
    }
}
