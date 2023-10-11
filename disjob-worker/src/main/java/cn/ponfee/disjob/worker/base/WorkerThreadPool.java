/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.LoggedUncaughtExceptionHandler;
import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.core.base.SupervisorCoreRpcService;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.exception.CancelTaskException;
import cn.ponfee.disjob.core.exception.PauseTaskException;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.handle.TaskExecutor;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import cn.ponfee.disjob.core.handle.execution.WorkflowPredecessorNode;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import cn.ponfee.disjob.core.param.StartTaskParam;
import cn.ponfee.disjob.core.param.TaskWorkerParam;
import cn.ponfee.disjob.core.param.TerminateTaskParam;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static cn.ponfee.disjob.core.enums.ExecuteState.*;

/**
 * Thread pool of execute task, also is a boss thread
 *
 * @author Ponfee
 */
public class WorkerThreadPool extends Thread implements Closeable {

    private final static Logger LOG = LoggerFactory.getLogger(WorkerThreadPool.class);
    private final static int ERROR_MSG_MAX_LENGTH = 1024;

    /**
     * This jdk thread pool for asynchronous to stop(pause or cancel) task
     */
    private final ThreadPoolExecutor stopTaskExecutor = ThreadPoolExecutors.builder()
        .corePoolSize(10)
        .maximumPoolSize(30)
        .workQueue(new LinkedBlockingQueue<>(200))
        .keepAliveTimeSeconds(300)
        .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
        .threadFactory(NamedThreadFactory.builder().prefix("stop_task_operation").priority(Thread.MAX_PRIORITY).build())
        .build();

    /**
     * Supervisor core rpc client
     */
    private final SupervisorCoreRpcService supervisorCoreRpcClient;

    /**
     * Maximum pool size
     */
    private final int maximumPoolSize;

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
     * Task execution param queue
     */
    private final LinkedBlockingDeque<ExecuteTaskParam> taskQueue = new LinkedBlockingDeque<>();

    /**
     * Counts worker thread number
     */
    private final AtomicInteger workerThreadCounter = new AtomicInteger(0);

    /**
     * Pool is whether closed status
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public WorkerThreadPool(int maximumPoolSize,
                            long keepAliveTimeSeconds,
                            SupervisorCoreRpcService supervisorCoreRpcClient) {
        SingletonClassConstraint.constrain(this);

        Assert.isTrue(maximumPoolSize > 0, "Maximum pool size must be positive number.");
        Assert.isTrue(keepAliveTimeSeconds > 0, "Keep alive time seconds must be positive number.");
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
        this.supervisorCoreRpcClient = supervisorCoreRpcClient;

        super.setDaemon(true);
        super.setName(getClass().getSimpleName());
        super.setPriority(Thread.MAX_PRIORITY);
        super.setUncaughtExceptionHandler(LoggedUncaughtExceptionHandler.INSTANCE);
    }

    /**
     * Submit task execution param to thread pool
     *
     * @param param the execution param
     * @return {@code true} if thread pool accepted
     */
    public boolean submit(ExecuteTaskParam param) {
        if (closed.get()) {
            return false;
        }

        LOG.info("Submitted task {} | {} | {}", param.getTaskId(), param.getOperation(), param.getWorker());
        if (param.operation().isTrigger()) {
            return taskQueue.offerLast(param);
        } else {
            stopTaskExecutor.execute(() -> stop(param));
            return true;
        }
    }

    /**
     * Stop(Pause or Cancel) specified task
     *
     * @param stopParam the stops task param
     */
    private void stop(ExecuteTaskParam stopParam) {
        Operations ops = stopParam.operation();
        Assert.isTrue(ops != null && ops.isNotTrigger(), () -> "Invalid stop operation: " + ops);

        if (closed.get()) {
            return;
        }

        long taskId = stopParam.getTaskId();
        Pair<WorkerThread, ExecuteTaskParam> pair = activePool.stopTask(taskId, ops);
        if (pair == null) {
            LOG.info("Not found stoppable task {} | {}", taskId, ops);
            return;
        }

        WorkerThread workerThread = pair.getLeft();
        ExecuteTaskParam param = pair.getRight();
        LOG.info("Stop task: {} | {} | {}", taskId, ops, workerThread.getName());
        try {
            // stop the work thread
            stopWorkerThread(workerThread, true);
        } finally {
            terminateTask(supervisorCoreRpcClient, param, ops, ops.toState(), null);
        }
    }

