package br.ufc.poo.corebank.domain.export;

import br.ufc.poo.corebank.domain.model.ledger.ExtratoConta;

public interface ExportadorExtrato {

    String exportar(ExtratoConta extratoConta);

    String obterContentType();

    String obterExtensaoArquivo();
}
