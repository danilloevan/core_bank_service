package br.ufc.poo.corebank.integration;

import br.ufc.poo.corebank.application.service.ClienteService;
import br.ufc.poo.corebank.application.service.ContaService;
import br.ufc.poo.corebank.application.service.ExtratoService;
import br.ufc.poo.corebank.application.service.MotorTransacaoService;
import br.ufc.poo.corebank.application.service.SaldoService;
import br.ufc.poo.corebank.domain.exception.SaldoInsuficienteException;
import br.ufc.poo.corebank.domain.model.cliente.Endereco;
import br.ufc.poo.corebank.domain.model.cliente.PessoaFisica;
import br.ufc.poo.corebank.domain.model.cliente.PessoaJuridica;
import br.ufc.poo.corebank.domain.model.conta.ChavePix;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.TipoChavePix;
import br.ufc.poo.corebank.domain.model.ledger.ItemExtratoConta;
import br.ufc.poo.corebank.domain.model.ledger.ResumoSaldoConta;
import br.ufc.poo.corebank.domain.model.transacao.Transacao;
import br.ufc.poo.corebank.testsupport.InMemoryCoreBankStore;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.restassured.http.ContentType;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class FluxoBancarioReativoIntegrationTest {

    @Inject
    InMemoryCoreBankStore bancoEmMemoria;

    @Inject
    ClienteService clienteService;

    @Inject
    ContaService contaService;

    @Inject
    MotorTransacaoService motorTransacaoService;

    @Inject
    SaldoService saldoService;

    @Inject
    ExtratoService extratoService;

    @Inject
    Vertx vertx;

    private <T> Uni<T> executarNoContextoVertx(Supplier<Uni<T>> fornecedorUni) {
        return Uni.createFrom().emitter(emissor -> {
            Context contextoIsolado = VertxContext.getOrCreateDuplicatedContext(vertx);
            VertxContextSafetyToggle.setContextSafe(contextoIsolado, true);
            contextoIsolado.runOnContext(evento ->
                    fornecedorUni.get().subscribe().with(emissor::complete, emissor::fail)
            );
        });
    }

    private <T> Multi<T> executarMultiNoContextoVertx(Supplier<Multi<T>> fornecedorMulti) {
        return Multi.createFrom().emitter(emissor -> {
            Context contextoIsolado = VertxContext.getOrCreateDuplicatedContext(vertx);
            VertxContextSafetyToggle.setContextSafe(contextoIsolado, true);
            contextoIsolado.runOnContext(evento ->
                    fornecedorMulti.get().subscribe().with(
                            emissor::emit,
                            emissor::fail,
                            emissor::complete
                    )
            );
        });
    }

    @BeforeEach
    void limparEstadoAntesDeCadaTeste() {
        bancoEmMemoria.limparTudo();
    }

    @Test
    void deveExecutarFluxoBancarioCompletoViaRestAssuredComIdempotenciaELedgerImutavel() {
        // 1. Cadastrar Cliente Pessoa Física (PF)
        Map<String, Object> enderecoRequest = Map.of(
                "logradouro", "Avenida da Universidade",
                "numero", "2853",
                "complemento", "Campus Benfica",
                "bairro", "Benfica",
                "cidade", "Fortaleza",
                "uf", "CE",
                "cep", "60020-181"
        );

        Map<String, Object> cadastroPfRequest = Map.of(
                "nomeCompleto", "Danillo Evangelista",
                "cpf", "529.982.247-25",
                "dataNascimento", "1998-04-15",
                "email", "danillo@ufc.br",
                "telefone", "(85) 99988-7766",
                "endereco", enderecoRequest
        );

        String clientePfId = given()
                .contentType(ContentType.JSON)
                .body(cadastroPfRequest)
                .when().post("/api/v1/clientes/pf")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("tipoCliente", equalTo("PESSOA_FISICA"))
                .body("documentoFiscal", equalTo("52998224725"))
                .body("isentoTarifaPix", equalTo(true))
                .extract().path("id");

        // Deve rejeitar CPF duplicado com HTTP 409 Conflict
        given()
                .contentType(ContentType.JSON)
                .body(cadastroPfRequest)
                .when().post("/api/v1/clientes/pf")
                .then()
                .statusCode(409)
                .body("codigoErro", equalTo("CONFLITO_NEGOCIO"));

        // 2. Cadastrar Cliente Pessoa Jurídica (PJ)
        Map<String, Object> cadastroPjRequest = Map.of(
                "razaoSocial", "UFC Soluções Financeiras Digitais LTDA",
                "nomeFantasia", "UFC FinTech",
                "cnpj", "11.222.333/0001-81",
                "email", "tesouraria@ufcfintech.com.br",
                "telefone", "(85) 3366-7000",
                "endereco", enderecoRequest
        );

        String clientePjId = given()
                .contentType(ContentType.JSON)
                .body(cadastroPjRequest)
                .when().post("/api/v1/clientes/pj")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("tipoCliente", equalTo("PESSOA_JURIDICA"))
                .body("documentoFiscal", equalTo("11222333000181"))
                .body("isentoTarifaPix", equalTo(false))
                .extract().path("id");

        // 3. Abrir Contas Bancárias (ContaCorrente para PF e ContaEmpresarial para PJ)
        String contaPfId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("clienteTitularId", clientePfId, "codigoAgencia", "0001"))
                .when().post("/api/v1/contas")
                .then()
                .statusCode(201)
                .body("tipoConta", equalTo("CONTA_CORRENTE"))
                .body("statusConta", equalTo("ATIVA"))
                .body("codigoIspbBanco", equalTo("12345678"))
                .extract().path("id");

        String contaPjId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("clienteTitularId", clientePjId, "codigoAgencia", "0001"))
                .when().post("/api/v1/contas")
                .then()
                .statusCode(201)
                .body("tipoConta", equalTo("CONTA_EMPRESARIAL"))
                .body("statusConta", equalTo("ATIVA"))
                .extract().path("id");

        // 4. Registrar chave PIX (CNPJ) na Conta Empresarial PJ
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("tipoChave", "CNPJ", "valorChave", "11.222.333/0001-81"))
                .when().post("/api/v1/contas/" + contaPjId + "/chaves-pix")
                .then()
                .statusCode(201)
                .body("valorChave", equalTo("11222333000181"));

        // 5. Realizar Depósito de R$ 3.000,00 na Conta PF (com teste de Idempotência)
        Map<String, Object> depositoRequest = Map.of(
                "contaDestinoId", contaPfId,
                "valor", 3000.00,
                "canalOrigem", "TERMINAL_AUTOATENDIMENTO_UFC"
        );

        String transacaoDepositoId = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "idem-e2e-deposito-001")
                .body(depositoRequest)
                .when().post("/api/v1/operacoes/deposito")
                .then()
                .statusCode(201)
                .body("tipoOperacao", equalTo("DEPOSITO"))
                .body("statusTransacao", equalTo("CONCLUIDA"))
                .body("lancamentosPartidasDobradas.size()", equalTo(2))
                .extract().path("id");

        // Repetir a mesma requisição com a mesma Idempotency-Key: deve retornar a mesma transação sem duplicar o depósito!
        String transacaoDepositoRepetidaId = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "idem-e2e-deposito-001")
                .body(depositoRequest)
                .when().post("/api/v1/operacoes/deposito")
                .then()
                .statusCode(201)
                .extract().path("id");

        assertEquals(transacaoDepositoId, transacaoDepositoRepetidaId);

        // 6. Realizar Transferência PIX de PF -> PJ no valor de R$ 400,00 (Isento para PF)
        Map<String, Object> pixRequest = Map.of(
                "contaOrigemId", contaPfId,
                "chavePixDestino", "11222333000181",
                "valor", 400.00,
                "descricao", "Pagamento mensalidade software"
        );

        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "idem-e2e-pix-002")
                .body(pixRequest)
                .when().post("/api/v1/operacoes/pix")
                .then()
                .statusCode(201)
                .body("tipoOperacao", equalTo("PIX"))
                .body("valorTarifa", equalTo(0.0f))
                .body("endToEndIdPix", containsString("E12345678"))
                .body("lancamentosPartidasDobradas.size()", equalTo(2));

        // 7. Realizar Transferência TED de PF para banco externo no valor de R$ 500,00 (Tarifa fixa R$ 12,50)
        Map<String, Object> tedRequest = Map.of(
                "contaOrigemId", contaPfId,
                "codigoBancoDestino", "00000000",
                "codigoAgenciaDestino", "1234",
                "numeroContaDestino", "55667-8",
                "documentoFavorecido", "11222333000181",
                "valor", 500.00
        );

        String transacaoTedId = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "idem-e2e-ted-003")
                .body(tedRequest)
                .when().post("/api/v1/operacoes/ted")
                .then()
                .statusCode(201)
                .body("tipoOperacao", equalTo("TED"))
                .body("valorTarifa", equalTo(12.50f))
                .body("valorTotalDebitadoOrigem", equalTo(512.50f))
                .body("lancamentosPartidasDobradas.size()", equalTo(4))
                .extract().path("id");

        // 8. Realizar Saque de R$ 200,00 na Conta PF
        Map<String, Object> saqueRequest = Map.of(
                "contaOrigemId", contaPfId,
                "valor", 200.00,
                "codigoTerminalAtm", "ATM-BENFICA-01"
        );

        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "idem-e2e-saque-004")
                .body(saqueRequest)
                .when().post("/api/v1/operacoes/saque")
                .then()
                .statusCode(201)
                .body("tipoOperacao", equalTo("SAQUE"))
                .body("lancamentosPartidasDobradas.size()", equalTo(2));

        // 9. Teste de Saldo Insuficiente: Conta PF tem 3000 - 400 - 512.50 - 200 = R$ 1.887,50.
        // Tentar PIX de R$ 1.950,00 deve falhar com HTTP 422 SALDO_INSUFICIENTE
        given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", "idem-e2e-pix-sem-saldo")
                .body(Map.of(
                        "contaOrigemId", contaPfId,
                        "chavePixDestino", "11222333000181",
                        "valor", 1950.00,
                        "descricao", "Tentativa acima do saldo"
                ))
                .when().post("/api/v1/operacoes/pix")
                .then()
                .statusCode(422)
                .body("codigoErro", equalTo("SALDO_INSUFICIENTE"));

        // 10. Consultar Saldo Dinâmico no Ledger da Conta PF (R$ 1.887,50) e da Conta PJ (R$ 400,00)
        given()
                .when().get("/api/v1/contas/" + contaPfId + "/saldo")
                .then()
                .statusCode(200)
                .body("saldoDisponivel", equalTo(1887.50f))
                .body("totalCreditos", equalTo(3000.00f))
                .body("totalDebitos", equalTo(1112.50f))
                .body("quantidadeLancamentosProcessados", equalTo(5));

        given()
                .when().get("/api/v1/contas/" + contaPjId + "/saldo")
                .then()
                .statusCode(200)
                .body("saldoDisponivel", equalTo(400.00f))
                .body("totalCreditos", equalTo(400.00f))
                .body("totalDebitos", equalTo(0.0f))
                .body("quantidadeLancamentosProcessados", equalTo(1));

        // 11. Consultar Extrato Detalhado (JSON, CSV, TXT e Stream) e Auditoria da Transação TED
        given()
                .when().get("/api/v1/contas/" + contaPfId + "/extrato")
                .then()
                .statusCode(200)
                .body("saldoFinalDoPeriodo", equalTo(1887.50f))
                .body("movimentacoes.size()", equalTo(5));

        given()
                .when().get("/api/v1/contas/" + contaPfId + "/extrato/csv")
                .then()
                .statusCode(200)
                .body(containsString("data_hora;transacao_id;tipo_lancamento;descricao;valor;valor_com_sinal;saldo_apos_movimentacao"))
                .body(containsString("1887.50"));

        given()
                .when().get("/api/v1/contas/" + contaPfId + "/extrato/txt")
                .then()
                .statusCode(200)
                .body(containsString("CORE BANK SERVICE - EXTRATO BANCARIO"))
                .body(containsString("SALDO FINAL       : R$ 1887.50"));

        given()
                .when().get("/api/v1/transacoes/" + transacaoTedId)
                .then()
                .statusCode(200)
                .body("id", equalTo(transacaoTedId))
                .body("lancamentosPartidasDobradas.size()", equalTo(4));
    }

    @Test
    void deveOrquestrarFluxoReativoMutinyComUniAssertSubscriber() {
        Endereco endereco = new Endereco(
                "Rua Doutor Gilvan Maia",
                "100",
                "Bloco IUVI",
                "Pici",
                "Fortaleza",
                "CE",
                "60455-760"
        );

        // 1. Cadastro reativo de PF e PJ verificando emissões com UniAssertSubscriber
        PessoaFisica clientePf = executarNoContextoVertx(() -> clienteService.cadastrarPessoaFisica(
                        "Mariana Alencar",
                        "529.982.247-25",
                        LocalDate.of(1997, 9, 10),
                        "mariana.alencar@ufc.br",
                        "85988776655",
                        endereco
                ))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        PessoaJuridica clientePj = executarNoContextoVertx(() -> clienteService.cadastrarPessoaJuridica(
                        "Editora Academica Ceara LTDA",
                        "EdCeara",
                        "11.222.333/0001-81",
                        "contato@edceara.com.br",
                        "8532110099",
                        endereco
                ))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // 2. Abertura reativa de contas e registro de chave PIX
        Conta contaPf = executarNoContextoVertx(() -> contaService.abrirContaParaCliente(clientePf.getId(), "0001"))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        Conta contaPj = executarNoContextoVertx(() -> contaService.abrirContaParaCliente(clientePj.getId(), "0001"))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        ChavePix chavePixPf = executarNoContextoVertx(() ->
                        contaService.registrarChavePix(contaPf.getId(), TipoChavePix.EMAIL, "mariana.alencar@ufc.br")
                )
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();
        assertNotNull(chavePixPf.getId());

        // 3. Depósito na Conta Empresarial PJ (R$ 2.000,00) e PIX PJ -> PF (R$ 600,00 com tarifa de 0.50% = R$ 3,00)
        executarNoContextoVertx(() -> motorTransacaoService.realizarDeposito(
                        "idem-mutiny-dep-pj",
                        contaPj.getId(),
                        new BigDecimal("2000.00"),
                        "TED_RECEBIDA_TESOURARIA"
                ))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertCompleted();

        Transacao pixPjParaPf = executarNoContextoVertx(() -> motorTransacaoService.realizarTransferenciaPix(
                        "idem-mutiny-pix-pj",
                        contaPj.getId(),
                        "mariana.alencar@ufc.br",
                        new BigDecimal("600.00"),
                        "Bolsa de Pesquisa POO"
                ))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertEquals(new BigDecimal("3.00"), pixPjParaPf.getValorTarifa());
        assertEquals(4, pixPjParaPf.getLancamentos().size());

        // 4. Verificação de falha reativa com UniAssertSubscriber quando saldo é insuficiente
        executarNoContextoVertx(() -> motorTransacaoService.realizarSaque(
                        "idem-mutiny-saque-falha",
                        contaPf.getId(),
                        new BigDecimal("900.00"),
                        "ATM-UFC-01"
                ))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(SaldoInsuficienteException.class);

        // 5. Consulta reativa de saldo da Conta PJ (2000 - 600 - 3 = 1397.00) e stream Multi de extrato
        ResumoSaldoConta resumoPj = executarNoContextoVertx(() -> saldoService.consultarResumoSaldo(contaPj.getId()))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertEquals(new BigDecimal("1397.00"), resumoPj.saldoDisponivel());
        assertEquals(3L, resumoPj.quantidadeLancamentos());

        List<ItemExtratoConta> itensStreamPj = executarMultiNoContextoVertx(() -> extratoService.streamItensExtrato(
                        contaPj.getId(),
                        null,
                        null,
                        null
                ))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitCompletion()
                .getItems();

        assertEquals(3, itensStreamPj.size());
    }
}
