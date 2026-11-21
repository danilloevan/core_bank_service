package br.ufc.poo.corebank.domain.model.cliente;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.conta.TipoConta;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clientes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_cliente", discriminatorType = DiscriminatorType.STRING, length = 30)
public abstract class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 150)
    protected String nome;

    @Column(name = "documento_fiscal", nullable = false, unique = true, length = 14)
    protected String documentoFiscal;

    @Column(name = "email", nullable = false, length = 150)
    protected String email;

    @Column(name = "telefone", nullable = false, length = 20)
    protected String telefone;

    @Embedded
    private Endereco endereco;

    @Column(name = "ativo", nullable = false)
    protected boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Cliente() {
        // Construtor protegido exigido pelo JPA/Hibernate
    }

    protected Cliente(
            String nome,
            String documentoFiscalBruto,
            String email,
            String telefone,
            Endereco endereco) {
        validarDadosComunsCliente(nome, email, telefone, endereco);
        this.nome = nome.trim();
        this.documentoFiscal = extrairSomenteDigitos(documentoFiscalBruto);
        this.email = email.trim().toLowerCase();
        this.telefone = extrairSomenteDigitos(telefone);
        this.endereco = endereco;
        this.ativo = true;
        this.criadoEm = Instant.now();
    }

    public static String extrairSomenteDigitos(String textoBruto) {
        if (textoBruto == null) {
            return "";
        }
        return textoBruto.replaceAll("\\D", "");
    }

    protected void validarDadosComunsCliente(
            String nomeInformado,
            String emailInformado,
            String telefoneInformado,
            Endereco enderecoInformado) {
        if (nomeInformado == null || nomeInformado.isBlank()) {
            throw new RegraNegocioException("O nome do cliente é obrigatório.");
        }
        if (emailInformado == null || !emailInformado.contains("@")) {
            throw new RegraNegocioException("O e-mail informado para o cliente é inválido.");
        }
        String telefoneLimpo = extrairSomenteDigitos(telefoneInformado);
        if (telefoneLimpo.length() < 10 || telefoneLimpo.length() > 13) {
            throw new RegraNegocioException("O telefone do cliente deve conter entre 10 e 13 dígitos numéricos.");
        }
        if (enderecoInformado == null) {
            throw new RegraNegocioException("O endereço do cliente é obrigatório.");
        }
    }

    public void atualizarEndereco(Endereco novoEndereco) {
        if (novoEndereco == null) {
            throw new RegraNegocioException("O novo endereço não pode ser nulo.");
        }
        this.endereco = novoEndereco;
    }

    public void desativarCadastro() {
        this.ativo = false;
    }

    public void ativarCadastro() {
        this.ativo = true;
    }

    public abstract TipoCliente obterTipoCliente();

    public abstract boolean validarDocumentoFiscal();

    public abstract BigDecimal obterLimiteDiarioPixPadrao();

    public abstract boolean isIsentoTarifaPix();

    public abstract boolean podeSerTitularDe(TipoConta tipoConta);

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDocumentoFiscal() {
        return documentoFiscal;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
