package br.ufc.poo.corebank.domain.export;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.ledger.ExtratoConta;
import br.ufc.poo.corebank.domain.model.ledger.ItemExtratoConta;

public class ExportadorExtratoTexto implements ExportadorExtrato {

    private static final String LINHA_DIVISORIA = "======================================================================";
    private static final String QUEBRA_LINHA = "\n";

    @Override
    public String exportar(ExtratoConta extratoConta) {
        if (extratoConta == null) {
            throw new RegraNegocioException("O extrato da conta é obrigatório para exportação em texto.");
        }

        StringBuilder comprovante = new StringBuilder();
        comprovante.append(LINHA_DIVISORIA).append(QUEBRA_LINHA);
        comprovante.append("                CORE BANK SERVICE - EXTRATO BANCARIO                  ").append(QUEBRA_LINHA);
        comprovante.append(LINHA_DIVISORIA).append(QUEBRA_LINHA);
        comprovante.append("Agencia: ").append(extratoConta.getCodigoAgencia())
                .append(" | Conta: ").append(extratoConta.getNumeroConta()).append(QUEBRA_LINHA);
        comprovante.append("Titular: ").append(extratoConta.getNomeTitular())
                .append(" (Doc: ").append(extratoConta.getDocumentoTitular()).append(")").append(QUEBRA_LINHA);
        comprovante.append("----------------------------------------------------------------------").append(QUEBRA_LINHA);
        comprovante.append("SALDO ANTERIOR: R$ ").append(extratoConta.getSaldoAnteriorAoPeriodo()).append(QUEBRA_LINHA);
        comprovante.append("----------------------------------------------------------------------").append(QUEBRA_LINHA);

        if (extratoConta.getMovimentacoes().isEmpty()) {
            comprovante.append("Nenhuma movimentacao registrada no periodo consultado.").append(QUEBRA_LINHA);
        } else {
            for (ItemExtratoConta itemAtual : extratoConta.getMovimentacoes()) {
                comprovante.append(itemAtual.criadoEm())
                        .append(" | ").append(String.format("%-7s", itemAtual.tipoLancamento()))
                        .append(" | R$ ").append(String.format("%10s", itemAtual.valorComSinalAlgebrico()))
                        .append(" | Saldo: R$ ").append(String.format("%10s", itemAtual.saldoAposMovimentacao()))
                        .append(" | ").append(itemAtual.descricao())
                        .append(QUEBRA_LINHA);
            }
        }

        comprovante.append("----------------------------------------------------------------------").append(QUEBRA_LINHA);
        comprovante.append("TOTAL ENTRADAS (+): R$ ").append(extratoConta.getTotalEntradasCreditos()).append(QUEBRA_LINHA);
        comprovante.append("TOTAL SAIDAS   (-): R$ ").append(extratoConta.getTotalSaidasDebitos()).append(QUEBRA_LINHA);
        comprovante.append("SALDO FINAL       : R$ ").append(extratoConta.getSaldoFinalDoPeriodo()).append(QUEBRA_LINHA);
        comprovante.append(LINHA_DIVISORIA).append(QUEBRA_LINHA);

        return comprovante.toString();
    }

    @Override
    public String obterContentType() {
        return "text/plain; charset=UTF-8";
    }

    @Override
    public String obterExtensaoArquivo() {
        return "txt";
    }
}
