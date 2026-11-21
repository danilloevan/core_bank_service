package br.ufc.poo.corebank.infrastructure.repository;

import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import br.ufc.poo.corebank.domain.model.cliente.PessoaFisica;
import br.ufc.poo.corebank.domain.model.cliente.PessoaJuridica;
import br.ufc.poo.corebank.domain.model.cliente.TipoCliente;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ClienteRepository implements PanacheRepositoryBase<Cliente, UUID> {

    public Uni<Cliente> buscarPorId(UUID clienteId) {
        return findById(clienteId);
    }

    public Uni<Cliente> buscarPorDocumentoFiscal(String documentoFiscalSomenteDigitos) {
        return find("documentoFiscal", documentoFiscalSomenteDigitos).firstResult();
    }

    public Uni<Boolean> existePorDocumentoFiscal(String documentoFiscalSomenteDigitos) {
        return count("documentoFiscal", documentoFiscalSomenteDigitos)
                .onItem().transform(quantidadeEncontrada -> quantidadeEncontrada > 0);
    }

    public Uni<List<Cliente>> listarPorFiltroTipo(TipoCliente filtroTipoCliente) {
        if (filtroTipoCliente == null) {
            return listAll();
        }
        if (filtroTipoCliente == TipoCliente.PESSOA_FISICA) {
            return find("from PessoaFisica").list();
        }
        return find("from PessoaJuridica").list();
    }
}
