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
import cn.ponfee.disjob.common.base.TripState;
import cn.ponfee.disjob.common.concurrent.LoggedUncaughtExceptionHandler;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
import cn.ponfee.disjob.core.base.WorkerMetrics;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.exception.CancelTaskException;
import cn.ponfee.disjob.core.exception.PauseTaskException;
import cn.ponfee.disjob.core.exception.SavepointFailedException;
import cn.ponfee.disjob.core.handle.ExecuteResult;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.handle.Savepoint;
import cn.ponfee.disjob.core.handle.TaskExecutor;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import cn.ponfee.disjob.core.handle.execution.WorkflowPredecessorNode;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.supervisor.StartTaskParam;
import cn.ponfee.disjob.core.param.supervisor.TerminateTaskParam;
import cn.ponfee.disjob.core.param.supervisor.UpdateTaskWorkerParam;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;
import static cn.ponfee.disjob.core.enums.ExecuteState.*;

/**
 * Thread pool of execute task, also is a boss thread
 *
 * @author Ponfee
 */
public class WorkerThreadPool extends Thread implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerThreadPool.class);
    private static final int ERROR_MSG_MAX_LENGTH = 2048;

    /**
     * Supervisor rpc client
     */
    private final SupervisorRpcService supervisorRpcClient;

    /**
     * Maximum pool size
     */
    private volatile int maximumPoolSize;

    /**
     * Worker thread keep alive time seconds
     */
    private final long keepAliveTimeSeconds;

    /**
     * Active worker thread pool
     */
    private final ActiveThreadPool                 activePool = new ActiveThreadPool();

    /**
     * Idle worker thread pool
     */
    private final LinkedBlockingDeque<WorkerThread>  idlePool = new LinkedBlockingDeque<>();

    /**
     * Task execution task queue
     */
    private final LinkedBlockingDeque<ExecuteTaskParam> taskQueue = new LinkedBlockingDeque<>();

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
    private final TripState threadPoolState = TripState.createStarted();

    public WorkerThreadPool(int maximumPoolSize,
                            long keepAliveTimeSeconds,
                            SupervisorRpcService supervisorRpcClient) {
        SingletonClassConstraint.constrain(this);

        Assert.isTrue(maximumPoolSize > 0, "Maximum pool size must be positive number.");
        Assert.isTrue(keepAliveTimeSeconds > 0, "Keep alive time seconds must be positive number.");
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
        this.supervisorRpcClient = supervisorRpcClient;

        super.setDaemon(true);
        super.setName(getClass().getSimpleName());
        super.setPriority(Thread.MAX_PRIORITY);
        super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(LOG));

        WorkerConfigurator.setWorkerThreadPool(this);
    }

    /**
     * Submit task execution task to thread pool
     *
     * @param task the execution task
     * @return {@code true} if thread pool accepted
     */
    public boolean submit(ExecuteTaskParam task) {
        if (threadPoolState.isStopped()) {
            return false;
        }

        LOG.info("Task trace [{}] submitted: {}, {}", task.getTaskId(), task.getOperation(), task.getWorker());
        if (task.operation().isTrigger()) {
            return taskQueue.offerLast(task);
        } else {
            ThreadPoolExecutors.commonThreadPool().execute(() -> stopTask(task));
            return true;
        }
    }

    @PreDestroy
    @Override
    public void close() {
        if (!threadPoolState.stop()) {
            LOG.warn("Repeat call close method{}{}", "\n", Threads.getStackTrace());
            return;
        }

        LOG.info("Close worker thread pool start...");

        if (Thread.currentThread() != this) {
            // stop this boss thread
            ThrowingRunnable.doCaught(super::interrupt);
            ThrowingRunnable.doCaught(() -> Threads.stopThread(this, 5000));
        }

        // change executing pool thread state
        ThrowingRunnable.doCaught(activePool::toStopPool);
        // change idle pool thread state
        idlePool.forEach(e -> ThrowingRunnable.doCaught(e::toStop));

        // stop idle pool thread
        idlePool.forEach(e -> ThrowingRunnable.doCaught(() -> stopWorkerThread(e, true)));
        ThrowingRunnable.doCaught(idlePool::clear);
        // stop executing pool thread
        ThrowingRunnable.doCaught(activePool::doStopPool);
        // clear task execution task queue
        ThrowingRunnable.doCaught(taskQueue::clear);

        LOG.info("Close worker thread pool end.");
    }

    @Override
    public void run() {
        try {
            while (threadPoolState.isRunning() && !super.isInterrupted()) {
                executeTask();
            }
        } catch (Throwable t) {
            LOG.error("Thread pool running occur error.", t);
            Threads.interruptIfNecessary(t);
        }

        close();
    }

    @Override
    public String toString() {
        return String.format(
            "maximum-pool-size=%d, current-pool-size=%d, active-pool-size=%d, idle-pool-size=%d, queue-task-count=%d, completed-task-count=%d",
            maximumPoolSize, workerThreadCounter.get(), activePool.size(), idlePool.size(), taskQueue.size(), completedTaskCounter.get()
        );
    }

    WorkerMetrics.ThreadPoolMetrics metrics() {
        WorkerMetrics.ThreadPoolMetrics metrics = new WorkerMetrics.ThreadPoolMetrics();
        metrics.setClosed(threadPoolState.isStopped());
        metrics.setKeepAliveTime(keepAliveTimeSeconds);
        metrics.setMaximumPoolSize(maximumPoolSize);
        metrics.setCurrentPoolSize(workerThreadCounter.get());
        metrics.setActivePoolSize(activePool.size());
        metrics.setIdlePoolSize(idlePool.size());
        metrics.setQueueTaskCount(taskQueue.size());
        metrics.setCompletedTaskCount(completedTaskCounter.get());
        return metrics;
    }

    synchronized void modifyMaximumPoolSize(int value) {
        Assert.isTrue(value > 0 && value <= ThreadPoolExecutors.MAX_CAP, "Maximum pool size must be range [1, 32767].");
        this.maximumPoolSize = value;
    }

    synchronized void clearTaskQueue() {
        List<ExecuteTaskParam> tasks = new LinkedList<>();
        taskQueue.drainTo(tasks);

        List<UpdateTaskWorkerParam> list = tasks.stream()
            // 广播任务分派的worker不可修改，需要排除
            .filter(e -> e.getRouteStrategy() != RouteStrategy.BROADCAST)
            // 清除分派worker数据
            .map(e -> new UpdateTaskWorkerParam(e.getTaskId(), null))
            .collect(Collectors.toList());

        for (List<UpdateTaskWorkerParam> sub : Lists.partition(list, PROCESS_BATCH_SIZE)) {
            // 更新task的worker信息
            ThrowingRunnable.doCaught(() -> supervisorRpcClient.updateTaskWorker(sub), () -> "Update task worker error: " + Jsons.toJson(list));
        }
    }

    boolean existsTask(long taskId) {
        if (activePool.existsTask(taskId)) {
            return true;
        }
        for (ExecuteTaskParam task : taskQueue) {
            if (task.getTaskId() == taskId) {
                return true;
            }
        }
        return activePool.existsTask(taskId);
    }

    // ----------------------------------------------------------------------private methods

    /**
     * Stop(Pause or Cancel) specified task
     *
     * @param stopParam the stops task param
     */
    private void stopTask(ExecuteTaskParam stopParam) {
        Operation ops = stopParam.operation();
        Assert.isTrue(ops != null && ops.isNotTrigger(), () -> "Invalid stop operation: " + ops);

        if (threadPoolState.isStopped()) {
            return;
        }

        long taskId = stopParam.getTaskId();
        Pair<WorkerThread, ExecuteTaskParam> pair = activePool.stopTask(taskId, ops);
        if (pair == null) {
            LOG.warn("Not found executing task: {}, {}", taskId, ops);
            // 支持某些异常场景时手动结束任务（如断网数据库连接不上，任务执行结束后状态无法更新，一直停留在EXECUTING）：EXECUTING -> (PAUSED|CANCELED)
            // 但要注意可能存在的操作流程上的`ABA`问题：EXECUTING -> PAUSED -> WAITING -> EXECUTING -> (PAUSED|CANCELED)
            terminateTask(this, stopParam, ops, ops.toState(), ops.name() + " aborted EXECUTING state task");
            return;
        }

        WorkerThread workerThread = pair.getLeft();
        ExecuteTaskParam task = pair.getRight();
        LOG.info("Stop task: {}, {}, {}", taskId, ops, workerThread.getName());
        try {
            // stop the work thread
            stopWorkerThread(workerThread, true);
        } finally {
            terminateTask(this, task, ops, ops.toState(), null);
        }
    }

    private void executeTask() throws InterruptedException {
        ExecuteTaskParam task = taskQueue.takeFirst();
        WorkerThread workerThread = takeWorkerThread();

        if (workerThread.isStopped()) {
            LOG.info("Worker thread already stopped.");
            // re-execute this execution task
            taskQueue.putFirst(task);
            // destroy this worker thread
            stopWorkerThread(workerThread, true);
            return;
        }

        try {
            activePool.doExecute(workerThread, task);
        } catch (InterruptedException e) {
            // destroy this worker thread
            stopWorkerThread(workerThread, true);
            throw e;
        } catch (BrokenThreadException e) {
            LOG.error(e.getMessage());
            // re-execute this execution task
            taskQueue.putFirst(task);
            // destroy this worker thread
            stopWorkerThread(workerThread, true);
        } catch (IllegalTaskException e) {
            LOG.error(e.getMessage());
            // return this worker thread
            idlePool.putFirst(workerThread);
        } catch (DuplicateTaskException e) {
            LOG.error(e.getMessage());
            // cancel this execution task
            terminateTask(this, task, Operation.TRIGGER, VERIFY_FAILED, toErrorMsg(e));

            // return this worker thread
            idlePool.putFirst(workerThread);
        }
    }

    private WorkerThread takeWorkerThread() throws InterruptedException {
        // take a worker
        WorkerThread workerThread = idlePool.pollFirst();
        if (workerThread != null) {
            return workerThread;
        }

        for (; ; ) {
            if (threadPoolState.isStopped() || super.isInterrupted()) {
                throw new IllegalStateException("Take worker thread interrupted.");
            }
            workerThread = createWorkerThreadIfNecessary();
            if (workerThread == null) {
                workerThread = idlePool.pollFirst(1000, TimeUnit.MILLISECONDS);
            }
            if (workerThread != null) {
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
                WorkerThread thread = new WorkerThread(this, keepAliveTimeSeconds);
                LOG.info("Created worker thread, current size: {}", count + 1);
                return thread;
            }
        }
        return null;
    }

    /**
     * Task executed finished, then return the worker thread to idle pool.
     * <p>Called this method current thread is WorkerThread
     *
     * @param workerThread the worker thread
     * @return {@code true} if return to idle pool successfully
     */
    private boolean returnWorkerThread(WorkerThread workerThread) {
        if (activePool.removeThread(workerThread) == null) {
            // maybe already removed by other operation
            LOG.error("Return thread failed, because not found: {}", workerThread.getName());
            return false;
        }

        // return the detached worker thread to idle pool
        try {
            idlePool.putFirst(workerThread);
            return true;
        } catch (InterruptedException e) {
            LOG.error("Return thread to idle pool interrupted.", e);
            stopWorkerThread(workerThread, false);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Remove the worker thread from active pool and destroy it.
     *
     * @param workerThread the worker thread
     */
    private void removeWorkerThread(WorkerThread workerThread) {
        workerThread.toStop();

        boolean hasRemoved = activePool.removeThread(workerThread) != null;

        if (!hasRemoved) {
            hasRemoved = idlePool.remove(workerThread);
        }

        if (!hasRemoved) {
            LOG.warn("Not found removable thread: {}", workerThread.getName());
        }

        stopWorkerThread(workerThread, false);
    }

    /**
     * Stop and discard the worker thread
     *
     * @param workerThread the worker thread
     * @param doStop       if whether do stop the thread
     */
    private static void stopWorkerThread(WorkerThread workerThread, boolean doStop) {
        workerThread.toStop();
        if (doStop) {
            LOG.info("Do stop the worker thread: {}", workerThread.getName());
            workerThread.doStop();
        }
    }

    private static String toErrorMsg(Throwable t) {
        if (t == null) {
            return null;
        }
        String errorMsg = Throwables.getRootCauseStackTrace(t);
        if (errorMsg.length() > ERROR_MSG_MAX_LENGTH) {
            errorMsg = errorMsg.substring(0, ERROR_MSG_MAX_LENGTH);
        }
        return errorMsg;
    }

    private static void terminateTask(WorkerThreadPool threadPool, ExecuteTaskParam task, Operation ops, ExecuteState toState, String errorMsg) {
        Assert.notNull(ops, "Terminate task operation cannot be null.");
        Assert.notNull(task.getWorker(), "Execute task param worker cannot be null.");
        if (!task.updateOperation(ops, null)) {
            // already terminated
            LOG.warn("Terminate task conflict: {}, {}, {}", task.getTaskId(), ops, toState);
            return;
        }

        TerminateTaskParam terminateTaskParam = new TerminateTaskParam(
            task.getInstanceId(), task.getWnstanceId(), task.getTaskId(),
            task.getWorker().serialize(), ops, toState, errorMsg
        );
        threadPool.completedTaskCounter.incrementAndGet();
        long lockInstanceId = task.getWnstanceId() != null ? task.getWnstanceId() : task.getInstanceId();
        try {
            synchronized (JobConstants.INSTANCE_LOCK_POOL.intern(lockInstanceId)) {
                if (!threadPool.supervisorRpcClient.terminateTask(terminateTaskParam)) {
                    LOG.warn("Terminate task failed: {}, {}, {}", task.getTaskId(), ops, toState);
                }
            }
        } catch (Throwable t) {
            LOG.error("Terminate task occur error: {}, {}, {}", task.getTaskId(), ops, toState);
            Threads.interruptIfNecessary(t);
        }
    }

    /**
     * Active thread pool
     */
    private class ActiveThreadPool {
        //final BiMap<Long, WorkerThread> pool = Maps.synchronizedBiMap(HashBiMap.create());
        final Map<Long, WorkerThread> pool = new HashMap<>();

        private synchronized void doExecute(WorkerThread workerThread, ExecuteTaskParam task) throws InterruptedException {
            Operation operation = (task == null) ? null : task.operation();
            if (operation == null || operation.isNotTrigger()) {
                // cannot happen
                throw new IllegalTaskException("Not a executable task operation: " + task);
            }

            WorkerThread exists = pool.get(task.getTaskId());
            if (exists != null) {
                ExecuteTaskParam t = exists.currentTask();
                if (task.equals(t)) {
                    // 同一个task re-dispatch，导致重复
                    throw new IllegalTaskException("Repeat execute task: " + task);
                } else {
                    // 如果task分表时，不同task分表的task-id会有重复的可能性(task不做分片表时不会存在该问题)
                    throw new DuplicateTaskException("Duplicate task id: " + task + ", " + t);
                }
            }

            if (!workerThread.updateCurrentTask(null, task)) {
                ExecuteTaskParam t = workerThread.currentTask();
                throw new BrokenThreadException("Execute worker thread conflict: " + workerThread.getName() + ", " + task + ", " + t);
            }

            try {
                workerThread.execute(task);
            } catch (Throwable throwable) {
                workerThread.updateCurrentTask(task, null);
                throw throwable;
            }

            pool.put(task.getTaskId(), workerThread);
        }

        private synchronized Pair<WorkerThread, ExecuteTaskParam> stopTask(long taskId, Operation ops) {
            WorkerThread thread = pool.get(taskId);
            ExecuteTaskParam task;
            if (thread == null || (task = thread.currentTask()) == null) {
                return null;
            }

            if (!task.updateOperation(Operation.TRIGGER, ops)) {
                return null;
            }

            if (!thread.updateCurrentTask(task, null)) {
                // cannot happen
                LOG.error("Stop task clear current task failed: {}", task);
                return null;
            }
            pool.remove(taskId);
            LOG.info("Removed active pool worker thread: {}, {}", thread.getName(), task.getTaskId());
            return Pair.of(thread, task);
        }

        private synchronized ExecuteTaskParam removeThread(WorkerThread workerThread) {
            ExecuteTaskParam task = workerThread.currentTask();
            if (task == null) {
                return null;
            }

            if (!workerThread.updateCurrentTask(task, null)) {
                // cannot happen
                LOG.error("Remove thread clear current task failed: {}", task);
                return null;
            }

            WorkerThread removed = pool.remove(task.getTaskId());

            if (workerThread != removed) {
                // cannot happen
                LOG.error("Inconsistent removed worker thread: {}, {}, {}", task.getTaskId(), workerThread.getName(), removed.getName());
                return null;
            }

            return task;
        }

        private synchronized void toStopPool() {
            pool.forEach((taskId, workerThread) -> {
                workerThread.toStop();
                ExecuteTaskParam task = workerThread.currentTask();
                if (task != null) {
                    task.stop();
                }
            });
        }

        private synchronized void doStopPool() {
            pool.values().parallelStream().forEach(workerThread -> {
                ExecuteTaskParam task = workerThread.currentTask();
                Operation ops = Operation.PAUSE;

                // 1、first change the execution task operation
                boolean success = (task != null) && task.updateOperation(Operation.TRIGGER, ops);

                // 2、then stop the work thread
                ThrowingRunnable.doCaught(() -> stopWorkerThread(workerThread, true), () -> "Stop worker thread error: " + task + ", " + workerThread);

                // 3、finally update the sched task state
                if (success) {
                    ThrowingRunnable.doCaught(() -> terminateTask(WorkerThreadPool.this, task, ops, ops.toState(), null), () -> "Terminate task error: " + task);
                } else {
                    LOG.warn("Change execution task ops failed on thread pool close: {}, {}", task, ops);
                }

                workerThread.updateCurrentTask(task, null);
            });

            pool.clear();
        }

        private int size() {
            return pool.size();
        }

        private boolean existsTask(Long taskId) {
            return pool.containsKey(taskId);
        }
    }

    private static class IllegalTaskException extends RuntimeException {
        private static final long serialVersionUID = -1273937229826200274L;

        private IllegalTaskException(String message) {
            super(message);
        }
    }

    private static class DuplicateTaskException extends RuntimeException {
        private static final long serialVersionUID = -5210570253941551115L;

        private DuplicateTaskException(String message) {
            super(message);
        }
    }

    private static class BrokenThreadException extends RuntimeException {
        private static final long serialVersionUID = 3475868254991118684L;

        private BrokenThreadException(String message) {
            super(message);
        }
    }

    private static class TaskSavepoint implements Savepoint {
        private static final int SNAPSHOT_MAX_LENGTH = 65535;
        private final SupervisorRpcService client;
        private final long taskId;

        private TaskSavepoint(SupervisorRpcService client, long taskId) {
            this.client = client;
            this.taskId = taskId;
        }

        @Override
        public void save(String executeSnapshot) throws Exception {
            if (executeSnapshot != null && executeSnapshot.length() > SNAPSHOT_MAX_LENGTH) {
                throw new SavepointFailedException("Execution snapshot length to large " + executeSnapshot.length());
            }
            if (!client.savepoint(taskId, executeSnapshot)) {
                throw new SavepointFailedException("Save execution snapshot data occur error.");
            }
        }
    }

    /**
     * Worker thread
     */
    private static class WorkerThread extends Thread {
        private static final AtomicInteger NAMED_SEQ = new AtomicInteger(1);
        private static final AtomicInteger FUTURE_TASK_NAMED_SEQ = new AtomicInteger(1);

        /**
         * Worker thread pool reference
         */
        private final WorkerThreadPool threadPool;

        /**
         * Thread keep alive time
         */
        private final long keepAliveTime;

        /**
         * Work queue
         */
        private final BlockingQueue<ExecuteTaskParam> workQueue = new SynchronousQueue<>();

        /**
         * Worker thread state
         */
        private final TripState workerThreadState = TripState.createStarted();

        /**
         * Atomic reference object of current task
         */
        private final AtomicReference<ExecuteTaskParam> currentTask = new AtomicReference<>();

        private WorkerThread(WorkerThreadPool threadPool, long keepAliveTimeSeconds) {
            this.threadPool = threadPool;
            this.keepAliveTime = TimeUnit.SECONDS.toNanos(keepAliveTimeSeconds);

            super.setDaemon(true);
            super.setName(getClass().getSimpleName() + "-" + NAMED_SEQ.getAndIncrement());
            super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(LOG));
            super.start();
        }

        private void execute(ExecuteTaskParam task) throws InterruptedException {
            if (isStopped()) {
                throw new BrokenThreadException("Worker thread already stopped: " + super.getName());
            }
            if (!workQueue.offer(task, 5000, TimeUnit.MILLISECONDS)) {
                throw new BrokenThreadException("Put to worker thread queue timeout: " + super.getName());
            }
        }

        private void toStop() {
            if (workerThreadState.stop()) {
                threadPool.workerThreadCounter.decrementAndGet();
                ExecuteTaskParam task = currentTask();
                if (task != null) {
                    task.stop();
                }
            }
        }

        private void doStop() {
            toStop();
            Threads.stopThread(this, 5000);
        }

        private boolean updateCurrentTask(ExecuteTaskParam expect, ExecuteTaskParam update) {
            return currentTask.compareAndSet(expect, update);
        }

        private ExecuteTaskParam currentTask() {
            return currentTask.get();
        }

        private boolean isStopped() {
            return workerThreadState.isStopped() || Threads.isStopped(this);
        }

        @Override
        public void run() {
            while (workerThreadState.isRunning()) {
                if (super.isInterrupted()) {
                    LOG.warn("Worker thread run interrupted.");
                    break;
                }

                ExecuteTaskParam task;
                try {
                    task = workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    LOG.warn("Poll execution task block interrupted: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }

                ExecuteTaskParam current = currentTask();
                if (task == null) {
                    if (current == null) {
                        LOG.info("Worker thread exit, idle wait timeout: {}", super.getName());
                        break;
                    }
                    if ((task = workQueue.poll()) == null) {
                        // cannot happen
                        LOG.error("Not poll task, but has current task: {}", current);
                        break;
                    }
                }

                if (current != task) {
                    // cannot happen
                    LOG.error("Inconsistent poll task and current task: {}, {}", current, task);
                    break;
                }

                try {
                    LOG.info("Task trace [{}] readied: {}, {}", task.getTaskId(), task.getOperation(), task.getWorker());
                    runTask(task);
                } catch (Throwable t) {
                    LOG.error("Worker thread execute failed: " + task, t);
                    final ExecuteTaskParam task0 = task;
                    ThrowingRunnable.doCaught(() -> terminateTask(threadPool, task0, Operation.TRIGGER, EXECUTE_EXCEPTION, toErrorMsg(t)));
                }

                // return this to idle thread pool
                if (!threadPool.returnWorkerThread(this)) {
                    break;
                }
            }

            threadPool.removeWorkerThread(this);
        }

        private void runTask(ExecuteTaskParam task) {
            SchedTask schedTask;
            ExecutingTask executingTask;
            SupervisorRpcService client = threadPool.supervisorRpcClient;
            try {
                if ((schedTask = client.getTask(task.getTaskId())) == null) {
                    LOG.error("Sched task not found {}", task);
                    return;
                }

                ExecuteState fromState = ExecuteState.of(schedTask.getExecuteState());
                if (fromState != WAITING) {
                    LOG.warn("Task state not executable: {}, {}, {}", schedTask.getTaskId(), fromState, task.operation());
                    return;
                }

                // build executing task
                List<WorkflowPredecessorNode> nodes = null;
                if (task.getJobType() == JobType.WORKFLOW) {
                    nodes = client.findWorkflowPredecessorNodes(task.getWnstanceId(), task.getInstanceId());
                }
                executingTask = ExecutingTask.of(task.getJobId(), task.getWnstanceId(), schedTask, nodes);

                // update database records start state(sched_instance, sched_task)
                // 这里存在调用超时，但实际supervisor启动task成功的情况
                // 后续版本可考虑加一个请求ID标识，返回值也加上启动成功的那个请求ID。在重试时通过判断返回的请求ID是否等于本次的请求ID，来识别是否本次启动成功的
                if (!client.startTask(new StartTaskParam(task.getInstanceId(), task.getTaskId(), task.getWorker()))) {
                    LOG.warn("Task start conflicted {}", task);
                    return;
                }
            } catch (Throwable t) {
                LOG.warn("Start task fail: " + task, t);
                if (task.getRouteStrategy() != RouteStrategy.BROADCAST) {
                    // reset task worker
                    List<UpdateTaskWorkerParam> list = Collections.singletonList(new UpdateTaskWorkerParam(task.getTaskId(), null));
                    ThrowingRunnable.doCaught(() -> client.updateTaskWorker(list), () -> "Reset task worker occur error: " + task);
                }
                Threads.interruptIfNecessary(t);
                // discard task
                return;
            }

            // 1、prepare
            TaskExecutor taskExecutor;
            try {
                taskExecutor = JobHandlerUtils.load(task.getJobHandler());
                task.taskExecutor(taskExecutor);
            } catch (Throwable t) {
                LOG.error("Load job handler error: " + task, t);
                terminateTask(threadPool, task, Operation.TRIGGER, INSTANCE_FAILED, toErrorMsg(t));
                return;
            }

            // 2、init
            try {
                taskExecutor.init(executingTask);
                LOG.info("Initiated sched task {}", task.getTaskId());
            } catch (Throwable t) {
                LOG.error("Task init error: " + task, t);
                terminateTask(threadPool, task, Operation.TRIGGER, INIT_EXCEPTION, toErrorMsg(t));
                Threads.interruptIfNecessary(t);
                return;
            }

            // 3、execute
            try {
                ExecuteResult result;
                Savepoint savepoint = new TaskSavepoint(client, executingTask.getTaskId());
                if (task.getExecuteTimeout() > 0) {
                    FutureTask<ExecuteResult> futureTask = new FutureTask<>(() -> taskExecutor.execute(executingTask, savepoint));
                    String threadName = getClass().getSimpleName() + "#FutureTaskThread" + "-" + FUTURE_TASK_NAMED_SEQ.getAndIncrement();
                    Thread futureTaskThread = Threads.newThread(threadName, true, Thread.NORM_PRIORITY, futureTask, LOG);
                    futureTaskThread.start();
                    try {
                        result = futureTask.get(task.getExecuteTimeout(), TimeUnit.MILLISECONDS);
                    } finally {
                        Threads.stopThread(futureTaskThread, 0);
                    }
                } else {
                    result = taskExecutor.execute(executingTask, savepoint);
                }

                // 4、execute end
                if (result != null && result.isSuccess()) {
                    LOG.info("Task execute finished: {}, {}", task.getTaskId(), result.getMsg());
                    terminateTask(threadPool, task, Operation.TRIGGER, FINISHED, null);
                } else {
                    LOG.error("Task execute failed: {}, {}", task, result);
                    String msg = (result == null) ? "null result" : result.getMsg();
                    terminateTask(threadPool, task, Operation.TRIGGER, EXECUTE_FAILED, msg);
                }
            } catch (TimeoutException e) {
                LOG.error("Task execute timeout: " + task, e);
                terminateTask(threadPool, task, Operation.TRIGGER, EXECUTE_TIMEOUT, toErrorMsg(e));
            } catch (PauseTaskException e) {
                LOG.error("Pause task exception: {}, {}", task, e.getMessage());
                stopInstance(task, Operation.PAUSE, toErrorMsg(e));
            } catch (CancelTaskException e) {
                LOG.error("Cancel task exception:  {}, {}", task, e.getMessage());
                stopInstance(task, Operation.EXCEPTION_CANCEL, toErrorMsg(e));
            } catch (Throwable t) {
                if (t instanceof java.lang.ThreadDeath) {
                    // 调用`Thread#stop()`时会抛出该异常，如果捕获到`ThreadDeath`异常，建议重新抛出以使线程中止
                    LOG.warn("Task execute thread death: {}, {}", task, t.getMessage());
                } else if (t instanceof InterruptedException) {
                    LOG.warn("Task executed interrupted: {}, {}", task, t.getMessage());
                } else {
                    LOG.error("Task execute occur error: " + task, t);
                }
                terminateTask(threadPool, task, Operation.TRIGGER, EXECUTE_EXCEPTION, toErrorMsg(t));
                Threads.interruptIfNecessary(t);
            } finally {
                // 5、destroy
                try {
                    taskExecutor.destroy();
                    LOG.info("Destroyed sched task: {}", task.getTaskId());
                } catch (Throwable t) {
                    LOG.error("Task destroy error: " + task, t);
                }
            }
        }

        private void stopInstance(ExecuteTaskParam task, Operation ops, String errorMsg) {
            if (!task.updateOperation(Operation.TRIGGER, ops)) {
                LOG.info("Stop instance conflict: {}, {}", task, ops);
                return;
            }

            LOG.info("Stop instance task: {}, {}", task.getTaskId(), ops);
            terminateTask(threadPool, task, ops, ops.toState(), errorMsg);

            boolean res = true;
            long lockInstanceId = task.getWnstanceId() != null ? task.getWnstanceId() : task.getInstanceId();
            try {
                synchronized (JobConstants.INSTANCE_LOCK_POOL.intern(lockInstanceId)) {
                    if (ops == Operation.PAUSE) {
                        res = threadPool.supervisorRpcClient.pauseInstance(lockInstanceId);
                    } else if (ops == Operation.EXCEPTION_CANCEL) {
                        res = threadPool.supervisorRpcClient.cancelInstance(lockInstanceId, ops);
                    } else {
                        LOG.error("Stop instance unsupported operation: {}, {}", task.getTaskId(), ops);
                    }
                }
                if (!res) {
                    LOG.info("Stop instance conflict: {}, {}, {}", task.getInstanceId(), task.getTaskId(), ops);
                }
            } catch (Throwable t) {
                LOG.error("Stop instance error: " + task.getTaskId() + ", " + ops, t);
                Threads.interruptIfNecessary(t);
            }
        }

    } // end of worker thread class definition

}
