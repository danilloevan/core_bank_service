package br.ufc.poo.corebank.api.dto;

import br.ufc.poo.corebank.domain.model.conta.ChavePix;
import br.ufc.poo.corebank.domain.model.conta.TipoChavePix;

import java.time.Instant;
import java.util.UUID;

public record ChavePixResponse(
        UUID id,
        TipoChavePix tipoChave,
        String valorChave,
        UUID contaId,
        String numeroConta,
        Instant criadaEm
) {
    public static ChavePixResponse deDominio(ChavePix chavePix) {
        return new ChavePixResponse(
                chavePix.getId(),
                chavePix.getTipoChave(),
                chavePix.getValorChave(),
                chavePix.getConta().getId(),
                chavePix.getConta().getNumeroConta(),
                chavePix.getCriadaEm()
        );
    }
}
