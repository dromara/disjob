/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import org.springframework.core.NamedThreadLocal;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

/**
 * Spring transaction utility.
 *
 * @author Ponfee
 */
public class TransactionUtils {

    private static final ThreadLocal<Boolean> DO_AFTER_COMMIT = new NamedThreadLocal<>("Transaction doAfterCommit");

    /**
     * Execute database dml affected rows
     */
    private static final int AFFECTED_ONE_ROW = 1;

    public static boolean isNotAffectedRow(int totalAffectedRow) {
        return totalAffectedRow < AFFECTED_ONE_ROW;
    }

    public static boolean isOneAffectedRow(int totalAffectedRow) {
        return totalAffectedRow == AFFECTED_ONE_ROW;
    }

    public static boolean hasAffectedRow(int totalAffectedRow) {
        return totalAffectedRow >= AFFECTED_ONE_ROW;
    }

    public static void assertNotAffectedRow(int totalAffectedRow, Supplier<String> errorMsgSupplier) {
        if (totalAffectedRow >= AFFECTED_ONE_ROW) {
            throw new IllegalStateException(errorMsgSupplier.get());
        }
    }

    public static void assertNotAffectedRow(int totalAffectedRow, String errorMsg) {
        if (totalAffectedRow >= AFFECTED_ONE_ROW) {
            throw new IllegalStateException(errorMsg);
        }
    }

    public static void assertOneAffectedRow(int totalAffectedRow, Supplier<String> errorMsgSupplier) {
        if (totalAffectedRow != AFFECTED_ONE_ROW) {
            throw new IllegalStateException(errorMsgSupplier.get());
        }
    }

    public static void assertOneAffectedRow(int totalAffectedRow, String errorMsg) {
        if (totalAffectedRow != AFFECTED_ONE_ROW) {
            throw new IllegalStateException(errorMsg);
        }
    }

    public static void assertHasAffectedRow(int totalAffectedRow, Supplier<String> errorMsgSupplier) {
        if (totalAffectedRow < AFFECTED_ONE_ROW) {
            throw new IllegalStateException(errorMsgSupplier.get());
        }
    }

    public static void assertHasAffectedRow(int totalAffectedRow, String errorMsg) {
        if (totalAffectedRow < AFFECTED_ONE_ROW) {
            throw new IllegalStateException(errorMsg);
        }
    }

    public static void doAfterTransactionCommit(Collection<Runnable> actions) {
        if (actions != null) {
            actions.forEach(TransactionUtils::doAfterTransactionCommit);
        }
    }

    /**
     * <pre>
     * 在事务提交成功后再执行`action`，注意：
     *   1）在单层事务中`doAfterTransactionCommit`添加了一些`action`，后面的代码因异常回滚了这个事务，因事务没有提交(已回滚)，这些`action`不会执行
     *   2）在嵌套事务中`doAfterTransactionCommit`添加了一些`action`，后面在嵌套事务中因异常回滚了嵌套事务，外层的事务提交成功后，这些`action`仍会执行
     *   3）在嵌套事务中`doAfterTransactionCommit`添加了一些`action`，后面在外层事务中因异常回滚了整个事务(嵌套和外层都会回滚)，这些`action`不会执行
     * </pre>
     *
     * @param action the action code
     */
    public static void doAfterTransactionCommit(final Runnable action) {
        if (action == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronization ts = new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    DO_AFTER_COMMIT.set(Boolean.TRUE);
                    try {
                        action.run();
                    } finally {
                        DO_AFTER_COMMIT.remove();
                    }
                }
            };
            TransactionSynchronizationManager.registerSynchronization(ts);
        } else {
            action.run();
        }
    }

    public static void assertDoInTransaction() {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "Must do in transaction.");
    }

    public static boolean isCurrentDoAfterCommit() {
        return Boolean.TRUE.equals(DO_AFTER_COMMIT.get());
    }

    public static boolean isNotDoInTransaction() {
        return !TransactionSynchronizationManager.isActualTransactionActive() || isCurrentDoAfterCommit();
    }

    /**
     * 创建一个新事务，如果当前存在事务，则将这个事务挂起。
     * <p>内部事务与外部事务相互独立，互不依赖，互不影响。
     *
     * @param txManager    the txManager
     * @param action       the action
     * @param errorHandler the error handler
     * @return run action result, return null if transaction commit failed
     */
    public static <R> R doInRequiresNewTransaction(PlatformTransactionManager txManager,
                                                   ThrowingSupplier<R, Throwable> action,
                                                   Consumer<Throwable> errorHandler) {
        return doInPropagationTransaction(txManager, action, errorHandler, PROPAGATION_REQUIRES_NEW);
    }

    /**
     * <pre>
     * 如果当前存在事务则开启一个嵌套事务，如果当前不存在事务则新建一个事务并运行。
     *   1）内部事务为外部事务的一个子事务。
     *   2）内部事务的提交/回滚不影响外部事务的提交/回滚
     *   3）内部事务的提交/回滚最终依赖外部事务的提交/回滚。
     * </pre>
     *
     * @param transactionTemplate the transaction template
     * @param action              the action
     * @param errorHandler        the error handler, execute after transaction rollback
     * @return run action result, return null if transaction commit failed
     */
    public static <R> R doInNestedTransaction(TransactionTemplate transactionTemplate,
                                              ThrowingSupplier<R, Throwable> action,
                                              Consumer<Throwable> errorHandler) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            PlatformTransactionManager txManager = transactionTemplate.getTransactionManager();
            return doInPropagationTransaction(txManager, action, errorHandler, PROPAGATION_NESTED);
        } else {
            throw new IllegalStateException("Do nested transaction must be in parent transaction.");
        }
    }

    // ----------------------------------------------------------------------private methods

    private static <R> R doInPropagationTransaction(PlatformTransactionManager txManager,
                                                    ThrowingSupplier<R, Throwable> action,
                                                    Consumer<Throwable> errorHandler,
                                                    int transactionPropagation) {
        Objects.requireNonNull(txManager, "Transaction manager cannot be null.");
        DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
        txDefinition.setPropagationBehavior(transactionPropagation);
        TransactionStatus status = txManager.getTransaction(txDefinition);
        try {
            R result = action.get();
            txManager.commit(status);
            return result;
        } catch (Throwable t) {
            txManager.rollback(status);
            errorHandler.accept(t);
            return null;
        }
    }

}
