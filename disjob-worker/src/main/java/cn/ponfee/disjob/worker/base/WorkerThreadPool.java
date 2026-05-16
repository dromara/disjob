/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.collect.SynchronizedSegmentMap;
import cn.ponfee.disjob.common.concurrent.*;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.supervisor.SupervisorRpcService;
import cn.ponfee.disjob.core.supervisor.dto.StartTaskResult;
import cn.ponfee.disjob.core.supervisor.dto.StopTaskParam;
import cn.ponfee.disjob.core.worker.WorkerMetrics;
import cn.ponfee.disjob.worker.exception.OperationTaskException;
import cn.ponfee.disjob.worker.exception.SavepointFailedException;
import cn.ponfee.disjob.worker.executor.ExecutionResult;
import cn.ponfee.disjob.worker.executor.ExecutionTask;
import cn.ponfee.disjob.worker.executor.Savepoint;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors.commonScheduledPool;
import static cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors.commonThreadPool;

/**
 * Thread pool of execute task, also is a boss thread
 *
 * @author Ponfee
 */
@Slf4j
public class WorkerThreadPool extends Thread implements Closeable {

    private static final int ERR_MSG_MAX_LENGTH = 2048;
    private static final AtomicLong NAMED_SEQ = new AtomicLong(1);

    /**
     * Supervisor rpc client
     */
    private final SupervisorRpcService supervisorRpcClient;

    /**
     * Maximum pool size
     */
    private volatile int maximumPoolSize;

    /**
     * Worker thread keep alive time
     */
    private final long keepAliveTime;

    /**
     * Active worker thread pool
     */
    private final ActiveThreadPool activePool = new ActiveThreadPool();

    /**
     * Idle worker thread pool
     */
    private final LinkedBlockingDeque<WorkerThread> idlePool = new LinkedBlockingDeque<>();

    /**
     * Task queue for pending execution
     */
    private final LinkedBlockingDeque<WorkerTask> taskQueue = new LinkedBlockingDeque<>();

    /**
     * Counts worker thread number
     */
    private final AtomicInteger workerThreadCounter = new AtomicInteger(0);

    /**
     * Counts completed task number
     */
    private final AtomicLong completedTaskCounter = new AtomicLong(0);

    /**
     * Thread pool state
     */
    private final TripleState threadPoolState = TripleState.createStarted();

    public WorkerThreadPool(int maximumPoolSize, long keepAliveTimeSeconds, SupervisorRpcService supervisorRpcClient) {
        Assert.isTrue(keepAliveTimeSeconds > 0, "Keep alive time seconds must be positive number.");
        setMaximumPoolSize(maximumPoolSize);
        this.keepAliveTime = TimeUnit.SECONDS.toNanos(keepAliveTimeSeconds);
        this.supervisorRpcClient = Objects.requireNonNull(supervisorRpcClient);
        SingletonClassConstraint.constrain(this);

        super.setDaemon(true);
        super.setName(getClass().getSimpleName());
        super.setPriority(Thread.MAX_PRIORITY);
        super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(log));

