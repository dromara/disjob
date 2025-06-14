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

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.collect.SynchronizedSegmentMap;
import cn.ponfee.disjob.common.concurrent.*;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
import cn.ponfee.disjob.core.base.WorkerMetrics;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskResult;
import cn.ponfee.disjob.core.dto.supervisor.StopTaskParam;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.worker.exception.OperationTaskException;
import cn.ponfee.disjob.worker.exception.SavepointFailedException;
import cn.ponfee.disjob.worker.executor.ExecutionResult;
import cn.ponfee.disjob.worker.executor.ExecutionTask;
import cn.ponfee.disjob.worker.executor.JobExecutor;
import cn.ponfee.disjob.worker.executor.Savepoint;
import cn.ponfee.disjob.worker.util.JobExecutorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static cn.ponfee.disjob.core.enums.ExecuteState.*;

/**
 * Thread pool of execute task, also is a boss thread
 *
 * @author Ponfee
 */
public class WorkerThreadPool extends Thread implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerThreadPool.class);
    private static final int ERROR_MSG_MAX_LENGTH = 2048;
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
     * Task execution task queue
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
        super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(LOG));

        WorkerConfigurator.setWorkerThreadPool(this);
    }

    /**
     * Submit the execution task to thread pool
     *
     * @param task the execution task
     * @return {@code true} if thread pool accepted
     */
    public boolean submit(WorkerTask task) {
        if (threadPoolState.isStopped()) {
            return false;
        }
        LOG.info("Task trace [{}] submitted: {}, {}", task.getTaskId(), task.getOperation(), task.getWorker());
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

        LOG.info("Close worker thread pool start...");

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

        LOG.info("Close worker thread pool end.");
    }

    @Override
    public void run() {
        try {
            while (threadPoolState.isRunning() && !super.isInterrupted()) {
                executeTask();
            }
        } catch (Throwable t) {
            LOG.warn("Worker thread pool boss thread run error: {}({})", t.getClass(), t.getMessage());
            Threads.interruptIfNecessary(t);
        }
        close();
    }

    @Override
    public String toString() {
        String format = "maximum-pool-size=%d, current-pool-size=%d, active-pool-size=%d, idle-pool-size=%d, queue-task-count=%d, completed-task-count=%d";
        return String.format(format, maximumPoolSize, workerThreadCounter.get(), activePool.size(), idlePool.size(), taskQueue.size(), completedTaskCounter.get());
    }

    WorkerMetrics.ThreadPoolMetrics metrics() {
        WorkerMetrics.ThreadPoolMetrics metrics = new WorkerMetrics.ThreadPoolMetrics();
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
        if (taskQueue.stream().anyMatch(e -> e.getTaskId() == taskId)) {
            return true;
        }
        return activePool.containsKey(taskId);
    }

    // ----------------------------------------------------------------------private methods

    /**
     * Stop(Pause or Cancel) specified task
     *
     * @param stopParam the stops task param
     */
    private void stopTask(WorkerTask stopParam) {
        Operation ops = stopParam.getOperation();
        long taskId = stopParam.getTaskId();
        Assert.isTrue(ops != null && ops.isNotTrigger(), () -> "Invalid stop operation: " + ops);

        if (threadPoolState.isStopped()) {
            LOG.warn("Worker thread pool closed, discard task: {}, {}", taskId, ops);
            return;
        }

        Pair<WorkerThread, WorkerTask> pair = activePool.takeThread(taskId, null, ops);
        if (pair == null) {
            LOG.warn("Not found executing task: {}, {}", taskId, ops);
            // 支持某些异常场景时手动结束任务（如断网数据库连接不上，任务执行结束后状态无法更新，一直停留在EXECUTING）：EXECUTING -> (PAUSED|CANCELED)
            // 但要注意可能存在的操作流程上的`ABA`问题：EXECUTING -> PAUSED -> WAITING -> EXECUTING -> (PAUSED|CANCELED)
            stopTask(stopParam, ops, ops.name() + " aborted EXECUTING state task");
        } else {
            stopTask(pair, ops);
        }
    }

    private void stopTask(Pair<WorkerThread, WorkerTask> pair, Operation ops) {
        WorkerThread workerThread = pair.getLeft();
        WorkerTask task = pair.getRight();
        LOG.info("Stop task: {}, {}, {}", task.getTaskId(), ops, workerThread.getName());
        try {
            workerThread.doStop();
        } finally {
            stopTask(task, ops, null);
        }
    }

    private void executeTask() throws Throwable {
        WorkerTask task = taskQueue.takeFirst();
        WorkerThread workerThread = takeWorkerThread();

        if (workerThread.isStopped()) {
            LOG.info("Worker thread already stopped.");
            // re-execute this execution task
            taskQueue.putFirst(task);
            workerThread.doStop();
            return;
        }

        try {
            activePool.doExecute(workerThread, task);
        } catch (BrokenThreadException e) {
            LOG.error(e.getMessage());
            // re-execute this execution task
            taskQueue.putFirst(task);
            workerThread.doStop();
        } catch (IllegalTaskException e) {
            LOG.error(e.getMessage());
            // return the worker thread to idle pool
            idlePool.putFirst(workerThread);
        } catch (Throwable e) {
            workerThread.doStop();
            throw e;
        }
    }

    private WorkerThread takeWorkerThread() throws InterruptedException {
        WorkerThread workerThread = idlePool.pollFirst();
        if (workerThread != null) {
            return workerThread;
        }
        for (; ; ) {
            if (threadPoolState.isStopped() || super.isInterrupted()) {
                throw new IllegalStateException("Take worker thread interrupted.");
            }
            if ((workerThread = createWorkerThreadIfNecessary()) != null) {
                return workerThread;
            }
            if ((workerThread = idlePool.pollFirst(1000L, TimeUnit.MILLISECONDS)) != null) {
                return workerThread;
            }
        }
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
                LOG.info("Created worker thread {}, current size: {}", thread, count + 1);
                return thread;
            }
        }
        return null;
    }

    private static String toErrorMsg(Throwable t) {
        return Throwables.getRootCauseStackTrace(t, ERROR_MSG_MAX_LENGTH);
    }

    private void stopTask(WorkerTask task, Operation ops, String errorMsg) {
        stopTask(task, ops, ops.toState(), errorMsg);
    }

    private void stopTask(WorkerTask task, Operation ops, ExecuteState toState, String errorMsg) {
        Assert.notNull(ops, "Stop task operation cannot be null.");
        if (!task.updateOperation(ops, null)) {
            // stop failed
            LOG.info("Stop task failed: {}, {}, {}", task.getTaskId(), ops, toState);
            return;
        }

        completedTaskCounter.incrementAndGet();

        StopTaskParam param = task.toStopTaskParam(ops, toState, errorMsg);
        LOG.info("Stop task operation: {}, {}, {}", task.getTaskId(), ops, toState);
        Supplier<String> msgSupplier = () -> "Stop task error: " + task.getTaskId() + ", " + ops + ", " + toState;
        CoreUtils.doInSynchronized(task.getLockInstanceId(), () -> supervisorRpcClient.stopTask(param), msgSupplier);
    }

    private void stopInstance(WorkerTask task, Operation ops, String errorMsg) {
        if (!task.updateOperation(Operation.TRIGGER, ops)) {
            LOG.info("Stop instance conflict: {}, {}", task, ops);
            return;
        }

        stopTask(task, ops, errorMsg);

        Long lockInstanceId = task.getLockInstanceId();
        LOG.info("Stop instance trace: {}, {}, {}", lockInstanceId, task.getTaskId(), ops);
        Supplier<String> msgSupplier = () -> "Stop instance error: " + lockInstanceId + ", " + task.getTaskId() + ", " + ops;
        CoreUtils.doInSynchronized(lockInstanceId, () -> stopInstance(lockInstanceId, task.getTaskId(), ops), msgSupplier);
    }

    private void stopInstance(long lockInstanceId, long taskId, Operation ops) throws Exception {
        boolean res = true;
        if (ops == Operation.PAUSE) {
            res = supervisorRpcClient.pauseInstance(lockInstanceId);
        } else if (ops == Operation.EXCEPTION_CANCEL) {
            res = supervisorRpcClient.cancelInstance(lockInstanceId, ops);
        } else {
            LOG.error("Stop instance unknown: {}, {}, {}", lockInstanceId, taskId, ops);
        }
        if (!res) {
            LOG.info("Stop instance conflict: {}, {}, {}", lockInstanceId, taskId, ops);
        }
    }

    /**
     * Active thread pool
     */
    private static class ActiveThreadPool extends SynchronizedSegmentMap<Long, WorkerThread> {

        private void doExecute(WorkerThread wt, WorkerTask task) throws Throwable {
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
                    wt.execute(task);
                    map.put(task.getTaskId(), wt);
                    LOG.info("Put to active pool worker thread: {}, {}", task.getTaskId(), wt.getName());
                } catch (Throwable e) {
                    wt.setCurrentTask(null);
                    throw e;
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
                LOG.info("Taken from active pool worker thread: {}, {}", task.getTaskId(), wt.getName());
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
                LOG.info("Removed from active pool worker thread: {}, {}", task.getTaskId(), wt.getName());
                return task;
            });
        }

        private void close() {
            LOG.info("Close active thread pool start: {}", super.size());
            ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
            MultithreadExecutors.run(super.values(), WorkerThread::close, cachedThreadPool);
            cachedThreadPool.shutdown();
            LOG.info("Close active thread pool end: {}", super.size());
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
            super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(LOG));
            super.start();
        }

        private void execute(WorkerTask task) throws InterruptedException {
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
                WorkerTask task = getCurrentTask();
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
                LOG.error("Return thread to idle pool interrupted.", e);
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
                LOG.info("Worker thread not in thread pool: {}", super.getName());
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
            LOG.info("Close worker thread staring: {}, {}", task, super.getName());
            if (task == null) {
                return;
            }
            Operation ops = task.getShutdownStrategy().operation();
            boolean updated = task.updateOperation(Operation.TRIGGER, ops);
            LOG.info("Close worker thread update task operation: {}, {}, {}", updated, task, ops);
            ThrowingRunnable.doCaught(this::doStop, () -> "Close worker thread error: " + task + ", " + super.getName());
            if (updated) {
                ThrowingRunnable.doCaught(() -> stopTask(task, ops, "Worker shutdown"), () -> "Stop task fail: " + task);
            }
            LOG.info("Close worker thread end: {}, {}", task, super.getName());
        }

        @Override
        public void run() {
            while (state.isRunning() && !super.isInterrupted()) {
                WorkerTask task;
                try {
                    task = queue.poll(keepAliveTime, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    LOG.warn("Poll execution task block interrupted: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
                if (task == null) {
                    LOG.info("Worker thread exit, idle wait timeout: {}", super.getName());
                    break;
                }
                if (task != currentTask) {
                    LOG.error("Inconsistent poll task and current task: {}, {}", currentTask, task);
                    break;
                }

                try {
                    LOG.info("Task trace [{}] readied: {}, {}", task.getTaskId(), task.getOperation(), task.getWorker());
                    run(task);
                } catch (Throwable t) {
                    LOG.error("Worker thread execute failed: " + task, t);
                    ThrowingRunnable.doCaught(() -> stopTask(task, Operation.TRIGGER, EXECUTE_EXCEPTION, toErrorMsg(t)));
                }

                // return this to idle thread pool
                if (!returnToPool()) {
                    break;
                }
            }

            removeFromPool();
        }

        private void run(WorkerTask workerTask) {
            ExecutionTask executionTask;
            try {
                StartTaskResult startTaskResult = CoreUtils.doInSynchronized(
                    workerTask.getLockInstanceId(), () -> supervisorRpcClient.startTask(workerTask.toStartTaskParam()));
                if (!startTaskResult.isSuccess()) {
                    LOG.warn("Start task failed: {}, {}", workerTask, startTaskResult.getFailedMessage());
                    return;
                }
                executionTask = workerTask.toExecutionTask(startTaskResult);
            } catch (Throwable t) {
                LOG.warn("Start task error: " + workerTask, t);
                if (workerTask.getRouteStrategy().isNotBroadcast()) {
                    // reset task worker
                    List<Long> list = Collections.singletonList(workerTask.getTaskId());
                    ThrowingRunnable<?> action = () -> supervisorRpcClient.updateTaskWorker(null, list);
                    ThrowingRunnable.doCaught(action, () -> "Reset task worker error: " + workerTask);
                }
                Threads.interruptIfNecessary(t);
                return;
            }

            JobExecutor taskExecutor;
            try {
                taskExecutor = JobExecutorUtils.loadJobExecutor(workerTask.getJobExecutor());
                workerTask.bindTaskExecutor(taskExecutor);
                taskExecutor.init(executionTask);
                LOG.info("Initialized task executor: {}", workerTask.getTaskId());
            } catch (Throwable t) {
                LOG.error("Initialize task executor error: " + workerTask, t);
                stopTask(workerTask, Operation.TRIGGER, INITIALIZE_EXCEPTION, toErrorMsg(t));
                Threads.interruptIfNecessary(t);
                return;
            }

            try {
                execute(workerTask, taskExecutor, executionTask);
            } catch (OperationTaskException e) {
                LOG.error("Operation task exception: {}, {}, {}", e.operation(), workerTask, e.getMessage());
                stopInstance(workerTask, e.operation(), toErrorMsg(e));
            } catch (Throwable t) {
                if (t instanceof java.lang.ThreadDeath) {
                    // 调用`Thread#stop()`时可能会抛出该异常
                    LOG.warn("Execute task thread death: {}, {}", workerTask, t.getMessage());
                } else if (t instanceof InterruptedException) {
                    LOG.warn("Execute task interrupted: {}, {}", workerTask, t.getMessage());
                } else {
                    LOG.error("Execute task error: " + workerTask, t);
                }
                stopTask(workerTask, Operation.TRIGGER, EXECUTE_EXCEPTION, toErrorMsg(t));
                Threads.interruptIfNecessary(t);
            } finally {
                try {
                    taskExecutor.destroy();
                    LOG.info("Destroyed task executor: {}", workerTask.getTaskId());
                } catch (Throwable t) {
                    LOG.error("Destroy task executor error: " + workerTask, t);
                }
            }
        }

        private void execute(WorkerTask workerTask, JobExecutor taskExecutor, ExecutionTask executionTask) throws Exception {
            if (workerTask.getExecuteTimeout() > 0) {
                long expectTime = System.currentTimeMillis() + workerTask.getExecuteTimeout();
                Runnable stopTimeoutTask = () -> {
                    long delayedTime = System.currentTimeMillis() - expectTime;
                    Operation ops = Operation.TIMEOUT_CANCEL;
                    Pair<WorkerThread, WorkerTask> pair = activePool.takeThread(workerTask.getTaskId(), workerTask, ops);
                    if (pair != null) {
                        LOG.error("Stop timeout task: {}, {}", workerTask.getTaskId(), delayedTime);
                        stopTask(pair, ops);
                    } else {
                        LOG.info("Skip timeout task: {}, {}", workerTask.getTaskId(), delayedTime);
                    }
                };
                commonScheduledPool().schedule(stopTimeoutTask, workerTask.getExecuteTimeout(), TimeUnit.MILLISECONDS);
            }

            ExecutionResult result = taskExecutor.execute(executionTask, new TaskSavepoint(workerTask));
            if (result != null && result.isSuccess()) {
                Operation ops = workerTask.getOperation();
                LOG.info("Execute task success: {}, {}, {}", workerTask.getTaskId(), ops, result.getMsg());
                if (ops != Operation.TRIGGER) {
                    // 如果task被非TRIGGER操作，但又正常执行成功，则要更新回TRIGGER
                    boolean updated = workerTask.updateOperation(ops, Operation.TRIGGER);
                    LOG.info("Execute task revert to trigger: {}, {}, {}", workerTask.getTaskId(), ops, updated);
                }
                stopTask(workerTask, Operation.TRIGGER, COMPLETED, null);
            } else {
                LOG.error("Execute task failed: {}, {}", workerTask, result);
                stopTask(workerTask, Operation.TRIGGER, EXECUTE_FAILED, Objects.toString(result, "null"));
            }
        }
    } // end of worker thread class definition

}
