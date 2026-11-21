package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.StatusConta;
import br.ufc.poo.corebank.domain.model.conta.TipoConta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContaResponse(
        UUID id,
        String codigoIspbBanco,
        String codigoAgencia,
        String nomeAgencia,
        String numeroConta,
        TipoConta tipoConta,
        StatusConta statusConta,
        UUID titularId,
        String nomeTitular,
        String documentoTitular,
        BigDecimal limiteSaqueDiario,
        BigDecimal tarifaManutencaoMensal,
        Instant abertaEm
) {
    public static ContaResponse deDominio(Conta conta) {
        UUID titularId = conta.getTitular() != null ? conta.getTitular().getId() : null;
        String nomeTitular = conta.getTitular() != null ? conta.getTitular().getNome() : "BANCO CORE (CONTA TÉCNICA)";
        String documentoTitular = conta.getTitular() != null ? conta.getTitular().getDocumentoFiscal() : null;

        return new ContaResponse(
                conta.getId(),
                Conta.CODIGO_ISPB_BANCO,
                conta.getAgencia().getCodigo(),
                conta.getAgencia().getNome(),
                conta.getNumeroConta(),
                conta.obterTipoConta(),
                conta.getStatusConta(),
                titularId,
                nomeTitular,
                documentoTitular,
                conta.obterLimiteSaqueDiario(),
                conta.obterTarifaManutencaoMensal(),
                conta.getAbertaEm()
        );
    }
}
