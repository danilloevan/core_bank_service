package br.ufc.poo.corebank.infrastructure.bootstrap;

import br.ufc.poo.corebank.domain.model.conta.Agencia;
import br.ufc.poo.corebank.domain.model.conta.ContaTecnica;
import br.ufc.poo.corebank.domain.model.conta.TipoContaTecnica;
import br.ufc.poo.corebank.infrastructure.repository.AgenciaRepository;
import br.ufc.poo.corebank.infrastructure.repository.ContaRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CoreBankBootstrap {

    public static final String NUMERO_CONTA_CAIXA_TESOURARIA = "000001-0";
    public static final String NUMERO_CONTA_RECEITA_TARIFAS = "000002-0";
    public static final String NUMERO_CONTA_LIQUIDACAO_EXTERNA = "000003-0";

    private final AgenciaRepository agenciaRepository;
    private final ContaRepository contaRepository;

    public CoreBankBootstrap(AgenciaRepository agenciaRepository, ContaRepository contaRepository) {
        this.agenciaRepository = agenciaRepository;
        this.contaRepository = contaRepository;
    }

    @WithTransaction
    public Uni<Agencia> garantirInfraestruturaContabilBasica() {
        return agenciaRepository.obterOuCriarAgenciaMatriz()
                .onItem().transformToUni(agenciaMatriz ->
                        garantirContaTecnica(agenciaMatriz, TipoContaTecnica.CAIXA_TESOURARIA, NUMERO_CONTA_CAIXA_TESOURARIA)
                                .onItem().transformToUni(contaCaixa ->
                                        garantirContaTecnica(agenciaMatriz, TipoContaTecnica.RECEITA_TARIFAS, NUMERO_CONTA_RECEITA_TARIFAS))
                                .onItem().transformToUni(contaTarifas ->
                                        garantirContaTecnica(agenciaMatriz, TipoContaTecnica.LIQUIDACAO_EXTERNA, NUMERO_CONTA_LIQUIDACAO_EXTERNA))
                                .onItem().transform(contaLiquidacao -> agenciaMatriz)
                );
    }

    private Uni<ContaTecnica> garantirContaTecnica(
            Agencia agenciaMatriz,
            TipoContaTecnica tipoContaTecnica,
            String numeroContaTecnica) {
        return contaRepository.buscarContaTecnicaPorTipo(tipoContaTecnica)
                .onItem().ifNull().switchTo(() -> {
                    ContaTecnica novaContaTecnica = new ContaTecnica(
                            agenciaMatriz,
                            tipoContaTecnica,
                            numeroContaTecnica
                    );
                    agenciaMatriz.adicionarConta(novaContaTecnica);
                    return contaRepository.persist(novaContaTecnica)
                            .onItem().transform(contaPersistida -> novaContaTecnica);
                });
    }
}
