package br.ufc.poo.corebank.testsupport;

import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.ContaTecnica;
import br.ufc.poo.corebank.domain.model.conta.TipoContaTecnica;
import br.ufc.poo.corebank.infrastructure.repository.ContaRepository;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@Mock
@ApplicationScoped
public class InMemoryContaRepository extends ContaRepository {

    private final InMemoryCoreBankStore bancoEmMemoria;

    public InMemoryContaRepository(InMemoryCoreBankStore bancoEmMemoria) {
        this.bancoEmMemoria = bancoEmMemoria;
    }

    @Override
    public Uni<Conta> buscarPorId(UUID contaId) {
        return Uni.createFrom().item(bancoEmMemoria.getContasPorId().get(contaId));
    }

    @Override
    public Uni<Conta> buscarPorIdComBloqueioPessimista(UUID contaId) {
        return buscarPorId(contaId);
    }

    @Override
    public Uni<Conta> buscarPorNumeroConta(String numeroConta) {
        Conta contaEncontrada = bancoEmMemoria.getContasPorId().values().stream()
                .filter(conta -> conta.getNumeroConta().equals(numeroConta))
                .findFirst()
                .orElse(null);
        return Uni.createFrom().item(contaEncontrada);
    }

    @Override
    public Uni<Boolean> existePorNumeroConta(String numeroConta) {
        boolean existe = bancoEmMemoria.getContasPorId().values().stream()
                .anyMatch(conta -> conta.getNumeroConta().equals(numeroConta));
        return Uni.createFrom().item(existe);
    }

    @Override
    public Uni<ContaTecnica> buscarContaTecnicaPorTipo(TipoContaTecnica tipoContaTecnica) {
        ContaTecnica contaTecnicaEncontrada = bancoEmMemoria.getContasPorId().values().stream()
                .filter(ContaTecnica.class::isInstance)
                .map(ContaTecnica.class::cast)
                .filter(contaTecnica -> contaTecnica.getTipoContaTecnica() == tipoContaTecnica)
                .findFirst()
                .orElse(null);
        return Uni.createFrom().item(contaTecnicaEncontrada);
    }

    @Override
    public Uni<List<Conta>> listarContasPorTitularId(UUID titularId) {
        List<Conta> contasDoTitular = bancoEmMemoria.getContasPorId().values().stream()
                .filter(conta -> conta.getTitular() != null && titularId.equals(conta.getTitular().getId()))
                .toList();
        return Uni.createFrom().item(contasDoTitular);
    }

    @Override
    public Uni<Conta> persist(Conta conta) {
        InMemoryCoreBankStore.garantirIdGerado(conta, Conta.class);
        bancoEmMemoria.getContasPorId().put(conta.getId(), conta);
        return Uni.createFrom().item(conta);
    }
}
