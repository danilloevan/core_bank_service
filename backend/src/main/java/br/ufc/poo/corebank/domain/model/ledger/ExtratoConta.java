package br.ufc.poo.corebank.domain.model.ledger;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.conta.Conta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ExtratoConta {

    private final UUID contaId;
    private final String codigoAgencia;
    private final String numeroConta;
    private final String nomeTitular;
    private final String documentoTitular;
    private final Instant dataInicioFiltro;
    private final Instant dataFimFiltro;
    private final BigDecimal saldoAnteriorAoPeriodo;
    private final BigDecimal totalEntradasCreditos;
    private final BigDecimal totalSaidasDebitos;
    private final BigDecimal saldoFinalDoPeriodo;
    private final List<ItemExtratoConta> movimentacoes;
    private final Instant emitidoEm;

    private ExtratoConta(
            UUID contaId,
            String codigoAgencia,
            String numeroConta,
            String nomeTitular,
            String documentoTitular,
            Instant dataInicioFiltro,
            Instant dataFimFiltro,
            BigDecimal saldoAnteriorAoPeriodo,
            BigDecimal totalEntradasCreditos,
            BigDecimal totalSaidasDebitos,
            BigDecimal saldoFinalDoPeriodo,
            List<ItemExtratoConta> movimentacoes,
            Instant emitidoEm) {
        this.contaId = contaId;
        this.codigoAgencia = codigoAgencia;
        this.numeroConta = numeroConta;
        this.nomeTitular = nomeTitular;
        this.documentoTitular = documentoTitular;
        this.dataInicioFiltro = dataInicioFiltro;
        this.dataFimFiltro = dataFimFiltro;
        this.saldoAnteriorAoPeriodo = saldoAnteriorAoPeriodo;
        this.totalEntradasCreditos = totalEntradasCreditos;
        this.totalSaidasDebitos = totalSaidasDebitos;
        this.saldoFinalDoPeriodo = saldoFinalDoPeriodo;
        this.movimentacoes = Collections.unmodifiableList(new ArrayList<>(movimentacoes));
        this.emitidoEm = emitidoEm;
    }

    public static ExtratoConta construirExtrato(
            Conta conta,
            Instant dataInicioFiltro,
            Instant dataFimFiltro,
            BigDecimal saldoAnteriorInformado,
            List<LancamentoContabil> lancamentosNoPeriodo) {
        if (conta == null) {
            throw new RegraNegocioException("A conta bancária é obrigatória para construção do extrato.");
        }
        if (dataInicioFiltro != null && dataFimFiltro != null && dataInicioFiltro.isAfter(dataFimFiltro)) {
            throw new RegraNegocioException("A data inicial do filtro de extrato não pode ser posterior à data final.");
        }

        BigDecimal saldoAnteriorNormalizado = LancamentoContabil.normalizarValorMonetario(saldoAnteriorInformado);
        BigDecimal totalCreditosAcumulado = LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);
        BigDecimal totalDebitosAcumulado = LancamentoContabil.normalizarValorMonetario(BigDecimal.ZERO);
        BigDecimal saldoProgressivo = saldoAnteriorNormalizado;

        List<LancamentoContabil> lancamentosOrdenados = (lancamentosNoPeriodo != null)
                ? lancamentosNoPeriodo.stream()
                        .sorted(Comparator.comparing(LancamentoContabil::getCriadoEm))
                        .toList()
                : List.of();

        List<ItemExtratoConta> itensCalculados = new ArrayList<>(lancamentosOrdenados.size());

        for (LancamentoContabil lancamentoAtual : lancamentosOrdenados) {
            BigDecimal valorComSinal = lancamentoAtual.obterValorComSinalAlgebrico();
            saldoProgressivo = LancamentoContabil.normalizarValorMonetario(saldoProgressivo.add(valorComSinal));

            if (lancamentoAtual.getTipoLancamento() == TipoLancamento.CREDITO) {
                totalCreditosAcumulado = LancamentoContabil.normalizarValorMonetario(
                        totalCreditosAcumulado.add(lancamentoAtual.getValor())
                );
            } else {
                totalDebitosAcumulado = LancamentoContabil.normalizarValorMonetario(
                        totalDebitosAcumulado.add(lancamentoAtual.getValor())
                );
            }

            itensCalculados.add(new ItemExtratoConta(
                    lancamentoAtual.getId(),
                    lancamentoAtual.getTransacaoId(),
                    lancamentoAtual.getTipoLancamento(),
                    lancamentoAtual.getValor(),
                    valorComSinal,
                    saldoProgressivo,
                    lancamentoAtual.getDescricao(),
                    lancamentoAtual.getCriadoEm()
            ));
        }

        String nomeTitular = conta.getTitular() != null ? conta.getTitular().getNome() : "BANCO CORE (CONTA TÉCNICA)";
        String documentoTitular = conta.getTitular() != null ? conta.getTitular().getDocumentoFiscal() : Conta.CODIGO_ISPB_BANCO;

        return new ExtratoConta(
                conta.getId(),
                conta.getAgencia().getCodigo(),
                conta.getNumeroConta(),
                nomeTitular,
                documentoTitular,
                dataInicioFiltro,
                dataFimFiltro,
                saldoAnteriorNormalizado,
                totalCreditosAcumulado,
                totalDebitosAcumulado,
                saldoProgressivo,
                itensCalculados,
                Instant.now()
        );
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getCodigoAgencia() {
        return codigoAgencia;
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public String getNomeTitular() {
        return nomeTitular;
    }

    public String getDocumentoTitular() {
        return documentoTitular;
    }

    public Instant getDataInicioFiltro() {
        return dataInicioFiltro;
    }

    public Instant getDataFimFiltro() {
        return dataFimFiltro;
    }

    public BigDecimal getSaldoAnteriorAoPeriodo() {
        return saldoAnteriorAoPeriodo;
    }

    public BigDecimal getTotalEntradasCreditos() {
        return totalEntradasCreditos;
    }

    public BigDecimal getTotalSaidasDebitos() {
        return totalSaidasDebitos;
    }

    public BigDecimal getSaldoFinalDoPeriodo() {
        return saldoFinalDoPeriodo;
    }

    public List<ItemExtratoConta> getMovimentacoes() {
        return movimentacoes;
    }

    public Instant getEmitidoEm() {
        return emitidoEm;
    }
}