    @PreDestroy
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            LOG.warn("Repeat call close method." + "\n" + ObjectUtils.getStackTrace());
            return;
        }

        LOG.info("Close worker thread pool start...");

        // 1、prepare close
        // 1.1、change executing pool thread state
        ThrowingRunnable.execute(activePool::stopPool);

        // 1.2、change idle pool thread state
        idlePool.forEach(e -> ThrowingRunnable.execute(e::toStop));

        // 2、do close
        // 2.1、stop this boss thread
        ThrowingRunnable.execute(() -> Threads.stopThread(this, 200));

        // 2.2、stop idle pool thread
        idlePool.forEach(e -> ThrowingRunnable.execute(() -> stopWorkerThread(e, true)));
        ThrowingRunnable.execute(idlePool::clear);

        // 2.3、stop executing pool thread
        ThrowingRunnable.execute(activePool::closePool);
        workerThreadCounter.set(0);

        // 2.4、shutdown jdk thread pool
        ThreadPoolExecutors.shutdown(stopTaskExecutor, 1);

        // 2.5、clear task execution param queue
        ThrowingRunnable.execute(taskQueue::clear);

        LOG.info("Close worker thread pool end.");
    }

    private WorkerThread takeWorkerThread() throws InterruptedException {
        while (true) {
            if (closed.get() || super.isInterrupted()) {
                throw new IllegalStateException("Take worker thread interrupted.");
            }
            WorkerThread workerThread = createWorkerThreadIfNecessary();
            if (workerThread == null) {
                LOG.info("Take worker thread with timeout from idle pool.");
                workerThread = idlePool.pollFirst(1000, TimeUnit.MILLISECONDS);
            }
            if (workerThread != null) {
                return workerThread;
            }
        }
    }

    @Override
    public void run() {
        try {
            while (!closed.get()) {
                if (super.isInterrupted()) {
                    throw new IllegalStateException("Boss thread run interrupted.");
                }
                ExecuteTaskParam param = taskQueue.takeFirst();

                // take a worker
                WorkerThread workerThread = idlePool.pollFirst();
                if (workerThread == null) {
                    workerThread = takeWorkerThread();
                }

                if (workerThread.isStopped()) {
                    LOG.info("Worker thread already stopped.");
                    // re-execute this execution param
                    taskQueue.putFirst(param);
                    // destroy this worker thread
                    stopWorkerThread(workerThread, true);
                    continue;
                }

                try {
                    activePool.doExecute(workerThread, param);
                } catch (InterruptedException e) {
                    LOG.error("Do execute occur thread interrupted.", e);
                    // discard this execution param
                    param = null;
                    // destroy this worker thread
                    stopWorkerThread(workerThread, true);
                    throw e;
                } catch (BrokenThreadException e) {
                    LOG.error(e.getMessage());
                    // re-execute this execution param
                    taskQueue.putFirst(param);
                    // destroy this worker thread
                    stopWorkerThread(workerThread, true);
                } catch (IllegalTaskException e) {
                    LOG.error(e.getMessage());
                    // discard the execute param
                    param = null;
                    // return this worker thread
                    idlePool.putFirst(workerThread);
                } catch (DuplicateTaskException e) {
                    LOG.error(e.getMessage());
                    // cancel this execution param
                    terminateTask(supervisorCoreRpcClient, param, Operations.TRIGGER, VERIFY_FAILED, toErrorMsg(e));

                    // return this worker thread
                    idlePool.putFirst(workerThread);
                }
            }
        } catch (InterruptedException e) {
            LOG.warn("Thread pool running interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            LOG.error("Thread pool running occur error.", t);
        }

        close();
    }

    @Override
    public String toString() {
        return String.format(
            "maximum-pool-size=%d, total-count=%d, active-count=%d, idle-count=%d, task-count=%d",
            maximumPoolSize, workerThreadCounter.get(), activePool.size(), idlePool.size(), taskQueue.size()
        );
    }

    // ----------------------------------------------------------------------private methods

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
            LOG.warn("Return thread failed, because not found: {}", workerThread.getName());
            return false;
        }

        // return the detached worker thread to idle pool
        try {
            idlePool.putFirst(workerThread);
            return true;
        } catch (InterruptedException e) {
            LOG.error("Return thread to idle pool interrupted.", e);
            Thread.currentThread().interrupt();
            stopWorkerThread(workerThread, false);
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
    private void stopWorkerThread(WorkerThread workerThread, boolean doStop) {
        workerThread.toStop();
        if (workerThread.toDestroy()) {
            workerThreadCounter.decrementAndGet();
        }
        if (doStop) {
            LOG.info("Do stop the worker thread: {}", workerThread.getName());
            workerThread.doStop();
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
                WorkerThread thread = new WorkerThread(this, supervisorCoreRpcClient, keepAliveTimeSeconds);
                LOG.info("Created worker thread, current size: {}", count + 1);
                return thread;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------private static definitions

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

    private static void terminateTask(SupervisorCoreRpcService client, ExecuteTaskParam param, Operations ops, ExecuteState toState, String errorMsg) {
        Assert.notNull(ops, "Terminate task operation cannot be null.");
        if (!param.updateOperation(ops, null)) {
            // already terminated
            LOG.warn("Terminate task conflict: {} | {} | {}", param.getTaskId(), ops, toState);
            return;
        }

        TerminateTaskParam terminateTaskParam = new TerminateTaskParam(
            param.getInstanceId(), param.getWnstanceId(), param.getTaskId(), ops, toState, errorMsg
        );
        try {
            if (!client.terminateTask(terminateTaskParam)) {
                LOG.warn("Terminate task failed: {} | {} | {}", param.getTaskId(), ops, toState);
            }
        } catch (Throwable t) {
            LOG.error("Terminate task error: " + param.getTaskId() + " | " + ops + " | " + toState);
            Threads.interruptIfNecessary(t);
        }
    }

    private static void stopInstance(SupervisorCoreRpcService client, ExecuteTaskParam param, Operations ops, String errorMsg) {
        if (!param.updateOperation(Operations.TRIGGER, ops)) {
            LOG.info("Stop instance conflict: {} | {}", param, ops);
            return;
        }

        LOG.info("Stop instance task: {} | {}", param.getTaskId(), ops);
        terminateTask(client, param, ops, ops.toState(), errorMsg);

        try {
            boolean res = true;
            long lockInstanceId = Optional.ofNullable(param.getWnstanceId()).orElse(param.getInstanceId());
            if (ops == Operations.PAUSE) {
                res = client.pauseInstance(lockInstanceId);
            } else if (ops == Operations.EXCEPTION_CANCEL) {
                res = client.cancelInstance(lockInstanceId, ops);
            } else {
                LOG.error("Stop instance unsupported operation: {} | {}", param.getTaskId(), ops);
            }
            if (!res) {
                LOG.info("Stop instance conflict: {} | {} | {}", param.getInstanceId(), param.getTaskId(), ops);
            }
        } catch (Throwable t) {
            LOG.error("Stop instance error: " + param.getTaskId() + " | " + ops, t);
            Threads.interruptIfNecessary(t);
        }
    }

    /**
     * Active thread pool
     */
    private class ActiveThreadPool {
        //private final BiMap<Long, WorkerThread> activePool = Maps.synchronizedBiMap(HashBiMap.create());
        private final Map<Long, WorkerThread> pool = new HashMap<>();

        synchronized void doExecute(WorkerThread workerThread, ExecuteTaskParam param) throws InterruptedException {
            if (param == null || param.operation().isNotTrigger()) {
                // cannot happen
                throw new IllegalTaskException("Invalid execute param: " + param);
            }

            WorkerThread exists = pool.get(param.getTaskId());
            if (exists != null) {
                ExecuteTaskParam p = exists.executingParam();
                if (param.equals(p)) {
                    // 同一个task re-dispatch，导致重复
                    throw new IllegalTaskException("Repeat execute task: " + param);
                } else {
                    // 如果task分表时，不同task分表的task-id会有重复的可能性(task不做分片表时不会存在该问题)
                    throw new DuplicateTaskException("Duplicate task id: " + param + " | " + p);
                }
            }

            if (!workerThread.updateExecuteParam(null, param)) {
                ExecuteTaskParam p = workerThread.executingParam();
                throw new BrokenThreadException("Execute worker thread conflict: " + workerThread.getName() + " | " + param + " | " + p);
            }

            try {
                workerThread.execute(param);
            } catch (Throwable t) {
                workerThread.updateExecuteParam(param, null);
                throw t;
            }
            pool.put(param.getTaskId(), workerThread);
        }

        synchronized Pair<WorkerThread, ExecuteTaskParam> stopTask(long taskId, Operations ops) {
            WorkerThread thread = pool.get(taskId);
            ExecuteTaskParam param;
            if (thread == null || (param = thread.executingParam()) == null) {
                return null;
            }

            if (!param.updateOperation(Operations.TRIGGER, ops)) {
                return null;
            }

            if (!thread.updateExecuteParam(param, null)) {
                // cannot happen
                LOG.error("Stop task clear execute param failed: {}", param);
                return null;
            }
            pool.remove(taskId);
            LOG.info("Removed active pool worker thread: {} | {}", thread.getName(), param.getTaskId());
            return Pair.of(thread, param);
        }

        synchronized ExecuteTaskParam removeThread(WorkerThread workerThread) {
            ExecuteTaskParam param = workerThread.executingParam();
            if (param == null) {
                return null;
            }

            if (!workerThread.updateExecuteParam(param, null)) {
                // cannot happen
                LOG.error("Remove thread clear execute param failed: {}", param);
                return null;
            }
            WorkerThread removed = pool.remove(param.getTaskId());

            // cannot happen
            Assert.isTrue(
                workerThread == removed,
                () -> "Inconsistent worker thread: " + param.getTaskId() + ", " + workerThread.getName() + ", " + removed.getName()
            );
            return param;
        }

        synchronized void stopPool() {
            pool.forEach((id, workerThread) -> {
                workerThread.toStop();
                ExecuteTaskParam param = workerThread.executingParam();
                if (param != null) {
                    param.stop();
                }
            });
        }

        synchronized void closePool() {
            pool.entrySet()
                .parallelStream()
                .forEach(entry -> {
                    WorkerThread workerThread = entry.getValue();
                    ExecuteTaskParam param = workerThread.executingParam();
                    Operations ops = Operations.PAUSE;

                    // 1、first change the execution param operation
                    boolean success = (param == null) || param.updateOperation(Operations.TRIGGER, ops);

                    // 2、then stop the work thread
                    try {
                        WorkerThreadPool.this.stopWorkerThread(workerThread, true);
                    } catch (Throwable t) {
                        LOG.error("Stop worker thread occur error on thread pool close: " + param + " | " + workerThread, t);
                    }

                    // 3、finally update the sched task state
                    if (success) {
                        terminateTask(supervisorCoreRpcClient, param, ops, ops.toState(), null);
                    } else {
                        LOG.warn("Change execution param ops failed on thread pool close: {} | {}", param, ops);
                    }

                    workerThread.updateExecuteParam(param, null);
                });

            pool.clear();
        }

        int size() {
            return pool.size();
        }
    }

    private static class IllegalTaskException extends RuntimeException {
        private static final long serialVersionUID = -1273937229826200274L;

        public IllegalTaskException(String message) {
            super(message);
        }
    }

    private static class DuplicateTaskException extends RuntimeException {
        private static final long serialVersionUID = -5210570253941551115L;

        public DuplicateTaskException(String message) {
            super(message);
        }
    }

    private static class BrokenThreadException extends RuntimeException {
        private static final long serialVersionUID = 3475868254991118684L;

        public BrokenThreadException(String message) {
            super(message);
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
         * Supervisor core rpc client
         */
        private final SupervisorCoreRpcService supervisorCoreRpcClient;

        /**
         * Thread keep alive time
         */
        private final long keepAliveTime;

        /**
         * Work queue
         */
        private final BlockingQueue<ExecuteTaskParam> workQueue = new SynchronousQueue<>();

        /**
         * Thread is whether stopped status
         */
        private volatile boolean stopped = false;

        /**
         * Thread is whether destroyed
         */
        private final AtomicBoolean destroyed = new AtomicBoolean(false);

        /**
         * Atomic reference object of executing param
         */
        private final AtomicReference<ExecuteTaskParam> executingParam = new AtomicReference<>();

        private WorkerThread(WorkerThreadPool threadPool,
                             SupervisorCoreRpcService supervisorCoreRpcClient,
                             long keepAliveTimeSeconds) {
            this.threadPool = threadPool;
            this.supervisorCoreRpcClient = supervisorCoreRpcClient;
            this.keepAliveTime = TimeUnit.SECONDS.toNanos(keepAliveTimeSeconds);

            super.setDaemon(true);
            super.setName(getClass().getSimpleName() + "-" + NAMED_SEQ.getAndIncrement());
            this.setUncaughtExceptionHandler(LoggedUncaughtExceptionHandler.INSTANCE);
            super.start();
        }

        private void execute(ExecuteTaskParam param) throws InterruptedException {
            if (stopped || isStopped()) {
                throw new BrokenThreadException("Worker thread already stopped: " + super.getName());
            }
            if (!workQueue.offer(param, 1000, TimeUnit.MILLISECONDS)) {
                throw new BrokenThreadException("Put to worker thread queue timeout: " + super.getName());
            }
        }

        private void toStop() {
            stopped = true;
        }

        private void doStop() {
            toStop();
            ExecuteTaskParam param = executingParam();
            if (param != null) {
                param.stop();
            }
            Threads.stopThread(this, 2000);
        }

        private boolean updateExecuteParam(ExecuteTaskParam expect, ExecuteTaskParam update) {
            return executingParam.compareAndSet(expect, update);
        }

        private boolean toDestroy() {
            return destroyed.compareAndSet(false, true);
        }

        public final ExecuteTaskParam executingParam() {
            return executingParam.get();
        }

        public final boolean isExecuting() {
            return executingParam() != null;
        }

        public final boolean isStopped() {
            return Threads.isStopped(this);
        }

        @Override
        public void run() {
            while (!stopped) {
                if (super.isInterrupted()) {
                    LOG.warn("Worker thread run interrupted.");
                    break;
                }

                ExecuteTaskParam param;
                try {
                    param = workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    LOG.warn("Poll execution param block interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }

                if (param == null) {
                    if (executingParam.get() != null) {
                        param = workQueue.poll();
                    }
                    if (param == null) {
                        LOG.info("Worker thread exit, idle wait timeout.");
                        break;
                    }
                }

                try {
                    runTask(param);
                } catch (Throwable t) {
                    LOG.error("Worker thread execute failed: " + param, t);
                    terminateTask(supervisorCoreRpcClient, param, Operations.TRIGGER, EXECUTE_EXCEPTION, toErrorMsg(t));
                }

                // return this to idle thread pool
                if (!threadPool.returnWorkerThread(this)) {
                    return;
                }
            }

            threadPool.removeWorkerThread(this);
        }

        private void runTask(ExecuteTaskParam param) {
            SchedTask task;
            ExecutingTask executingTask;
            try {
                if ((task = supervisorCoreRpcClient.getTask(param.getTaskId())) == null) {
                    LOG.error("Sched task not found {}", param);
                    return;
                }

                ExecuteState fromState = ExecuteState.of(task.getExecuteState());
                if (fromState != WAITING) {
                    LOG.warn("Task state not executable: {} | {} | {}", task.getTaskId(), fromState, param.operation());
                    return;
                }

                // build executing task
                List<WorkflowPredecessorNode> nodes = null;
                if (param.getJobType() == JobType.WORKFLOW) {
                    nodes = supervisorCoreRpcClient.findWorkflowPredecessorNodes(param.getWnstanceId(), param.getInstanceId());
                }
                executingTask = ExecutingTask.of(param.getJobId(), param.getWnstanceId(), task, nodes);

                // update database records start state(sched_instance, sched_task)
                if (!supervisorCoreRpcClient.startTask(StartTaskParam.from(param))) {
                    LOG.warn("Task start conflicted {}", param);
                    return;
                }
            } catch (Throwable t) {
                LOG.warn("Start task fail: " + param, t);
                if (param.getRouteStrategy() != RouteStrategy.BROADCAST) {
                    // reset task worker
                    final List<TaskWorkerParam> list = Collections.singletonList(new TaskWorkerParam(param.getTaskId(), ""));
                    ThrowingRunnable.execute(() -> supervisorCoreRpcClient.updateTaskWorker(list), () -> "Reset task worker occur error: " + param);
                }
                Threads.interruptIfNecessary(t);
                // discard task
                return;
            }

            // 1、prepare
            TaskExecutor<?> taskExecutor;
            try {
                taskExecutor = JobHandlerUtils.load(param.getJobHandler());
                param.taskExecutor(taskExecutor);
            } catch (Throwable t) {
                LOG.error("Load job handler error: " + param, t);
                terminateTask(supervisorCoreRpcClient, param, Operations.TRIGGER, INSTANCE_FAILED, toErrorMsg(t));
                return;
            }

            // 2、init
            try {
                taskExecutor.init(executingTask);
                LOG.info("Initiated sched task {}", param.getTaskId());
            } catch (Throwable t) {
                LOG.error("Task init error: " + param, t);
                terminateTask(supervisorCoreRpcClient, param, Operations.TRIGGER, INIT_EXCEPTION, toErrorMsg(t));
                Threads.interruptIfNecessary(t);
                return;
            }

            // 3、execute
            try {
                Result<?> result;
                if (param.getExecuteTimeout() > 0) {
                    FutureTask<Result<?>> futureTask = new FutureTask<>(() -> taskExecutor.execute(executingTask, supervisorCoreRpcClient));
                    String threadName = getClass().getSimpleName() + "#FutureTaskThread" + "-" + FUTURE_TASK_NAMED_SEQ.getAndIncrement();
                    Thread futureTaskThread = Threads.newThread(threadName, true, Thread.NORM_PRIORITY, futureTask);
                    futureTaskThread.start();
                    try {
                        result = futureTask.get(param.getExecuteTimeout(), TimeUnit.MILLISECONDS);
                    } finally {
                        Threads.stopThread(futureTaskThread, 0);
                    }
                } else {
                    result = taskExecutor.execute(executingTask, supervisorCoreRpcClient);
                }

                // 4、execute end
                if (result != null && result.isSuccess()) {
                    LOG.info("Task execute finished {}", param.getTaskId());
                    terminateTask(supervisorCoreRpcClient, param, Operations.TRIGGER, FINISHED, null);
                } else {
                    LOG.error("Task execute failed {} | {}", param, result);
                    String msg = (result == null) ? "null result" : result.getMsg();
                    terminateTask(supervisorCoreRpcClient, param, Operations.TRIGGER, EXECUTE_FAILED, msg);
                }
            } catch (TimeoutException e) {
                LOG.error("Task execute timeout: " + param, e);
                terminateTask(supervisorCoreRpcClient, param, Operations.TRIGGER, EXECUTE_TIMEOUT, toErrorMsg(e));
            } catch (PauseTaskException e) {
                LOG.error("Pause task exception: {} | {}", param, e.getMessage());
                stopInstance(supervisorCoreRpcClient, param, Operations.PAUSE, toErrorMsg(e));
            } catch (CancelTaskException e) {
                LOG.error("Cancel task exception:  {} | {}", param, e.getMessage());
                stopInstance(supervisorCoreRpcClient, param, Operations.EXCEPTION_CANCEL, toErrorMsg(e));
            } catch (Throwable t) {
                if (t instanceof java.lang.ThreadDeath) {
                    LOG.warn("Task execute thread death: {} | {}", param, t.getMessage());
                } else if (t instanceof InterruptedException) {
                    LOG.warn("Task executed interrupted: {} | {}", param, t.getMessage());
                } else {
                    LOG.error("Task execute occur error: " + param, t);
                }
                terminateTask(supervisorCoreRpcClient, param, Operations.TRIGGER, EXECUTE_EXCEPTION, toErrorMsg(t));
                Threads.interruptIfNecessary(t);
            } finally {
                // 5、destroy
                try {
                    taskExecutor.destroy();
                    LOG.info("Destroyed sched task: {}", param.getTaskId());
                } catch (Throwable t) {
                    LOG.error("Task destroy error: " + param, t);
                }
            } // end of try catch block
        } // end of runTask method
    } // end of worker thread class definition

}
