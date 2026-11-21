package br.ufc.poo.corebank.domain.model.cliente;

import br.ufc.poo.corebank.domain.exception.DocumentoInvalidoException;
import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import br.ufc.poo.corebank.domain.model.conta.TipoConta;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Entity
@DiscriminatorValue("PESSOA_FISICA")
public class PessoaFisica extends Cliente {

    public static final BigDecimal LIMITE_DIARIO_PIX_PADRAO_PF = new BigDecimal("5000.00");
    public static final int IDADE_MINIMA_TITULAR_CONTA = 18;
    private static final int TAMANHO_CPF = 11;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    protected PessoaFisica() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public PessoaFisica(
            String nomeCompleto,
            String cpfBruto,
            LocalDate dataNascimento,
            String email,
            String telefone,
            Endereco endereco) {
        super(nomeCompleto, cpfBruto, email, telefone, endereco);
        if (!validarDocumentoFiscal()) {
            throw new DocumentoInvalidoException("O CPF informado é inválido: " + cpfBruto);
        }
        validarMaioridadeCivil(dataNascimento);
        this.dataNascimento = dataNascimento;
    }

    public static boolean validarDigitosCpf(String cpfBruto) {
        String cpfSomenteDigitos = extrairSomenteDigitos(cpfBruto);
        if (cpfSomenteDigitos.length() != TAMANHO_CPF) {
            return false;
        }
        if (possuiTodosDigitosIguais(cpfSomenteDigitos)) {
            return false;
        }

        int primeiroDigitoCalculado = calcularDigitoVerificadorCpf(cpfSomenteDigitos, 9, 10);
        int segundoDigitoCalculado = calcularDigitoVerificadorCpf(cpfSomenteDigitos, 10, 11);

        int primeiroDigitoInformado = Character.getNumericValue(cpfSomenteDigitos.charAt(9));
        int segundoDigitoInformado = Character.getNumericValue(cpfSomenteDigitos.charAt(10));

        return primeiroDigitoCalculado == primeiroDigitoInformado
                && segundoDigitoCalculado == segundoDigitoInformado;
    }

    private static int calcularDigitoVerificadorCpf(String cpfSomenteDigitos, int quantidadeDigitosBase, int pesoInicial) {
        int somaPonderada = 0;
        for (int indice = 0; indice < quantidadeDigitosBase; indice++) {
            int digitoAtual = Character.getNumericValue(cpfSomenteDigitos.charAt(indice));
            int pesoAtual = pesoInicial - indice;
            somaPonderada += digitoAtual * pesoAtual;
        }
        int restoDivisao = (somaPonderada * 10) % 11;
        return (restoDivisao == 10 || restoDivisao == 11) ? 0 : restoDivisao;
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

    private void validarMaioridadeCivil(LocalDate dataNascimentoInformada) {
        if (dataNascimentoInformada == null) {
            throw new RegraNegocioException("A data de nascimento da Pessoa Física é obrigatória.");
        }
        int idadeCalculada = Period.between(dataNascimentoInformada, LocalDate.now()).getYears();
        if (idadeCalculada < IDADE_MINIMA_TITULAR_CONTA) {
            throw new RegraNegocioException("O titular Pessoa Física deve ter no mínimo 18 anos para cadastro bancário.");
        }
    }

    @Override
    public TipoCliente obterTipoCliente() {
        return TipoCliente.PESSOA_FISICA;
    }

    @Override
    public boolean validarDocumentoFiscal() {
        return validarDigitosCpf(this.documentoFiscal);
    }

    @Override
    public BigDecimal obterLimiteDiarioPixPadrao() {
        return LIMITE_DIARIO_PIX_PADRAO_PF;
    }

    @Override
    public boolean isIsentoTarifaPix() {
        return true;
    }

    @Override
    public boolean podeSerTitularDe(TipoConta tipoConta) {
        return tipoConta == TipoConta.CONTA_CORRENTE;
    }

    public String getCpf() {
        return documentoFiscal;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }
}
