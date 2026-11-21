package br.ufc.poo.corebank.api.resource;

import br.ufc.poo.corebank.api.dto.AbrirContaRequest;
import br.ufc.poo.corebank.api.dto.AlterarStatusContaRequest;
import br.ufc.poo.corebank.api.dto.ChavePixResponse;
import br.ufc.poo.corebank.api.dto.ContaResponse;
import br.ufc.poo.corebank.api.dto.RegistrarChavePixRequest;
import br.ufc.poo.corebank.application.service.ContaService;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/contas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Contas & PIX", description = "Abertura de contas (Corrente PF / Empresarial PJ), gestão de status e chaves PIX")
public class ContaResource {

    private final ContaService contaService;

    public ContaResource(ContaService contaService) {
        this.contaService = contaService;
    }

    @POST
    @Operation(
            summary = "Abrir conta bancária",
            description = "Instancia polimorficamente ContaCorrente para PF ou ContaEmpresarial para PJ e agrega à Agência"
    )
    public Uni<Response> abrirConta(@Valid AbrirContaRequest requisicao) {
        return contaService.abrirContaParaCliente(requisicao.clienteTitularId(), requisicao.codigoAgencia())
                .onItem().transform(contaAberta ->
                        Response.status(Response.Status.CREATED)
                                .entity(ContaResponse.deDominio(contaAberta))
                                .build()
                );
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar conta por ID")
    public Uni<ContaResponse> buscarContaPorId(@PathParam("id") UUID contaId) {
        return contaService.buscarContaPorId(contaId)
                .onItem().transform(ContaResponse::deDominio);
    }

    @GET
    @Operation(summary = "Listar contas de um titular")
    public Uni<List<ContaResponse>> listarContasPorTitular(@QueryParam("titularId") UUID titularId) {
        return contaService.listarContasPorTitular(titularId)
                .onItem().transform(listaContas ->
                        listaContas.stream()
                                .map(ContaResponse::deDominio)
                                .toList()
                );
    }

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Alterar status da conta (ATIVA, BLOQUEADA, ENCERRADA)")
    public Uni<ContaResponse> alterarStatusConta(
            @PathParam("id") UUID contaId,
            @Valid AlterarStatusContaRequest requisicao) {
        return contaService.alterarStatusConta(contaId, requisicao.novoStatus(), BigDecimal.ZERO)
                .onItem().transform(ContaResponse::deDominio);
    }

    @POST
    @Path("/{id}/chaves-pix")
    @Operation(summary = "Registrar chave PIX na conta")
    public Uni<Response> registrarChavePix(
            @PathParam("id") UUID contaId,
            @Valid RegistrarChavePixRequest requisicao) {
        return contaService.registrarChavePix(contaId, requisicao.tipoChave(), requisicao.valorChave())
                .onItem().transform(chaveCriada ->
                        Response.status(Response.Status.CREATED)
                                .entity(ChavePixResponse.deDominio(chaveCriada))
                                .build()
                );
    }

    @GET
    @Path("/{id}/chaves-pix")
    @Operation(summary = "Listar chaves PIX vinculadas à conta")
    public Uni<List<ChavePixResponse>> listarChavesPixDaConta(@PathParam("id") UUID contaId) {
        return contaService.listarChavesPixDaConta(contaId)
                .onItem().transform(listaChaves ->
                        listaChaves.stream()
                                .map(ChavePixResponse::deDominio)
                                .toList()
                );
    }
}
