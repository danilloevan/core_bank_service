package br.ufc.poo.corebank.api.resource;

import br.ufc.poo.corebank.api.dto.AbrirContaRequest;
import br.ufc.poo.corebank.api.dto.AlterarStatusContaRequest;
import br.ufc.poo.corebank.api.dto.ChavePixResponse;
import br.ufc.poo.corebank.api.dto.ContaResponse;
import br.ufc.poo.corebank.api.dto.ExtratoResponse;
import br.ufc.poo.corebank.api.dto.ItemExtratoResponse;
import br.ufc.poo.corebank.api.dto.RegistrarChavePixRequest;
import br.ufc.poo.corebank.api.dto.SaldoResponse;
import br.ufc.poo.corebank.application.service.ContaService;
import br.ufc.poo.corebank.application.service.ExtratoService;
import br.ufc.poo.corebank.application.service.SaldoService;
import br.ufc.poo.corebank.domain.export.ExportadorExtrato;
import br.ufc.poo.corebank.domain.export.ExportadorExtratoCsv;
import br.ufc.poo.corebank.domain.export.ExportadorExtratoTexto;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;
import io.smallrye.mutiny.Multi;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/contas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Contas, Saldo, Extrato & PIX", description = "Abertura de contas, consulta de saldo dinâmico, extrato no Ledger, exportação CSV/TXT e chaves PIX")
public class ContaResource {

    private final ContaService contaService;
    private final SaldoService saldoService;
    private final ExtratoService extratoService;

    public ContaResource(
            ContaService contaService,
            SaldoService saldoService,
            ExtratoService extratoService) {
        this.contaService = contaService;
        this.saldoService = saldoService;
        this.extratoService = extratoService;
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

    @GET
    @Path("/{id}/saldo")
    @Operation(
            summary = "Consultar saldo dinâmico da conta no Ledger",
            description = "Calcula em tempo real SUM(CREDITO) - SUM(DEBITO) a partir do histórico imutável de lançamentos contábeis"
    )
    public Uni<SaldoResponse> consultarSaldo(@PathParam("id") UUID contaId) {
        return saldoService.consultarResumoSaldo(contaId)
                .onItem().transform(SaldoResponse::deDominio);
    }

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Alterar status da conta (ATIVA, BLOQUEADA, ENCERRADA)")
    public Uni<ContaResponse> alterarStatusConta(
            @PathParam("id") UUID contaId,
            @Valid AlterarStatusContaRequest requisicao) {
        return contaService.alterarStatusConta(contaId, requisicao.novoStatus())
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

    @GET
    @Path("/{id}/extrato")
    @Operation(
            summary = "Consultar extrato bancário detalhado no Ledger",
            description = "Calcula saldo anterior ao período, saldo progressivo lançamento a lançamento, total de entradas/saídas e saldo final"
    )
    public Uni<ExtratoResponse> consultarExtrato(
            @PathParam("id") UUID contaId,
            @QueryParam("dataInicio") LocalDate dataInicio,
            @QueryParam("dataFim") LocalDate dataFim,
            @QueryParam("tipoLancamento") TipoLancamento tipoLancamento) {
        return extratoService.emitirExtrato(contaId, dataInicio, dataFim, tipoLancamento)
                .onItem().transform(ExtratoResponse::deDominio);
    }

    @GET
    @Path("/{id}/extrato/stream")
    @Operation(
            summary = "Stream reativo de itens do extrato (Mutiny Multi)",
            description = "Emite cada lançamento do extrato de forma reativa usando Multi<ItemExtratoResponse>"
    )
    public Multi<ItemExtratoResponse> streamItensExtrato(
            @PathParam("id") UUID contaId,
            @QueryParam("dataInicio") LocalDate dataInicio,
            @QueryParam("dataFim") LocalDate dataFim,
            @QueryParam("tipoLancamento") TipoLancamento tipoLancamento) {
        return extratoService.streamItensExtrato(contaId, dataInicio, dataFim, tipoLancamento)
                .onItem().transform(ItemExtratoResponse::deDominio);
    }

    @GET
    @Path("/{id}/extrato/csv")
    @Produces("text/csv")
    @Operation(
            summary = "Exportar extrato em formato CSV",
            description = "Utiliza polimorfismo da interface ExportadorExtrato (ExportadorExtratoCsv) para gerar arquivo CSV"
    )
    public Uni<Response> exportarExtratoCsv(
            @PathParam("id") UUID contaId,
            @QueryParam("dataInicio") LocalDate dataInicio,
            @QueryParam("dataFim") LocalDate dataFim,
            @QueryParam("tipoLancamento") TipoLancamento tipoLancamento) {
        ExportadorExtrato exportadorCsv = new ExportadorExtratoCsv();
        return extratoService.exportarExtrato(contaId, dataInicio, dataFim, tipoLancamento, exportadorCsv)
                .onItem().transform(conteudoCsv ->
                        Response.ok(conteudoCsv)
                                .type(exportadorCsv.obterContentType())
                                .header("Content-Disposition", "attachment; filename=\"extrato-" + contaId + "." + exportadorCsv.obterExtensaoArquivo() + "\"")
                                .build()
                );
    }

    @GET
    @Path("/{id}/extrato/txt")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
            summary = "Exportar extrato em formato texto (comprovante bancário)",
            description = "Utiliza polimorfismo da interface ExportadorExtrato (ExportadorExtratoTexto) para gerar comprovante formatado"
    )
    public Uni<Response> exportarExtratoTexto(
            @PathParam("id") UUID contaId,
            @QueryParam("dataInicio") LocalDate dataInicio,
            @QueryParam("dataFim") LocalDate dataFim,
            @QueryParam("tipoLancamento") TipoLancamento tipoLancamento) {
        ExportadorExtrato exportadorTexto = new ExportadorExtratoTexto();
        return extratoService.exportarExtrato(contaId, dataInicio, dataFim, tipoLancamento, exportadorTexto)
                .onItem().transform(conteudoTexto ->
                        Response.ok(conteudoTexto)
                                .type(exportadorTexto.obterContentType())
                                .build()
                );
    }
}
