package br.ufc.poo.corebank.testsupport;

import br.ufc.poo.corebank.domain.model.conta.Agencia;
import br.ufc.poo.corebank.infrastructure.repository.AgenciaRepository;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class InMemoryAgenciaRepository extends AgenciaRepository {

    private final InMemoryCoreBankStore bancoEmMemoria;

    public InMemoryAgenciaRepository(InMemoryCoreBankStore bancoEmMemoria) {
        this.bancoEmMemoria = bancoEmMemoria;
    }

    @Override
    public Uni<Agencia> buscarPorCodigo(String codigoAgencia) {
        Agencia agenciaEncontrada = bancoEmMemoria.getAgenciasPorId().values().stream()
                .filter(agencia -> agencia.getCodigo().equals(codigoAgencia))
                .findFirst()
                .orElse(null);
        return Uni.createFrom().item(agenciaEncontrada);
    }

    @Override
    public Uni<Agencia> persist(Agencia agencia) {
        InMemoryCoreBankStore.garantirIdGerado(agencia, Agencia.class);
        bancoEmMemoria.getAgenciasPorId().put(agencia.getId(), agencia);
        return Uni.createFrom().item(agencia);
    }
}
