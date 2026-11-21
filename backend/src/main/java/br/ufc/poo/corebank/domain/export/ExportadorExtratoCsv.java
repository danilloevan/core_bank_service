package br.ufc.poo.corebank.domain.export;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.ledger.ExtratoConta;
import br.ufc.poo.corebank.domain.model.ledger.ItemExtratoConta;

public class ExportadorExtratoCsv implements ExportadorExtrato {

    private static final String SEPARADOR_CSV = ";";
    private static final String QUEBRA_LINHA = "\n";

    @Override
    public String exportar(ExtratoConta extratoConta) {
        if (extratoConta == null) {
            throw new RegraNegocioException("O extrato da conta é obrigatório para exportação CSV.");
        }

        StringBuilder construtorCsv = new StringBuilder();
        construtorCsv.append("# CORE BANK SERVICE - EXTRATO CONTABIL").append(QUEBRA_LINHA);
        construtorCsv.append("# Agencia").append(SEPARADOR_CSV).append(extratoConta.getCodigoAgencia())
                .append(SEPARADOR_CSV).append("Conta").append(SEPARADOR_CSV).append(extratoConta.getNumeroConta())
                .append(SEPARADOR_CSV).append("Titular").append(SEPARADOR_CSV).append(limparSeparador(extratoConta.getNomeTitular()))
                .append(QUEBRA_LINHA);
        construtorCsv.append("# Saldo Anterior").append(SEPARADOR_CSV).append(extratoConta.getSaldoAnteriorAoPeriodo())
                .append(SEPARADOR_CSV).append("Total Entradas").append(SEPARADOR_CSV).append(extratoConta.getTotalEntradasCreditos())
                .append(SEPARADOR_CSV).append("Total Saidas").append(SEPARADOR_CSV).append(extratoConta.getTotalSaidasDebitos())
                .append(SEPARADOR_CSV).append("Saldo Final").append(SEPARADOR_CSV).append(extratoConta.getSaldoFinalDoPeriodo())
                .append(QUEBRA_LINHA);

        construtorCsv.append("data_hora")
                .append(SEPARADOR_CSV).append("transacao_id")
                .append(SEPARADOR_CSV).append("tipo_lancamento")
                .append(SEPARADOR_CSV).append("descricao")
                .append(SEPARADOR_CSV).append("valor")
                .append(SEPARADOR_CSV).append("valor_com_sinal")
                .append(SEPARADOR_CSV).append("saldo_apos_movimentacao")
                .append(QUEBRA_LINHA);

        for (ItemExtratoConta itemAtual : extratoConta.getMovimentacoes()) {
            construtorCsv.append(itemAtual.criadoEm())
                    .append(SEPARADOR_CSV).append(itemAtual.transacaoId())
                    .append(SEPARADOR_CSV).append(itemAtual.tipoLancamento())
                    .append(SEPARADOR_CSV).append(limparSeparador(itemAtual.descricao()))
                    .append(SEPARADOR_CSV).append(itemAtual.valor())
                    .append(SEPARADOR_CSV).append(itemAtual.valorComSinalAlgebrico())
                    .append(SEPARADOR_CSV).append(itemAtual.saldoAposMovimentacao())
                    .append(QUEBRA_LINHA);
        }

        return construtorCsv.toString();
    }

    private String limparSeparador(String textoOriginal) {
        if (textoOriginal == null) {
            return "";
        }
        return textoOriginal.replace(SEPARADOR_CSV, ",").replace("\n", " ").replace("\r", "");
    }

    @Override
    public String obterContentType() {
        return "text/csv; charset=UTF-8";
    }

    @Override
    public String obterExtensaoArquivo() {
        return "csv";
    }
}
