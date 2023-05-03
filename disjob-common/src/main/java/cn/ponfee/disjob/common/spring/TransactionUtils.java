/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import java.util.function.Consumer;

/**
 * Spring transaction utility.
 *
 * @author Ponfee
 */
public class TransactionUtils {

    /**
     * 在事务提交后再执行
     *
     * @param action the action code
     */
    public static void doAfterTransactionCommit(final Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronization ts = new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            };
            TransactionSynchronizationManager.registerSynchronization(ts);
        } else {
            action.run();
        }
    }

    /**
     * 嵌套事务，执行在外层事务内。
     * <p>如果内层代码执行异常，则只回滚内层事务，外层事务不受影响。
     * <p>如果内层代码执行成功，外层代码执行异常，则内层事务与外层事务都会回滚。
     *
     * @param txManager the txManager
     * @param action    the action code
     * @param log       the exception log
     * @param <R>       return type
     * @return do action result
     */
    public static <R> R doInTransactionNested(PlatformTransactionManager txManager,
                                              ThrowingSupplier<R, Throwable> action,
                                              Consumer<Throwable> log) {
        Assert.isTrue(
            TransactionSynchronizationManager.isActualTransactionActive(),
            "Do nested transaction must be in parent transaction."
        );

        DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
        txDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        TransactionStatus status = txManager.getTransaction(txDefinition);
        try {
            R result = action.get();
            txManager.commit(status);
            return result;
        } catch (Throwable t) {
            txManager.rollback(status);
            log.accept(t);
            return null;
        }
    }

}
