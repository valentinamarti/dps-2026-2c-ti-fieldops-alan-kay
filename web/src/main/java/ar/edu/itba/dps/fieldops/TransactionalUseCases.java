package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.business.exceptions.ConcurrentUpdateException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TransactionalUseCases {
    private final TransactionTemplate writing;
    private final TransactionTemplate reading;

    public TransactionalUseCases(PlatformTransactionManager transactions) {
        this.writing = new TransactionTemplate(transactions);
        this.reading = new TransactionTemplate(transactions);
        this.reading.setReadOnly(true);
    }

    public <P> P writing(Class<P> port, P useCase) {
        return wrapped(port, useCase, writing);
    }

    public <P> P reading(Class<P> port, P useCase) {
        return wrapped(port, useCase, reading);
    }

    private <P> P wrapped(Class<P> port, P useCase, TransactionTemplate template) {
        return port.cast(Proxy.newProxyInstance(useCase.getClass().getClassLoader(), new Class<?>[]{port},
                (_, method, arguments) -> inTransaction(template, useCase, method, arguments)));
    }

    private Object inTransaction(TransactionTemplate template, Object useCase, Method method, Object[] arguments) {
        try {
            return template.execute(_ -> invoke(useCase, method, arguments));
        } catch (OptimisticLockingFailureException conflict) {
            throw new ConcurrentUpdateException(conflict);
        }
    }

    private Object invoke(Object useCase, Method method, Object[] arguments) {
        try {
            return method.invoke(useCase, arguments);
        } catch (InvocationTargetException failure) {
            throw failure.getCause() instanceof RuntimeException runtime ? runtime : new IllegalStateException(failure.getCause());
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
