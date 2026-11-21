package br.ufc.poo.corebank.domain.model.cliente;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Endereco {

    @Column(name = "endereco_logradouro", nullable = false, length = 150)
    private String logradouro;

    @Column(name = "endereco_numero", nullable = false, length = 20)
    private String numero;

    @Column(name = "endereco_complemento", length = 100)
    private String complemento;

    @Column(name = "endereco_bairro", nullable = false, length = 100)
    private String bairro;

    @Column(name = "endereco_cidade", nullable = false, length = 100)
    private String cidade;

    @Column(name = "endereco_uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "endereco_cep", nullable = false, length = 8)
    private String cep;

    protected Endereco() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public Endereco(
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String uf,
            String cep) {
        validarCamposObrigatorios(logradouro, numero, bairro, cidade, uf, cep);
        this.logradouro = logradouro.trim();
        this.numero = numero.trim();
        this.complemento = complemento != null ? complemento.trim() : null;
        this.bairro = bairro.trim();
        this.cidade = cidade.trim();
        this.uf = uf.trim().toUpperCase();
        this.cep = extrairDigitosCep(cep);
    }

    private void validarCamposObrigatorios(
            String logradouroInformado,
            String numeroInformado,
            String bairroInformado,
            String cidadeInformada,
            String ufInformada,
            String cepInformado) {
        if (logradouroInformado == null || logradouroInformado.isBlank()) {
            throw new RegraNegocioException("O logradouro do endereço é obrigatório.");
        }
        if (numeroInformado == null || numeroInformado.isBlank()) {
            throw new RegraNegocioException("O número do endereço é obrigatório.");
        }
        if (bairroInformado == null || bairroInformado.isBlank()) {
            throw new RegraNegocioException("O bairro do endereço é obrigatório.");
        }
        if (cidadeInformada == null || cidadeInformada.isBlank()) {
            throw new RegraNegocioException("A cidade do endereço é obrigatória.");
        }
        if (ufInformada == null || ufInformada.trim().length() != 2) {
            throw new RegraNegocioException("A UF do endereço deve conter exatamente 2 letras.");
        }
        String cepLimpo = extrairDigitosCep(cepInformado);
        if (cepLimpo.length() != 8) {
            throw new RegraNegocioException("O CEP do endereço deve conter exatamente 8 dígitos numéricos.");
        }
    }

    private String extrairDigitosCep(String cepBruto) {
        if (cepBruto == null) {
            return "";
        }
        return cepBruto.replaceAll("\\D", "");
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public String getCep() {
        return cep;
    }
}
