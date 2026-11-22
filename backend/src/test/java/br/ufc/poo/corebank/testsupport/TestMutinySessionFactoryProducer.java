package br.ufc.poo.corebank.testsupport;

import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hibernate.reactive.common.spi.Implementor;
import org.hibernate.reactive.mutiny.Mutiny;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Function;

@ApplicationScoped
public class TestMutinySessionFactoryProducer {

    private static final String UUID_SESSAO_TESTE = UUID.randomUUID().toString();

    @Produces
    @Mock
    @ApplicationScoped
    public Mutiny.SessionFactory produzirSessionFactoryReativaEmMemoria() {
        Mutiny.Transaction transacaoEmMemoria = criarProxyTransacaoEmMemoria();
        Mutiny.Session sessaoEmMemoria = criarProxySessaoEmMemoria(transacaoEmMemoria);

        InvocationHandler manipuladorSessionFactory = (proxy, metodo, argumentos) -> {
            String nomeMetodo = metodo.getName();
            return switch (nomeMetodo) {
                case "createSession" -> sessaoEmMemoria;
                case "openSession" -> Uni.createFrom().item(sessaoEmMemoria);
                case "withSession" -> {
                    @SuppressWarnings("unchecked")
                    Function<Mutiny.Session, Uni<?>> funcaoSessao = (Function<Mutiny.Session, Uni<?>>) argumentos[0];
                    yield funcaoSessao.apply(sessaoEmMemoria);
                }
                case "withTransaction" -> {
                    @SuppressWarnings("unchecked")
                    Function<Mutiny.Session, Uni<?>> funcaoTransacao = (Function<Mutiny.Session, Uni<?>>) argumentos[0];
                    yield funcaoTransacao.apply(sessaoEmMemoria);
                }
                case "getUuid" -> UUID_SESSAO_TESTE;
                case "isOpen" -> true;
                case "close" -> null;
                case "toString" -> "TestMutinySessionFactoryEmMemoria";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == argumentos[0];
                default -> null;
            };
        };

        return (Mutiny.SessionFactory) Proxy.newProxyInstance(
                Mutiny.SessionFactory.class.getClassLoader(),
                new Class<?>[]{Mutiny.SessionFactory.class, Implementor.class},
                manipuladorSessionFactory
        );
    }

    private Mutiny.Session criarProxySessaoEmMemoria(Mutiny.Transaction transacaoEmMemoria) {
        InvocationHandler manipuladorSessao = (proxy, metodo, argumentos) -> {
            String nomeMetodo = metodo.getName();
            return switch (nomeMetodo) {
                case "isOpen" -> true;
                case "close" -> Uni.createFrom().voidItem();
                case "flush" -> Uni.createFrom().voidItem();
                case "withTransaction" -> {
                    @SuppressWarnings("unchecked")
                    Function<Mutiny.Transaction, Uni<?>> funcaoTransacao =
                            (Function<Mutiny.Transaction, Uni<?>>) argumentos[0];
                    yield funcaoTransacao.apply(transacaoEmMemoria);
                }
                case "currentTransaction" -> transacaoEmMemoria;
                case "toString" -> "TestMutinySessionEmMemoria";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == argumentos[0];
                default -> Uni.createFrom().nullItem();
            };
        };

        return (Mutiny.Session) Proxy.newProxyInstance(
                Mutiny.Session.class.getClassLoader(),
                new Class<?>[]{Mutiny.Session.class},
                manipuladorSessao
        );
    }

    private Mutiny.Transaction criarProxyTransacaoEmMemoria() {
        InvocationHandler manipuladorTransacao = (proxy, metodo, argumentos) -> {
            String nomeMetodo = metodo.getName();
            return switch (nomeMetodo) {
                case "isMarkedForRollback" -> false;
                case "toString" -> "TestMutinyTransactionEmMemoria";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == argumentos[0];
                default -> null;
            };
        };

        return (Mutiny.Transaction) Proxy.newProxyInstance(
                Mutiny.Transaction.class.getClassLoader(),
                new Class<?>[]{Mutiny.Transaction.class},
                manipuladorTransacao
        );
    }
}
