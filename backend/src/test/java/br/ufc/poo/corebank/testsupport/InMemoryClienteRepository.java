package br.ufc.poo.corebank.testsupport;

import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import br.ufc.poo.corebank.domain.model.cliente.TipoCliente;
import br.ufc.poo.corebank.infrastructure.repository.ClienteRepository;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@Mock
@ApplicationScoped
public class InMemoryClienteRepository extends ClienteRepository {

    private final InMemoryCoreBankStore bancoEmMemoria;

    public InMemoryClienteRepository(InMemoryCoreBankStore bancoEmMemoria) {
        this.bancoEmMemoria = bancoEmMemoria;
    }

    @Override
    public Uni<Cliente> buscarPorId(UUID clienteId) {
        return Uni.createFrom().item(bancoEmMemoria.getClientesPorId().get(clienteId));
    }

    @Override
    public Uni<Cliente> buscarPorDocumentoFiscal(String documentoFiscalSomenteDigitos) {
        Cliente clienteEncontrado = bancoEmMemoria.getClientesPorId().values().stream()
                .filter(cliente -> cliente.getDocumentoFiscal().equals(documentoFiscalSomenteDigitos))
                .findFirst()
                .orElse(null);
        return Uni.createFrom().item(clienteEncontrado);
    }

    @Override
    public Uni<Boolean> existePorDocumentoFiscal(String documentoFiscalSomenteDigitos) {
        boolean existe = bancoEmMemoria.getClientesPorId().values().stream()
                .anyMatch(cliente -> cliente.getDocumentoFiscal().equals(documentoFiscalSomenteDigitos));
        return Uni.createFrom().item(existe);
    }

    @Override
    public Uni<List<Cliente>> listarPorFiltroTipo(TipoCliente filtroTipoCliente) {
        List<Cliente> clientesFiltrados = bancoEmMemoria.getClientesPorId().values().stream()
                .filter(cliente -> filtroTipoCliente == null || cliente.obterTipoCliente() == filtroTipoCliente)
                .toList();
        return Uni.createFrom().item(clientesFiltrados);
    }

    @Override
    public Uni<Cliente> persist(Cliente cliente) {
        InMemoryCoreBankStore.garantirIdGerado(cliente, Cliente.class);
        bancoEmMemoria.getClientesPorId().put(cliente.getId(), cliente);
        return Uni.createFrom().item(cliente);
    }
}
