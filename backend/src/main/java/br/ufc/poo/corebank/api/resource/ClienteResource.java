package br.ufc.poo.corebank.api.resource;

import br.ufc.poo.corebank.api.dto.CadastrarPessoaFisicaRequest;
import br.ufc.poo.corebank.api.dto.CadastrarPessoaJuridicaRequest;
import br.ufc.poo.corebank.api.dto.ClienteResponse;
import br.ufc.poo.corebank.application.service.ClienteService;
import br.ufc.poo.corebank.domain.model.cliente.TipoCliente;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Clientes", description = "Cadastro e consulta de clientes Pessoa Física (PF) e Pessoa Jurídica (PJ)")
public class ClienteResource {

    private final ClienteService clienteService;

    public ClienteResource(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @POST
    @Path("/pf")
    @Operation(summary = "Cadastrar Pessoa Física", description = "Valida os dígitos verificadores do CPF, maioridade civil e endereço em composição")
    public Uni<Response> cadastrarPessoaFisica(@Valid CadastrarPessoaFisicaRequest requisicao) {
        return clienteService.cadastrarPessoaFisica(
                        requisicao.nomeCompleto(),
                        requisicao.cpf(),
                        requisicao.dataNascimento(),
                        requisicao.email(),
                        requisicao.telefone(),
                        requisicao.endereco().paraDominio()
                )
                .onItem().transform(pessoaFisicaCriada ->
                        Response.status(Response.Status.CREATED)
                                .entity(ClienteResponse.deDominio(pessoaFisicaCriada))
                                .build()
                );
    }

    @POST
    @Path("/pj")
    @Operation(summary = "Cadastrar Pessoa Jurídica", description = "Valida os dígitos verificadores do CNPJ, razão social e endereço da sede")
    public Uni<Response> cadastrarPessoaJuridica(@Valid CadastrarPessoaJuridicaRequest requisicao) {
        return clienteService.cadastrarPessoaJuridica(
                        requisicao.razaoSocial(),
                        requisicao.nomeFantasia(),
                        requisicao.cnpj(),
                        requisicao.email(),
                        requisicao.telefone(),
                        requisicao.endereco().paraDominio()
                )
                .onItem().transform(pessoaJuridicaCriada ->
                        Response.status(Response.Status.CREATED)
                                .entity(ClienteResponse.deDominio(pessoaJuridicaCriada))
                                .build()
                );
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar cliente por ID")
    public Uni<ClienteResponse> buscarClientePorId(@PathParam("id") UUID clienteId) {
        return clienteService.buscarClientePorId(clienteId)
                .onItem().transform(ClienteResponse::deDominio);
    }

    @GET
    @Operation(summary = "Listar clientes", description = "Lista clientes cadastrados com filtro opcional por tipo (PESSOA_FISICA ou PESSOA_JURIDICA)")
    public Uni<List<ClienteResponse>> listarClientes(@QueryParam("tipo") TipoCliente filtroTipoCliente) {
        return clienteService.listarClientes(filtroTipoCliente)
                .onItem().transform(listaClientes ->
                        listaClientes.stream()
                                .map(ClienteResponse::deDominio)
                                .toList()
                );
    }

    @GET
    @Path("/stream")
    @Operation(summary = "Stream reativo de clientes (Mutiny Multi)")
    public Multi<ClienteResponse> streamClientes(@QueryParam("tipo") TipoCliente filtroTipoCliente) {
        return clienteService.streamClientes(filtroTipoCliente)
                .onItem().transform(ClienteResponse::deDominio);
    }
}
