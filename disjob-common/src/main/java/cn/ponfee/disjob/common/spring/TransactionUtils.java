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

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Spring transaction utility.
 *
 * @author Ponfee
 */
public class TransactionUtils {

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

    /**
     * 在事务提交后再执行
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
                    action.run();
                }
            };
            TransactionSynchronizationManager.registerSynchronization(ts);
        } else {
            action.run();
        }
    }

    /**
     * 创建一个新事务，如果当前存在事务，则将这个事务挂起。
     * <p>内部事务与外部事务相互独立，互不依赖。
     *
     * @param txManager    the txManager
     * @param action       the action code
     * @param errorHandler the error handler
     */
    public static void doInRequiresNewTransaction(PlatformTransactionManager txManager,
                                                  ThrowingRunnable<Throwable> action,
                                                  Consumer<Throwable> errorHandler) {
        doInPropagationTransaction(txManager, action, errorHandler, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 如果当前存在事务则开启一个嵌套事务，如果当前不存在事务则新建一个事务并运行。
     * <p>内部事务为外部事务的一个子事务。
     * <p>内部事务的提交/回滚不影响外部事务的提交/回滚
     * <p>内部事务的提交/回滚最终依赖外部事务的提交/回滚。
     *
     * @param transactionTemplate the transaction template
     * @param action              the action code
     * @param errorHandler        the error handler
     */
    public static void doInNestedTransaction(TransactionTemplate transactionTemplate,
                                             ThrowingRunnable<Throwable> action,
                                             Consumer<Throwable> errorHandler) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            PlatformTransactionManager txManager = transactionTemplate.getTransactionManager();
            doInPropagationTransaction(txManager, action, errorHandler, TransactionDefinition.PROPAGATION_NESTED);
        } else {
            throw new IllegalStateException("Do nested transaction must be in parent transaction.");
        }
    }

    // ----------------------------------------------------------------------private methods

    private static void doInPropagationTransaction(PlatformTransactionManager txManager,
                                                   ThrowingRunnable<Throwable> action,
                                                   Consumer<Throwable> errorHandler,
                                                   int transactionPropagation) {
        Objects.requireNonNull(txManager, "Transaction manager cannot be null.");
        DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
        txDefinition.setPropagationBehavior(transactionPropagation);
        TransactionStatus status = txManager.getTransaction(txDefinition);
        try {
            action.run();
            txManager.commit(status);
        } catch (Throwable t) {
            txManager.rollback(status);
            errorHandler.accept(t);
        }
    }

}
