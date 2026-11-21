package br.ufc.poo.corebank.domain.model.transacao;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.conta.Conta;
import br.ufc.poo.corebank.domain.model.conta.TipoOperacao;
import br.ufc.poo.corebank.domain.model.ledger.LancamentoContabil;
import br.ufc.poo.corebank.domain.model.ledger.TipoLancamento;
import br.ufc.poo.corebank.domain.strategy.EstrategiaTarifa;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Entity
@DiscriminatorValue("TED")
public class TransacaoTed extends Transacao {

    public static final LocalTime HORARIO_ABERTURA_JANELA_TED = LocalTime.of(6, 30);
    public static final LocalTime HORARIO_LIMITE_TED = LocalTime.of(17, 0);
    public static final BigDecimal TARIFA_FIXA_TED_PADRAO = new BigDecimal("12.50");

    @Column(name = "codigo_banco_destino", length = 15)
    private String codigoBancoDestino;

    @Column(name = "codigo_agencia_destino", length = 10)
    private String codigoAgenciaDestino;

    @Column(name = "numero_conta_destino", length = 20)
    private String numeroContaDestino;

    @Column(name = "documento_favorecido", length = 14)
    private String documentoFavorecido;

    protected TransacaoTed() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public TransacaoTed(
            String chaveIdempotencia,
            Conta contaOrigem,
            Conta contaDestinoInternaOpcional,
            BigDecimal valorTed,
            String codigoBancoDestino,
            String codigoAgenciaDestino,
            String numeroContaDestino,
            String documentoFavorecido,
            EstrategiaTarifa estrategiaTarifa) {
        super(chaveIdempotencia, valorTed, contaOrigem, contaDestinoInternaOpcional, estrategiaTarifa);
        validarDadosBancariosDestinoTed(
                contaOrigem,
                codigoBancoDestino,
                codigoAgenciaDestino,
                numeroContaDestino,
                documentoFavorecido
        );
        this.codigoBancoDestino = codigoBancoDestino.trim();
        this.codigoAgenciaDestino = codigoAgenciaDestino.trim();
        this.numeroContaDestino = numeroContaDestino.trim();
        this.documentoFavorecido = documentoFavorecido.replaceAll("\\D", "");
    }

    public static boolean isDentroDaJanelaOperacionalTed(LocalTime horarioReferencia) {
        if (horarioReferencia == null) {
            return false;
        }
        return !horarioReferencia.isBefore(HORARIO_ABERTURA_JANELA_TED)
                && !horarioReferencia.isAfter(HORARIO_LIMITE_TED);
    }

    private void validarDadosBancariosDestinoTed(
            Conta contaOrigemInformada,
            String bancoInformado,
            String agenciaInformada,
            String contaInformada,
            String documentoInformado) {
        if (contaOrigemInformada == null) {
            throw new RegraNegocioException("A conta de origem da TED é obrigatória.");
        }
        if (bancoInformado == null || bancoInformado.isBlank()) {
            throw new RegraNegocioException("O código do banco ou ISPB de destino da TED é obrigatório.");
        }
        if (agenciaInformada == null || agenciaInformada.isBlank()) {
            throw new RegraNegocioException("O código da agência de destino da TED é obrigatório.");
        }
        if (contaInformada == null || contaInformada.isBlank()) {
            throw new RegraNegocioException("O número da conta de destino da TED é obrigatório.");
        }
        String documentoLimpo = documentoInformado != null ? documentoInformado.replaceAll("\\D", "") : "";
        if (documentoLimpo.length() != 11 && documentoLimpo.length() != 14) {
            throw new RegraNegocioException("O documento do favorecido da TED deve ser um CPF (11) ou CNPJ (14) válido.");
        }
    }

    @Override
    public TipoOperacao obterTipoOperacao() {
        return TipoOperacao.TED;
    }

    @Override
    protected void validarRegrasEspecificas(ContextoTransacao contextoTransacao) {
        if (contaDestino != null && contaOrigem.getNumeroConta().equals(contaDestino.getNumeroConta())) {
            throw new RegraNegocioException("Não é permitido enviar TED para a própria conta de origem.");
        }
        if (contaDestino == null && contextoTransacao.getContaLiquidacaoExterna() == null) {
            throw new RegraNegocioException(
                    "A Conta Técnica de Liquidação Externa é obrigatória para liquidar TED externa (STR)."
            );
        }
    }

    @Override
    protected List<LancamentoContabil> gerarLancamentosEspecificos(ContextoTransacao contextoTransacao) {
        Conta contaContrapartidaCredito = (contaDestino != null)
                ? contaDestino
                : contextoTransacao.getContaLiquidacaoExterna();

        String identificacaoDestino = "Banco " + codigoBancoDestino
                + " Ag " + codigoAgenciaDestino
                + " Cc " + numeroContaDestino;

        LancamentoContabil debitoPrincipalOrigem = new LancamentoContabil(
                getId(),
                contaOrigem,
                TipoLancamento.DEBITO,
                valorOperacao,
                "TED enviada para " + identificacaoDestino
        );

        LancamentoContabil creditoPrincipalDestino = new LancamentoContabil(
                getId(),
                contaContrapartidaCredito,
                TipoLancamento.CREDITO,
                valorOperacao,
                "TED recebida de conta " + contaOrigem.getNumeroConta()
        );

        return List.of(debitoPrincipalOrigem, creditoPrincipalDestino);
    }

    public String getCodigoBancoDestino() {
        return codigoBancoDestino;
    }

    public String getCodigoAgenciaDestino() {
        return codigoAgenciaDestino;
    }

    public String getNumeroContaDestino() {
        return numeroContaDestino;
    }

    public String getDocumentoFavorecido() {
        return documentoFavorecido;
    }
}
