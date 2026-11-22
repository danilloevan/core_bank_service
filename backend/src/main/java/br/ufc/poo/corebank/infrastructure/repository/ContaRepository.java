package br.ufc.poo.corebank.infrastructure.repository;

import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.ContaTecnica;
import br.ufc.poo.corebank.domain.model.conta.TipoContaTecnica;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ContaRepository implements PanacheRepositoryBase<Conta, UUID> {

    public Uni<Conta> buscarPorId(UUID contaId) {
        return findById(contaId);
    }

    public Uni<Conta> buscarPorIdComBloqueioPessimista(UUID contaId) {
        return getSession()
                .onItem().transformToUni(sessaoHibernate ->
                        sessaoHibernate.createSelectionQuery("select c from Conta c where c.id = :contaId", Conta.class)
                                .setParameter("contaId", contaId)
                                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                                .getSingleResultOrNull()
                );
    }

    public Uni<Conta> buscarPorNumeroConta(String numeroConta) {
        return find("numeroConta", numeroConta).firstResult();
    }

    public Uni<Boolean> existePorNumeroConta(String numeroConta) {
        return count("numeroConta", numeroConta)
                .onItem().transform(quantidadeEncontrada -> quantidadeEncontrada > 0);
    }

    public Uni<ContaTecnica> buscarContaTecnicaPorTipo(TipoContaTecnica tipoContaTecnica) {
        return find("select c from ContaTecnica c where c.tipoContaTecnica = ?1", tipoContaTecnica)
                .firstResult();
    }

    public Uni<List<Conta>> listarContasPorTitularId(UUID titularId) {
        return list("titular.id", titularId);
    }
}
