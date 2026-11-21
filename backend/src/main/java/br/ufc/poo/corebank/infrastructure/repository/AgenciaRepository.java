package br.ufc.poo.corebank.infrastructure.repository;

import br.ufc.poo.corebank.domain.model.conta.Agencia;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class AgenciaRepository implements PanacheRepositoryBase<Agencia, UUID> {

    public Uni<Agencia> buscarPorCodigo(String codigoAgencia) {
        return find("codigo", codigoAgencia).firstResult();
    }

    public Uni<Agencia> obterOuCriarAgenciaMatriz() {
        return buscarPorCodigo(Agencia.CODIGO_AGENCIA_MATRIZ)
                .onItem().ifNull().switchTo(() -> {
                    Agencia agenciaMatriz = new Agencia(
                            Agencia.CODIGO_AGENCIA_MATRIZ,
                            Agencia.NOME_AGENCIA_MATRIZ
                    );
                    return persist(agenciaMatriz);
                });
    }
}
