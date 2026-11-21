package br.ufc.poo.corebank.domain.model.conta;

import br.ufc.poo.corebank.domain.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "agencias")
public class Agencia {

    public static final String CODIGO_AGENCIA_MATRIZ = "0001";
    public static final String NOME_AGENCIA_MATRIZ = "Agência Matriz Digital UFC";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @OneToMany(mappedBy = "agencia", fetch = FetchType.LAZY)
    private List<Conta> contas = new ArrayList<>();

    protected Agencia() {
        // Construtor exigido pelo JPA/Hibernate
    }

    public Agencia(String codigo, String nome) {
        if (codigo == null || !codigo.matches("\\d{4}")) {
            throw new RegraNegocioException("O código da agência deve conter exatamente 4 dígitos numéricos.");
        }
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome da agência é obrigatório.");
        }
        this.codigo = codigo;
        this.nome = nome.trim();
    }

    public void adicionarConta(Conta contaParaVincular) {
        if (contaParaVincular == null) {
            throw new RegraNegocioException("A conta a ser agregada na agência não pode ser nula.");
        }
        if (!this.contas.contains(contaParaVincular)) {
            this.contas.add(contaParaVincular);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public List<Conta> getContas() {
        return Collections.unmodifiableList(contas);
    }
}
