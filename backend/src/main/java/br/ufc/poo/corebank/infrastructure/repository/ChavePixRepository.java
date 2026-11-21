package br.ufc.poo.corebank.infrastructure.repository;

import br.ufc.poo.corebank.domain.model.conta.ChavePix;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ChavePixRepository implements PanacheRepositoryBase<ChavePix, UUID> {

    public Uni<ChavePix> buscarPorValorChave(String valorChave) {
        return find("valorChave", valorChave).firstResult();
    }

    public Uni<Boolean> existePorValorChave(String valorChave) {
        return count("valorChave", valorChave)
                .onItem().transform(quantidadeEncontrada -> quantidadeEncontrada > 0);
    }

    public Uni<List<ChavePix>> listarPorContaId(UUID contaId) {
        return list("conta.id", contaId);
    }
}
