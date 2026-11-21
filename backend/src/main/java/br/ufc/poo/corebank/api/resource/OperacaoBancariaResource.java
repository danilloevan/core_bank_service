package br.ufc.poo.corebank.api.resource;

import br.ufc.poo.corebank.api.dto.DepositoRequest;
import br.ufc.poo.corebank.api.dto.SaqueRequest;
import br.ufc.poo.corebank.api.dto.TransacaoResponse;
import br.ufc.poo.corebank.api.dto.TransferenciaPixRequest;
import br.ufc.poo.corebank.api.dto.TransferenciaTedRequest;
import br.ufc.poo.corebank.application.service.MotorTransacaoService;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Operações Bancárias & Transações", description = "Liquidação reativa de Depósito, PIX, TED e Saque com Ledger de Partidas Dobradas e Idempotência")
public class OperacaoBancariaResource {

    private final MotorTransacaoService motorTransacaoService;

    public OperacaoBancariaResource(MotorTransacaoService motorTransacaoService) {
        this.motorTransacaoService = motorTransacaoService;
    }

    @POST
    @Path("/operacoes/deposito")
    @Operation(
            summary = "Realizar depósito em conta",
            description = "Gera partida dobrada (DEBITO na Conta Técnica de Caixa/Tesouraria e CREDITO na Conta do Cliente)"
    )
    public Uni<Response> realizarDeposito(
            @HeaderParam("Idempotency-Key") String chaveIdempotencia,
            @Valid DepositoRequest requisicao) {
        return motorTransacaoService.realizarDeposito(
                        chaveIdempotencia,
                        requisicao.contaDestinoId(),
                        requisicao.valor(),
                        requisicao.canalOrigem()
                )
                .onItem().transform(transacaoProcessada ->
                        Response.status(Response.Status.CREATED)
                                .entity(TransacaoResponse.deDominio(transacaoProcessada))
                                .build()
                );
    }

    @POST
    @Path("/operacoes/pix")
    @Operation(
            summary = "Realizar transferência PIX",
            description = "Liquidação instantânea com geração de EndToEndId, tarifa isenta para PF / percentual para PJ e limite noturno"
    )
    public Uni<Response> realizarTransferenciaPix(
            @HeaderParam("Idempotency-Key") String chaveIdempotencia,
            @Valid TransferenciaPixRequest requisicao) {
        return motorTransacaoService.realizarTransferenciaPix(
                        chaveIdempotencia,
                        requisicao.contaOrigemId(),
                        requisicao.chavePixDestino(),
                        requisicao.valor(),
                        requisicao.descricao()
                )
                .onItem().transform(transacaoProcessada ->
                        Response.status(Response.Status.CREATED)
                                .entity(TransacaoResponse.deDominio(transacaoProcessada))
                                .build()
                );
    }

    @POST
    @Path("/operacoes/ted")
    @Operation(
            summary = "Realizar transferência TED",
            description = "Liquidação TED com tarifa fixa, validação de janela operacional e partida dobrada para tarifa"
    )
    public Uni<Response> realizarTransferenciaTed(
            @HeaderParam("Idempotency-Key") String chaveIdempotencia,
            @Valid TransferenciaTedRequest requisicao) {
        return motorTransacaoService.realizarTransferenciaTed(
                        chaveIdempotencia,
                        requisicao.contaOrigemId(),
                        requisicao.codigoBancoDestino(),
                        requisicao.codigoAgenciaDestino(),
                        requisicao.numeroContaDestino(),
                        requisicao.documentoFavorecido(),
                        requisicao.valor()
                )
                .onItem().transform(transacaoProcessada ->
                        Response.status(Response.Status.CREATED)
                                .entity(TransacaoResponse.deDominio(transacaoProcessada))
                                .build()
                );
    }

    @POST
    @Path("/operacoes/saque")
    @Operation(
            summary = "Realizar saque em terminal ATM",
            description = "Valida saldo disponível no Ledger e limite diário de saque da conta (PF vs PJ)"
    )
    public Uni<Response> realizarSaque(
            @HeaderParam("Idempotency-Key") String chaveIdempotencia,
            @Valid SaqueRequest requisicao) {
        return motorTransacaoService.realizarSaque(
                        chaveIdempotencia,
                        requisicao.contaOrigemId(),
                        requisicao.valor(),
                        requisicao.codigoTerminalAtm()
                )
                .onItem().transform(transacaoProcessada ->
                        Response.status(Response.Status.CREATED)
                                .entity(TransacaoResponse.deDominio(transacaoProcessada))
                                .build()
                );
    }

    @GET
    @Path("/transacoes/{id}")
    @Operation(
            summary = "Consultar transação e seus lançamentos contábeis",
            description = "Retorna os detalhes da transação e a lista imutável de lançamentos contábeis (auditoria de partidas dobradas)"
    )
    public Uni<TransacaoResponse> buscarTransacaoPorId(@PathParam("id") UUID transacaoId) {
        return motorTransacaoService.buscarTransacaoPorId(transacaoId)
                .onItem().transform(TransacaoResponse::deDominio);
    }
}