        WorkerConfigurator.setWorkerThreadPool(this);
    }

    /**
     * Submit the execution task to thread pool
     *
     * @param task the execution task
     * @return {@code true} if thread pool accepted
     */
    boolean submit(WorkerTask task) {
        if (threadPoolState.isStopped()) {
            return false;
        }
        log.info("Task trace [{}] submitted: {}, {}", task.getTaskId(), task.getOperation(), task.getWorker());
        if (task.getOperation().isTrigger()) {
            return taskQueue.offerLast(task);
        } else {
            commonThreadPool().execute(ThrowingRunnable.toCaught(() -> stopTask(task)));
            return true;
        }
    }

    @PreDestroy
    @Override
    public void close() {
        if (!threadPoolState.stop()) {
            return;
        }

        log.info("Close worker thread pool start...");
        // stop this boss thread
        if (Thread.currentThread() != this) {
            ThrowingRunnable.doCaught(super::interrupt);
            ThrowingRunnable.doCaught(() -> Threads.stopThread(this, 6000L));
        }
        // stop idle pool thread
        List<WorkerThread> idleWorkerThreads = Collects.drainAll(idlePool);
        idleWorkerThreads.forEach(e -> ThrowingRunnable.doCaught(e::interrupt));
        idleWorkerThreads.forEach(e -> ThrowingRunnable.doCaught(e::doStop));
        // clear task queue
        ThrowingRunnable.doCaught(taskQueue::clear);
        // stop active pool thread
        ThrowingRunnable.doCaught(activePool::close);
        log.info("Close worker thread pool end.");
    }

    @Override
    public void run() {
        try {
            while (threadPoolState.isRunning() && !super.isInterrupted()) {
                WorkerTask task = taskQueue.takeFirst();
                WorkerThread workerThread = takeWorkerThread();
                if (workerThread.isStopped()) {
                    log.info("Worker thread was stopped: {}, {}", workerThread.getName(), task.getTaskId());
                    // Re-submit this execution task
                    taskQueue.putFirst(task);
                    workerThread.doStop();
                    continue;
                }
                try {
                    activePool.submit(workerThread, task);
                } catch (BrokenThreadException e) {
                    log.error(e.getMessage());
                    // Re-submit this execution task
                    taskQueue.putFirst(task);
                    workerThread.doStop();
                } catch (IllegalTaskException e) {
                    log.error(e.getMessage());
                    // return the worker thread to idle pool
                    idlePool.putFirst(workerThread);
                } catch (Throwable t) {
                    workerThread.doStop();
                    throw t;
                }
            }
        } catch (Throwable t) {
            log.error("Boss thread break: {}({})", t.getClass().getName(), t.getMessage());
            Threads.reinterruptIfInterruptedException(t);
        } finally {
            close();
        }
    }

    @Override
    public String toString() {
        String format = "maximum-pool-size=%d, current-pool-size=%d, active-pool-size=%d, idle-pool-size=%d, queue-task-count=%d, completed-task-count=%d";
        return String.format(format, maximumPoolSize, workerThreadCounter.get(), activePool.size(), idlePool.size(), taskQueue.size(), completedTaskCounter.get());
    }

    WorkerMetrics.WorkerThreadPoolMetrics metrics() {
        WorkerMetrics.WorkerThreadPoolMetrics metrics = new WorkerMetrics.WorkerThreadPoolMetrics();
        metrics.setClosed(threadPoolState.isStopped());
        metrics.setKeepAliveTime(TimeUnit.NANOSECONDS.toSeconds(keepAliveTime));
        metrics.setMaximumPoolSize(maximumPoolSize);
        metrics.setCurrentPoolSize(workerThreadCounter.get());
        metrics.setActivePoolSize(activePool.size());
        metrics.setIdlePoolSize(idlePool.size());
        metrics.setQueueTaskCount(taskQueue.size());
        metrics.setCompletedTaskCount(completedTaskCounter.get());
        return metrics;
    }

    synchronized void setMaximumPoolSize(int value) {
        Assert.isTrue(0 < value && value <= ThreadPoolExecutors.MAX_CAP, "Maximum pool size must be range [1, 32767].");
        this.maximumPoolSize = value;
    }

    boolean existsTask(long taskId) {
        if (activePool.containsKey(taskId)) {
            return true;
        }
        for (WorkerTask task : taskQueue) {
            if (task.getTaskId() == taskId) {
                return true;
            }
        }
        // check again from active pool
        return activePool.containsKey(taskId);
    }

    // ----------------------------------------------------------------------private methods

    /**
     * Stop(Pause or Cancel) specified task
     *
     * @param task the stops task
     */
    private void stopTask(WorkerTask task) {
        Operation ops = task.getOperation();
        long taskId = task.getTaskId();
        Assert.isTrue(ops != null && ops.isNotTrigger(), () -> "Invalid stop operation: " + ops);

        if (threadPoolState.isStopped()) {
            log.warn("Worker thread pool closed, discard task: {}, {}", taskId, ops);
            return;
        }

        Pair<WorkerThread, WorkerTask> pair = activePool.takeThread(taskId, null, ops);
        if (pair == null) {
            log.warn("Not found executing task: {}, {}", taskId, ops);
            // 某些异常场景导致任务状态更新失败停留在EXECUTING且任务已从线程池中移除，在此处修复状态：EXECUTING -> (PAUSED|CANCELED)
            stopTask(task, ops, ops.name() + " aborted EXECUTING state task");
        } else {
            stopTask(pair.getLeft(), pair.getRight(), ops);
        }
    }

    private void stopTask(WorkerThread workerThread, WorkerTask task, Operation ops) {
        log.info("Stop task: {}, {}, {}", task.getTaskId(), ops, workerThread.getName());
        try {
            workerThread.doStop();
        } finally {
            stopTask(task, ops, null);
        }
    }

    private WorkerThread takeWorkerThread() throws InterruptedException {
        WorkerThread workerThread = idlePool.pollFirst();
        if (workerThread != null) {
            return workerThread;
        }
        while (!threadPoolState.isStopped() && !super.isInterrupted()) {
            if ((workerThread = createWorkerThreadIfNecessary()) != null) {
                return workerThread;
            }
            if ((workerThread = idlePool.pollFirst(1000L, TimeUnit.MILLISECONDS)) != null) {
                return workerThread;
            }
        }
        throw new IllegalStateException("Take worker thread stopped.");
    }

    /**
     * if current thread count less than maximumPoolSize, then create new thread to pool.
     *
     * @return created worker thread object
     */
    private WorkerThread createWorkerThreadIfNecessary() {
        for (int count; (count = workerThreadCounter.get()) < maximumPoolSize; ) {
            if (workerThreadCounter.compareAndSet(count, count + 1)) {
                WorkerThread thread = new WorkerThread();
                log.info("Created worker thread {}, current size: {}", thread, count + 1);
                return thread;
            }
        }
        return null;
    }

    private void executeTask(WorkerTask task) {
        int phase = 0;
        ScheduledFuture<?> cancelTimeoutFuture = null;
        try {
            StartTaskResult startTaskResult = CoreUtils.doInSynchronized(
                task.getLockInstanceId(), () -> supervisorRpcClient.startTask(task.toStartTaskParam()));
            if (!startTaskResult.isSuccess()) {
                log.warn("Start task failed: {}, {}", task, startTaskResult.getFailedMessage());
                return;
            }
            phase = 1;
            log.info("Start task success: {}", task.getTaskId());

            ExecutionTask executionTask = task.init(startTaskResult);
            phase = 2;
            log.info("Initialize executor success: {}", task.getTaskId());

            cancelTimeoutFuture = registerCancelTimeoutTask(task);
            ExecutionResult result = task.execute(executionTask, new TaskSavepoint(task));
            if (result != null && result.isSuccess()) {
                Operation ops = task.getOperation();
                log.info("Execute task success: {}, {}, {}", task.getTaskId(), ops, result.getMsg());
                if (ops != Operation.TRIGGER) {
                    // 如果task被非TRIGGER操作，但又正常执行成功，则要更新回TRIGGER
                    boolean updated = task.updateOperation(ops, Operation.TRIGGER);
                    log.info("Execute task revert to trigger: {}, {}, {}", task.getTaskId(), ops, updated);
                }
                stopTask(task, Operation.TRIGGER, ExecuteState.COMPLETED, null);
            } else {
                log.error("Execute task failed: {}, {}", task, result);
                stopTask(task, Operation.TRIGGER, ExecuteState.EXECUTE_FAILED, Objects.toString(result, "null"));
            }
        } catch (OperationTaskException e) {
            log.error("Operation task exception: {}, {}, {}", e.operation(), task, e.getMessage());
            stopInstance(task, e.operation(), Throwables.getRootCauseStackTrace(e, ERR_MSG_MAX_LENGTH));
        } catch (Throwable t) {
            if (phase == 0) {
                log.warn("Start task error: {}", task, t);
                if (task.getRouteStrategy().isNotBroadcast()) {
                    // reset sched_task.worker to null value
                    List<Long> list = Collections.singletonList(task.getTaskId());
                    ThrowingRunnable.doCaught(() -> supervisorRpcClient.updateTaskWorker(list, null));
                }
            } else {
                if (Throwables.isThreadDeath(t)) {
                    // 调用`Thread#stop()`时可能会抛出该异常
                    log.warn("Execute thread death: {}, {}", task, t.getMessage());
                } else if (t instanceof InterruptedException) {
                    log.warn("Execute thread interrupted: {}, {}", task, t.getMessage());
                } else {
                    log.error("Execute task error: {}", task, t);
                }
                ExecuteState toState = (phase == 1) ? ExecuteState.INITIALIZE_EXCEPTION : ExecuteState.EXECUTE_EXCEPTION;
                stopTask(task, Operation.TRIGGER, toState, Throwables.getRootCauseStackTrace(t, ERR_MSG_MAX_LENGTH));
            }
            Throwables.rethrowIfFatal(t);
        } finally {
            task.destroy();
            if (cancelTimeoutFuture != null && !cancelTimeoutFuture.isDone()) {
                cancelTimeoutFuture.cancel(false);
            }
        }
    }

    private ScheduledFuture<?> registerCancelTimeoutTask(WorkerTask task) {
        if (task.getExecuteTimeout() <= 0) {
            return null;
        }
        final long expectTime = System.currentTimeMillis() + task.getExecuteTimeout();
        return commonScheduledPool().schedule(() -> {
            long delayedTime = System.currentTimeMillis() - expectTime;
            Pair<WorkerThread, WorkerTask> pair = activePool.takeThread(task.getTaskId(), task, Operation.TIMEOUT_CANCEL);
            if (pair != null) {
                log.error("Stop timeout task: {}, {}", task.getTaskId(), delayedTime);
                stopTask(pair.getLeft(), pair.getRight(), Operation.TIMEOUT_CANCEL);
            } else {
                log.info("Skip timeout task: {}, {}", task.getTaskId(), delayedTime);
            }
        }, task.getExecuteTimeout(), TimeUnit.MILLISECONDS);
    }

    private void stopTask(WorkerTask task, Operation ops, String errorMsg) {
        stopTask(task, ops, ops.toState(), errorMsg);
    }

    private void stopTask(WorkerTask task, Operation ops, ExecuteState toState, String errorMsg) {
        Assert.notNull(ops, "Stop task operation cannot be null.");
        if (!task.updateOperation(ops, null)) {
            log.info("Stop task failed: {}, {}, {}", task.getTaskId(), ops, toState);
            return;
        }
        completedTaskCounter.incrementAndGet();
        StopTaskParam param = task.toStopTaskParam(ops, toState, errorMsg);
        log.info("Stop task operation: {}, {}, {}", task.getTaskId(), ops, toState);
        Supplier<String> msgSupplier = () -> "Stop task error: " + task.getTaskId() + ", " + ops + ", " + toState;
        CoreUtils.doInSynchronized(task.getLockInstanceId(), () -> supervisorRpcClient.stopTask(param), msgSupplier);
    }

    private void stopInstance(WorkerTask task, Operation ops, String errorMsg) {
        if (!task.updateOperation(Operation.TRIGGER, ops)) {
            log.info("Stop instance conflict: {}, {}", task, ops);
            return;
        }
        stopTask(task, ops, errorMsg);
        Long lockInstanceId = task.getLockInstanceId();
        log.info("Stop instance trace: {}, {}, {}", lockInstanceId, task.getTaskId(), ops);
        Supplier<String> msgSupplier = () -> "Stop instance error: " + lockInstanceId + ", " + task.getTaskId() + ", " + ops;
        CoreUtils.doInSynchronized(lockInstanceId, () -> stopInstance(lockInstanceId, task.getTaskId(), ops), msgSupplier);
    }

    private void stopInstance(long lockInstanceId, long taskId, Operation ops) {
        boolean res = true;
        if (ops == Operation.PAUSE) {
            res = supervisorRpcClient.pauseInstance(lockInstanceId);
        } else if (ops == Operation.EXCEPTION_CANCEL) {
            res = supervisorRpcClient.cancelInstance(lockInstanceId, ops);
        } else {
            log.error("Stop instance unknown: {}, {}, {}", lockInstanceId, taskId, ops);
        }
        if (!res) {
            log.info("Stop instance conflict: {}, {}, {}", lockInstanceId, taskId, ops);
        }
    }

    /**
     * Active thread pool
     */
    private static class ActiveThreadPool extends SynchronizedSegmentMap<Long, WorkerThread> {

        private void submit(WorkerThread wt, WorkerTask task) throws Throwable {
            if (task.getOperation().isNotTrigger()) {
                throw new IllegalTaskException("Not a executable task operation: " + task);
            }
            execute(task.getTaskId(), map -> {
                WorkerThread et = map.get(task.getTaskId());
                if (et != null) {
                    // discard re-dispatched task
                    throw new IllegalTaskException("Repeat execute task: " + task + ", " + et.getCurrentTask());
                }
                try {
                    wt.setCurrentTask(task);
                    wt.submit(task);
                    map.put(task.getTaskId(), wt);
                    log.info("Put to active pool worker thread: {}, {}", task.getTaskId(), wt.getName());
                } catch (Throwable t) {
                    wt.setCurrentTask(null);
                    throw t;
                }
            });
        }

        private Pair<WorkerThread, WorkerTask> takeThread(long taskId, WorkerTask held, Operation ops) {
            return process(taskId, map -> {
                WorkerThread wt = map.get(taskId);
                WorkerTask task;
                if (wt == null || (task = wt.getCurrentTask()) == null || (held != null && task != held)) {
                    return null;
                }
                if (!task.updateOperation(Operation.TRIGGER, ops)) {
                    return null;
                }
                wt.setCurrentTask(null);
                map.remove(taskId);
                log.info("Taken from active pool worker thread: {}, {}", task.getTaskId(), wt.getName());
                return Pair.of(wt, task);
            });
        }

        private WorkerTask removeThread(WorkerThread wt) {
            WorkerTask task = wt.getCurrentTask();
            if (task == null) {
                return null;
            }
            return process(task.getTaskId(), map -> {
                if (map.get(task.getTaskId()) != wt) {
                    return null;
                }
                wt.setCurrentTask(null);
                map.remove(task.getTaskId());
                log.info("Removed from active pool worker thread: {}, {}", task.getTaskId(), wt.getName());
                return task;
            });
        }

        private void close() {
            log.info("Close active thread pool start: {}", super.size());
            ExecutorService cachedExecutor = Executors.newCachedThreadPool();
            MultithreadExecutors.run(super.values(), WorkerThread::close, cachedExecutor);
            cachedExecutor.shutdown();
            log.info("Close active thread pool end: {}", super.size());
        }
    }

    private static class IllegalTaskException extends RuntimeException {
        private static final long serialVersionUID = -1273937229826200274L;

        private IllegalTaskException(String message) {
            super(message);
        }
    }

    private static class BrokenThreadException extends RuntimeException {
        private static final long serialVersionUID = 3475868254991118684L;

        private BrokenThreadException(String message) {
            super(message);
        }
    }

    private class TaskSavepoint implements Savepoint {
        private final long taskId;
        private final String worker;

        private TaskSavepoint(WorkerTask workerTask) {
            this.taskId = workerTask.getTaskId();
            this.worker = workerTask.getWorker().serialize();
        }

        @Override
        public void save(String executeSnapshot) throws Exception {
            if (executeSnapshot != null && executeSnapshot.length() > JobConstants.CLOB_MAXIMUM_LENGTH) {
                throw new SavepointFailedException("Execution snapshot length too large: " + executeSnapshot.length());
            }
            if (!supervisorRpcClient.savepoint(taskId, worker, executeSnapshot)) {
                throw new SavepointFailedException("Save execution snapshot data occur error.");
            }
        }
    }

    private class WorkerThread extends Thread {
        /**
         * Worker task queue
         */
        private final BlockingQueue<WorkerTask> queue = new SynchronousQueue<>();

        /**
         * Worker thread state
         */
        private final TripleState state = TripleState.createStarted();

        /**
         * Current worker task
         */
        private volatile WorkerTask currentTask;

        private WorkerThread() {
            super.setDaemon(true);
            super.setName(getClass().getSimpleName() + "-" + NAMED_SEQ.getAndIncrement());
            super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(log));
            super.start();
        }

        private void submit(WorkerTask task) throws InterruptedException {
            if (isStopped()) {
                throw new BrokenThreadException("Worker thread already stopped: " + super.getName());
            }
            if (!queue.offer(task, 5000L, TimeUnit.MILLISECONDS)) {
                throw new BrokenThreadException("Put to worker thread queue timeout: " + super.getName());
            }
        }

        private void toStop() {
            if (state.stop()) {
                workerThreadCounter.decrementAndGet();
                final WorkerTask task = currentTask;
                if (task != null) {
                    ThrowingRunnable.doCaught(task::stop);
                }
            }
        }

        private void doStop() {
            toStop();
            Threads.stopThread(this, 5000L);
        }

        /**
         * Task execute finished, then return the worker thread to idle pool.
         * <p>Called this method current thread is WorkerThread
         *
         * @return {@code true} if return to idle pool successfully
         */
        private boolean returnToPool() {
            if (activePool.removeThread(this) == null || isStopped()) {
                // maybe already removed by other operation
                return false;
            }
            try {
                // return the detached worker thread to idle pool
                idlePool.putFirst(this);
                return true;
            } catch (InterruptedException e) {
                log.error("Return thread to idle pool interrupted.", e);
                Thread.currentThread().interrupt();
                return false;
            }
        }

        /**
         * Remove the worker thread from active pool and destroy it.
         */
        private void removeFromPool() {
            toStop();
            if (activePool.removeThread(this) == null && !idlePool.remove(this)) {
                log.info("Worker thread not in thread pool: {}", super.getName());
            }
        }

        private void setCurrentTask(WorkerTask task) {
            this.currentTask = task;
        }

        private WorkerTask getCurrentTask() {
            return currentTask;
        }

        private boolean isStopped() {
            return state.isStopped() || super.isInterrupted() || Threads.isStopped(this);
        }

        private void close() {
            WorkerTask task = activePool.removeThread(this);
            log.info("Close worker thread staring: {}, {}", task, super.getName());
            if (task == null) {
                return;
            }
            Operation ops = task.getShutdownStrategy().operation();
            boolean updated = task.updateOperation(Operation.TRIGGER, ops);
            log.info("Close worker thread update task operation: {}, {}, {}", updated, task, ops);
            ThrowingRunnable.doCaught(this::doStop, () -> "Close worker thread error: " + task + ", " + super.getName());
            if (updated) {
                ThrowingRunnable.doCaught(() -> stopTask(task, ops, "Worker shutdown"), () -> "Stop task fail: " + task);
            }
            log.info("Close worker thread end: {}, {}", task, super.getName());
        }

        @Override
        public void run() {
            try {
                while (state.isRunning() && !super.isInterrupted()) {
                    WorkerTask task = queue.poll(keepAliveTime, TimeUnit.NANOSECONDS);
                    if (task == null) {
                        log.info("Exit idle timeout worker thread: {}", super.getName());
                        break;
                    }
                    if (task != currentTask) {
                        log.error("Invalid polled worker task: {}, {}", task, currentTask);
                        break;
                    }
                    log.info("Task trace [{}] readied: {}, {}", task.getTaskId(), task.getOperation(), task.getWorker());
                    WorkerThreadPool.this.executeTask(task);
                    // return this to idle thread pool
                    if (!returnToPool()) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Poll worker task interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
            } finally {
                removeFromPool();
            }
        }
    }

}
