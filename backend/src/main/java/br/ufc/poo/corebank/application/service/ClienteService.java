package br.ufc.poo.corebank.application.service;

import br.ufc.poo.corebank.domain.exception.ConflitoNegocioException;
import br.ufc.poo.corebank.domain.exception.RecursoNaoEncontradoException;
import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import br.ufc.poo.corebank.domain.model.cliente.Endereco;
import br.ufc.poo.corebank.domain.model.cliente.PessoaFisica;
import br.ufc.poo.corebank.domain.model.cliente.PessoaJuridica;
import br.ufc.poo.corebank.domain.model.cliente.TipoCliente;
import br.ufc.poo.corebank.infrastructure.repository.ClienteRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @WithTransaction
    public Uni<PessoaFisica> cadastrarPessoaFisica(
            String nomeCompleto,
            String cpfBruto,
            LocalDate dataNascimento,
            String email,
            String telefone,
            Endereco endereco) {
        PessoaFisica novaPessoaFisica = new PessoaFisica(
                nomeCompleto,
                cpfBruto,
                dataNascimento,
                email,
                telefone,
                endereco
        );

        return clienteRepository.existePorDocumentoFiscal(novaPessoaFisica.getCpf())
                .onItem().transformToUni(documentoJaCadastrado -> {
                    if (Boolean.TRUE.equals(documentoJaCadastrado)) {
                        return Uni.createFrom().failure(
                                new ConflitoNegocioException("Já existe um cliente cadastrado com o CPF informado.")
                        );
                    }
                    return clienteRepository.persist(novaPessoaFisica)
                            .onItem().transform(clientePersistido -> novaPessoaFisica);
                });
    }

    @WithTransaction
    public Uni<PessoaJuridica> cadastrarPessoaJuridica(
            String razaoSocial,
            String nomeFantasia,
            String cnpjBruto,
            String emailCorporativo,
            String telefoneComercial,
            Endereco enderecoSede) {
        PessoaJuridica novaPessoaJuridica = new PessoaJuridica(
                razaoSocial,
                nomeFantasia,
                cnpjBruto,
                emailCorporativo,
                telefoneComercial,
                enderecoSede
        );

        return clienteRepository.existePorDocumentoFiscal(novaPessoaJuridica.getCnpj())
                .onItem().transformToUni(documentoJaCadastrado -> {
                    if (Boolean.TRUE.equals(documentoJaCadastrado)) {
                        return Uni.createFrom().failure(
                                new ConflitoNegocioException("Já existe um cliente cadastrado com o CNPJ informado.")
                        );
                    }
                    return clienteRepository.persist(novaPessoaJuridica)
                            .onItem().transform(clientePersistido -> novaPessoaJuridica);
                });
    }

    @WithSession
    public Uni<Cliente> buscarClientePorId(UUID clienteId) {
        return clienteRepository.buscarPorId(clienteId)
                .onItem().ifNull().failWith(() ->
                        new RecursoNaoEncontradoException("Cliente não encontrado para o ID: " + clienteId)
                );
    }

    @WithSession
    public Uni<List<Cliente>> listarClientes(TipoCliente filtroTipoCliente) {
        return clienteRepository.listarPorFiltroTipo(filtroTipoCliente);
    }

    public Multi<Cliente> streamClientes(TipoCliente filtroTipoCliente) {
        return listarClientes(filtroTipoCliente)
                .onItem().transformToMulti(listaClientes -> Multi.createFrom().iterable(listaClientes));
    }
}
