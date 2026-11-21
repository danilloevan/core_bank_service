package br.ufc.poo.corebank.domain.model.transacao;

import br.ufc.poo.corebank.domain.exception.LimiteOperacionalExcedidoException;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Entity
@DiscriminatorValue("PIX")
public class TransacaoPix extends Transacao {

    public static final BigDecimal LIMITE_NOTURNO_PADRAO_PIX = new BigDecimal("1000.00");
    public static final LocalTime HORARIO_INICIO_PERIODO_NOTURNO = LocalTime.of(20, 0);
    public static final LocalTime HORARIO_FIM_PERIODO_NOTURNO = LocalTime.of(6, 0);
    private static final DateTimeFormatter FORMATADOR_DATA_END_TO_END = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    @Column(name = "end_to_end_id", unique = true, length = 45)
    private String endToEndId;

    @Column(name = "chave_pix_destino", length = 150)
    private String chavePixDestino;

    @Column(name = "descricao_pix", length = 200)
    private String descricaoPix;

    protected TransacaoPix() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public TransacaoPix(
            String chaveIdempotencia,
            Conta contaOrigem,
            Conta contaDestinoOpcional,
            BigDecimal valorPix,
            String chavePixDestino,
            String descricaoPix,
            EstrategiaTarifa estrategiaTarifa) {
        super(chaveIdempotencia, valorPix, contaOrigem, contaDestinoOpcional, estrategiaTarifa);
        if (contaOrigem == null) {
            throw new RegraNegocioException("A conta de origem do PIX é obrigatória.");
        }
        if (chavePixDestino == null || chavePixDestino.isBlank()) {
            throw new RegraNegocioException("A chave PIX de destino é obrigatória.");
        }
        this.chavePixDestino = chavePixDestino.trim();
        this.descricaoPix = (descricaoPix != null && !descricaoPix.isBlank())
                ? descricaoPix.trim()
                : "Transferência PIX para chave " + this.chavePixDestino;
        this.endToEndId = gerarEndToEndId(Conta.CODIGO_ISPB_BANCO);
    }

    public static String gerarEndToEndId(String codigoIspbBanco) {
        String ispbNormalizado = (codigoIspbBanco != null && codigoIspbBanco.matches("\\d{8}"))
                ? codigoIspbBanco
                : Conta.CODIGO_ISPB_BANCO;
        String carimboDataHoraUtc = LocalDateTime.now(ZoneOffset.UTC).format(FORMATADOR_DATA_END_TO_END);
        String sufixoAleatorio = UUID.randomUUID().toString().replace("-", "").substring(0, 11).toUpperCase();
        return "E" + ispbNormalizado + carimboDataHoraUtc + sufixoAleatorio;
    }

    public static boolean isHorarioNoturno(LocalTime horarioReferencia) {
        if (horarioReferencia == null) {
            return false;
        }
        return !horarioReferencia.isBefore(HORARIO_INICIO_PERIODO_NOTURNO)
                || horarioReferencia.isBefore(HORARIO_FIM_PERIODO_NOTURNO);
    }

    @Override
    public TipoOperacao obterTipoOperacao() {
        return TipoOperacao.PIX;
    }

    @Override
    protected void validarRegrasEspecificas(ContextoTransacao contextoTransacao) {
        if (contaDestino != null &&mesmaConta(contaOrigem, contaDestino)) {
            throw new RegraNegocioException("Não é permitido realizar transferência PIX para a própria conta de origem.");
        }
        if (contaDestino == null && contextoTransacao.getContaLiquidacaoExterna() == null) {
            throw new RegraNegocioException(
                    "A Conta Técnica de Liquidação Externa é obrigatória para liquidar PIX destinado a outra instituição."
            );
        }

        LocalTime horarioOperacao = contextoTransacao.getDataHoraReferencia().toLocalTime();
        if (isHorarioNoturno(horarioOperacao)) {
            BigDecimal totalNoturnoProjetado = contextoTransacao.getTotalMovimentadoNoDiaNaOperacao()
                    .add(this.valorOperacao);
            if (totalNoturnoProjetado.compareTo(LIMITE_NOTURNO_PADRAO_PIX) > 0) {
                throw new LimiteOperacionalExcedidoException(
                        "O valor do PIX em horário noturno (20h às 06h) excede o limite de segurança de R$ "
                                + LIMITE_NOTURNO_PADRAO_PIX + "."
                );
            }
        }
    }

    private boolean mesmaConta(Conta primeiraConta, Conta segundaConta) {
        if (primeiraConta.getId() != null && segundaConta.getId() != null) {
            return primeiraConta.getId().equals(segundaConta.getId());
        }
        return primeiraConta.getNumeroConta().equals(segundaConta.getNumeroConta());
    }

    @Override
    protected List<LancamentoContabil> gerarLancamentosEspecificos(ContextoTransacao contextoTransacao) {
        Conta contaContrapartidaCredito = (contaDestino != null)
                ? contaDestino
                : contextoTransacao.getContaLiquidacaoExterna();

        LancamentoContabil debitoContaOrigem = new LancamentoContabil(
                getId(),
                contaOrigem,
                TipoLancamento.DEBITO,
                valorOperacao,
                "PIX enviado [" + endToEndId + "] para chave " + chavePixDestino
        );

        LancamentoContabil creditoContrapartida = new LancamentoContabil(
                getId(),
                contaContrapartidaCredito,
                TipoLancamento.CREDITO,
                valorOperacao,
                "PIX recebido [" + endToEndId + "] de conta " + contaOrigem.getNumeroConta()
        );

        return List.of(debitoContaOrigem, creditoContrapartida);
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public String getChavePixDestino() {
        return chavePixDestino;
    }

    public String getDescricaoPix() {
        return descricaoPix;
    }
}
