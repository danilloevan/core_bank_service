package br.ufc.poo.corebank.domain.model.conta;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.cliente.Cliente;
import br.ufc.poo.corebank.domain.model.cliente.TipoCliente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chaves_pix")
public class ChavePix {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_chave", nullable = false, length = 20)
    private TipoChavePix tipoChave;

    @Column(name = "valor_chave", nullable = false, unique = true, length = 150)
    private String valorChave;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    protected ChavePix() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public ChavePix(TipoChavePix tipoChave, String valorChaveInformado, Conta contaVinculada) {
        if (tipoChave == null) {
            throw new RegraNegocioException("O tipo da chave PIX é obrigatório.");
        }
        if (contaVinculada == null) {
            throw new RegraNegocioException("A conta bancária vinculada à chave PIX é obrigatória.");
        }
        if (!contaVinculada.isAtiva()) {
            throw new RegraNegocioException("Somente contas ativas podem registrar chaves PIX.");
        }
        if (contaVinculada.obterTipoConta() == TipoConta.CONTA_TECNICA) {
            throw new RegraNegocioException("Contas técnicas internas não podem registrar chaves PIX.");
        }

        this.tipoChave = tipoChave;
        this.conta = contaVinculada;
        this.valorChave = normalizarEValidarValorChave(tipoChave, valorChaveInformado, contaVinculada);
        this.criadaEm = Instant.now();
    }

    private String normalizarEValidarValorChave(
            TipoChavePix tipoChaveEscolhido,
            String valorBruto,
            Conta contaVinculada) {
        Cliente titularConta = contaVinculada.getTitular();

        switch (tipoChaveEscolhido) {
            case ALEATORIA -> {
                if (valorBruto == null || valorBruto.isBlank()) {
                    return UUID.randomUUID().toString();
                }
                return valorBruto.trim();
            }
            case CPF -> {
                if (titularConta.obterTipoCliente() != TipoCliente.PESSOA_FISICA) {
                    throw new RegraNegocioException("Chave PIX do tipo CPF só pode ser registrada em conta de Pessoa Física.");
                }
                String cpfLimpo = Cliente.extrairSomenteDigitos(valorBruto);
                if (!cpfLimpo.equals(titularConta.getDocumentoFiscal())) {
                    throw new RegraNegocioException("A chave PIX do tipo CPF deve corresponder ao CPF do titular da conta.");
                }
                return cpfLimpo;
            }
            case CNPJ -> {
                if (titularConta.obterTipoCliente() != TipoCliente.PESSOA_JURIDICA) {
                    throw new RegraNegocioException("Chave PIX do tipo CNPJ só pode ser registrada em conta de Pessoa Jurídica.");
                }
                String cnpjLimpo = Cliente.extrairSomenteDigitos(valorBruto);
                if (!cnpjLimpo.equals(titularConta.getDocumentoFiscal())) {
                    throw new RegraNegocioException("A chave PIX do tipo CNPJ deve corresponder ao CNPJ do titular da conta.");
                }
                return cnpjLimpo;
            }
            case EMAIL -> {
                if (valorBruto == null || !valorBruto.contains("@")) {
                    throw new RegraNegocioException("O e-mail informado para a chave PIX é inválido.");
                }
                return valorBruto.trim().toLowerCase();
            }
            case TELEFONE -> {
                String telefoneLimpo = Cliente.extrairSomenteDigitos(valorBruto);
                if (telefoneLimpo.length() < 10 || telefoneLimpo.length() > 13) {
                    throw new RegraNegocioException("O telefone informado para a chave PIX deve ter entre 10 e 13 dígitos.");
                }
                return telefoneLimpo;
            }
            default -> throw new RegraNegocioException("Tipo de chave PIX não suportado: " + tipoChaveEscolhido);
        }
    }

    public UUID getId() {
        return id;
    }

    public TipoChavePix getTipoChave() {
        return tipoChave;
    }

    public String getValorChave() {
        return valorChave;
    }

    public Conta getConta() {
        return conta;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
