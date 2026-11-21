package br.ufc.poo.corebank.domain.model.cliente;

import br.ufc.poo.corebank.domain.exception.DocumentoInvalidoException;
import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.conta.TipoConta;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("PESSOA_JURIDICA")
public class PessoaJuridica extends Cliente {

    public static final BigDecimal LIMITE_DIARIO_PIX_PADRAO_PJ = new BigDecimal("50000.00");
    private static final int TAMANHO_CNPJ = 14;
    private static final int[] PESOS_PRIMEIRO_DIGITO_CNPJ = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_SEGUNDO_DIGITO_CNPJ = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    @Column(name = "razao_social", length = 180)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 150)
    private String nomeFantasia;

    protected PessoaJuridica() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public PessoaJuridica(
            String razaoSocial,
            String nomeFantasia,
            String cnpjBruto,
            String emailCorporativo,
            String telefoneComercial,
            Endereco enderecoSede) {
        super(
                nomeFantasia != null && !nomeFantasia.isBlank() ? nomeFantasia : razaoSocial,
                cnpjBruto,
                emailCorporativo,
                telefoneComercial,
                enderecoSede
        );
        if (razaoSocial == null || razaoSocial.isBlank()) {
            throw new RegraNegocioException("A razão social da Pessoa Jurídica é obrigatória.");
        }
        if (!validarDocumentoFiscal()) {
            throw new DocumentoInvalidoException("O CNPJ informado é inválido: " + cnpjBruto);
        }
        this.razaoSocial = razaoSocial.trim();
        this.nomeFantasia = (nomeFantasia != null && !nomeFantasia.isBlank())
                ? nomeFantasia.trim()
                : this.razaoSocial;
    }

    public static boolean validarDigitosCnpj(String cnpjBruto) {
        String cnpjSomenteDigitos = extrairSomenteDigitos(cnpjBruto);
        if (cnpjSomenteDigitos.length() != TAMANHO_CNPJ) {
            return false;
        }
        if (possuiTodosDigitosIguais(cnpjSomenteDigitos)) {
            return false;
        }

        int primeiroDigitoCalculado = calcularDigitoVerificadorCnpj(cnpjSomenteDigitos, PESOS_PRIMEIRO_DIGITO_CNPJ);
        int segundoDigitoCalculado = calcularDigitoVerificadorCnpj(cnpjSomenteDigitos, PESOS_SEGUNDO_DIGITO_CNPJ);

        int primeiroDigitoInformado = Character.getNumericValue(cnpjSomenteDigitos.charAt(12));
        int segundoDigitoInformado = Character.getNumericValue(cnpjSomenteDigitos.charAt(13));

        return primeiroDigitoCalculado == primeiroDigitoInformado
                && segundoDigitoCalculado == segundoDigitoInformado;
    }

    private static int calcularDigitoVerificadorCnpj(String cnpjSomenteDigitos, int[] pesosMultiplicadores) {
        int somaPonderada = 0;
        for (int indice = 0; indice < pesosMultiplicadores.length; indice++) {
            int digitoAtual = Character.getNumericValue(cnpjSomenteDigitos.charAt(indice));
            somaPonderada += digitoAtual * pesosMultiplicadores[indice];
        }
        int restoDivisao = somaPonderada % 11;
        return (restoDivisao < 2) ? 0 : (11 - restoDivisao);
    }

    private static boolean possuiTodosDigitosIguais(String documento) {
        char primeiroCaractere = documento.charAt(0);
        for (int indice = 1; indice < documento.length(); indice++) {
            if (documento.charAt(indice) != primeiroCaractere) {
                return false;
            }
        }
        return true;
    }

    @Override
    public TipoCliente obterTipoCliente() {
        return TipoCliente.PESSOA_JURIDICA;
    }

    @Override
    public boolean validarDocumentoFiscal() {
        return validarDigitosCnpj(this.documentoFiscal);
    }

    @Override
    public BigDecimal obterLimiteDiarioPixPadrao() {
        return LIMITE_DIARIO_PIX_PADRAO_PJ;
    }

    @Override
    public boolean isIsentoTarifaPix() {
        return false;
    }

    @Override
    public boolean podeSerTitularDe(TipoConta tipoConta) {
        return tipoConta == TipoConta.CONTA_EMPRESARIAL;
    }

    public String getCnpj() {
        return documentoFiscal;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }
}
