/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.worker.base;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.scheduler.common.concurrent.Threads;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.exception.CancelTaskException;
import cn.ponfee.scheduler.core.exception.PauseTaskException;
import cn.ponfee.scheduler.core.handle.JobHandlerUtils;
import cn.ponfee.scheduler.core.handle.TaskExecutor;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static cn.ponfee.scheduler.core.enums.ExecuteState.*;

/**
 * Thread pool of execute task, also is a boss thread
 *
 * @author Ponfee
 */
public class WorkerThreadPool extends Thread implements AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(WorkerThreadPool.class);
    private final static int ERROR_MSG_MAX_LENGTH = 2048;

    /**
     * This jdk thread pool for asynchronous to stop(pause or cancel) task
     */
    private static final ThreadPoolExecutor STOP_TASK_POOL = ThreadPoolExecutors.create(
        1, 10, 300, 50, ThreadPoolExecutors.ALWAYS_CALLER_RUNS
    );

    /**
     * Supervisor client
     */
    private final SupervisorService supervisorClient;

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
    private final LinkedBlockingDeque<ExecuteParam> taskQueue = new LinkedBlockingDeque<>();

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
                            SupervisorService supervisorClient) {
        Assert.isTrue(maximumPoolSize > 0, "Maximum pool size must be positive number.");
        Assert.isTrue(keepAliveTimeSeconds > 0, "Keep alive time seconds must be positive number.");
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
        this.supervisorClient = supervisorClient;

        super.setDaemon(true);
        super.setName(getClass().getSimpleName());
    }

    /**
     * Submit task execution param to thread pool
     *
     * @param param the execution param
     * @return {@code true} if thread pool accepted
     */
    public boolean submit(ExecuteParam param) {
        if (closed.get()) {
            return false;
        }

        if (param.operation() == Operations.TRIGGER) {
            return taskQueue.offerLast(param);
        } else {
            STOP_TASK_POOL.execute(() -> stop(param));
            return true;
        }
    }

    /**
     * Stop(Pause or Cancel) specified task
     *
     * @param stopParam the stops task param
     */
    private void stop(ExecuteParam stopParam) {
        Operations ops = stopParam.operation();
        Assert.isTrue(ops != null && ops != Operations.TRIGGER, () -> "Invalid stop operation: " + ops);

        if (closed.get()) {
            return;
        }

        long taskId = stopParam.getTaskId();
        Pair<WorkerThread, ExecuteParam> pair = activePool.stopTask(taskId, ops);
        if (pair == null) {
            LOG.warn("Not found stoppable task {} | {}", taskId, ops);
            try {
                terminateTask(supervisorClient, stopParam, ops, ops.targetState(), null);
            } catch (Exception e) {
                LOG.error("Abort stopped task occur error: {} | {}", taskId, ops);
            }
            return;
        }

        WorkerThread workerThread = pair.getLeft();
        ExecuteParam param = pair.getRight();
        LOG.info("Stop task: {} | {} | {}", taskId, ops, workerThread.getName());
        try {
            param.interrupt();
            // stop the work thread
            stopWorkerThread(workerThread, true);
        } finally {
            try {
                terminateTask(supervisorClient, param, ops, ops.targetState(), null);
            } catch (Exception e) {
                LOG.error("Normal stop task occur error: {} | {} | {}", taskId, ops, workerThread.getName());
                Threads.interruptIfNecessary(e);
            }
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            LOG.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        LOG.info("Close worker thread pool start...");

        // 1、prepare close
        // 1.1、change executing pool thread state
        Throwables.caught(activePool::stopPool);

        // 1.2、change idle pool thread state
        idlePool.forEach(e -> Throwables.caught(e::toStop));

        // 1.3、clear task execution param queue
        Throwables.caught(taskQueue::clear);

        // 2、do close
        // 2.1、stop this boss thread
        Throwables.caught(() -> Threads.stopThread(this, 0, 0, 200));

        // 2.2、stop idle pool thread
        idlePool.forEach(e -> Throwables.caught(() -> stopWorkerThread(e, true)));
        Throwables.caught(idlePool::clear);

        // 2.3、stop executing pool thread
        Throwables.caught(activePool::closePool);
        workerThreadCounter.set(0);

        // 2.4、shutdown jdk thread pool
        Throwables.caught(() -> ThreadPoolExecutors.shutdown(STOP_TASK_POOL, 1));

        LOG.info("Close worker thread pool end.");
    }

    @Override
    public void run() {
        try {
            while (!closed.get()) {
                ExecuteParam param = taskQueue.takeFirst();

                // take a worker
                WorkerThread workerThread = idlePool.pollFirst();
                if (workerThread == null) {
                    workerThread = createWorkerThreadIfNecessary();
                }
                if (workerThread == null) {
                    workerThread = idlePool.takeFirst();
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
                    LOG.error("Do execute broken thread.", e);
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
                    try {
                        // cancel this execution param
                        terminateTask(supervisorClient, param, Operations.TRIGGER, VERIFY_FAILED, toErrorMsg(e));
                    } catch (Exception ex) {
                        LOG.error("Cancel duplicate task occur error: " + param, ex);
                    }
                    // return this worker thread
                    idlePool.putFirst(workerThread);
                }
            }
        } catch (InterruptedException e) {
            LOG.error("Thread pool running interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("Thread pool running occur error.", e);
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
     * Stop and discard the worker thread(death)
     *
     * @param workerThread the worker thread
     * @param doStop       if whether do stop the thread
     */
    private void stopWorkerThread(WorkerThread workerThread, boolean doStop) {
        workerThreadCounter.decrementAndGet();
        if (doStop) {
            LOG.info("Worker thread death: {}", workerThread.getName());
            workerThread.doStop(0, 0, 200);
        } else {
            workerThread.toStop();
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
                WorkerThread thread = new WorkerThread(this, supervisorClient, keepAliveTimeSeconds);
                LOG.info("Created worker thread, current size: {}", count + 1);
                return thread;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------private static definitions

    private static String toErrorMsg(Exception e) {
        if (e == null) {
            return null;
        }
        String errorMsg = Throwables.getRootCauseStackTrace(e);
        if (errorMsg.length() > ERROR_MSG_MAX_LENGTH) {
            errorMsg = errorMsg.substring(0, ERROR_MSG_MAX_LENGTH);
        }
        return errorMsg;
    }

    private static void terminateTask(SupervisorService supervisorClient, ExecuteParam param,
                                      Operations ops, ExecuteState toState, String errorMsg) throws Exception {
        Assert.notNull(ops, "Terminate task current operation cannot null.");
        if (param.operation() == null) {
            // already terminated
            return;
        }

        // clear execute param operation
        if (!param.updateOperation(ops, null)) {
            LOG.warn("Clear execution param operation conflict: {} | {} | {}", param, ops, toState);
            return;
        }

        boolean success;
        switch (ops) {
            case TRIGGER:
                success = supervisorClient.terminateExecutingTask(param, toState, errorMsg);
                break;
            case PAUSE:
                success = supervisorClient.pauseExecutingTask(param, errorMsg);
                break;
            default:
                success = supervisorClient.cancelExecutingTask(param, toState, errorMsg);
                break;
        }
        if (!success) {
            LOG.error("Terminate sched task failed: {} | {} | {} | {}", param, ops, toState, errorMsg);
        }
    }

    private static void terminateInstance(SupervisorService supervisorClient, ExecuteParam param,
                                          Operations ops, String errorMsg) throws Exception {
        if (!param.updateOperation(Operations.TRIGGER, ops)) {
            LOG.error("Terminate sched instance conflicted: {} | {} | {}", param, ops, errorMsg);
            return;
        }

        LOG.info("Terminate the sched instance {}", param);

        try {
            terminateTask(supervisorClient, param, ops, ops.targetState(), errorMsg);
        } catch (Exception e) {
            LOG.error("Terminate sched instance task error: " + param, e);
        }

        boolean success;
        switch (ops) {
            case PAUSE:
                success = supervisorClient.pauseInstance(param.getInstanceId());
                break;
            case EXCEPTION_CANCEL:
                success = supervisorClient.cancelInstance(param.getInstanceId(), ops);
                break;
            default:
                throw new UnsupportedOperationException("Terminate sched instance unsupported operation: " + ops);
        }
        if (!success) {
            LOG.error("Terminate sched instance failed: {} | {} | {}", param, ops, errorMsg);
        }
    }

    /**
     * Active thread pool
     */
    private class ActiveThreadPool {
        //private final BiMap<Long, WorkerThread> activePool = Maps.synchronizedBiMap(HashBiMap.create());
        private final Map<Long, WorkerThread> pool = new HashMap<>();

        synchronized void doExecute(WorkerThread workerThread, ExecuteParam param) throws InterruptedException {
            if (param == null || param.operation() != Operations.TRIGGER) {
                // cannot happen
                throw new IllegalTaskException("Invalid execute param: " + param);
            }

            WorkerThread exists = pool.get(param.getTaskId());
            if (exists != null) {
                throw param.same(exists.executingParam())
                    ? new IllegalTaskException("Task repeat execute: " + param)
                    : new DuplicateTaskException("Task id duplicate: " + param + ", " + exists.executingParam());
            }

            if (!workerThread.updateExecuteParam(null, param)) {
                throw new BrokenThreadException("Execute worker thread conflict: " + workerThread.getName() + ", " + workerThread.executingParam());
            }

            try {
                workerThread.execute(param);
            } catch (Exception e) {
                workerThread.updateExecuteParam(param, null);
                throw e;
            }
            pool.put(param.getTaskId(), workerThread);
        }

        synchronized Pair<WorkerThread, ExecuteParam> stopTask(long taskId, Operations ops) {
            WorkerThread thread = pool.get(taskId);
            ExecuteParam param;
            if (thread == null || (param = thread.executingParam()) == null) {
                return null;
            }

            if (!param.updateOperation(Operations.TRIGGER, ops)) {
                return null;
            }

            thread.updateExecuteParam(param, null);
            pool.remove(taskId);
            LOG.info("Removed active pool worker thread: {} | {}", taskId, thread.getName());
            return Pair.of(thread, param);
        }

        synchronized ExecuteParam removeThread(WorkerThread workerThread) {
            ExecuteParam param = workerThread.executingParam();
            if (param == null) {
                return null;
            }

            workerThread.updateExecuteParam(param, null);
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
                workerThread.executingParam().interrupt();
            });
        }

        synchronized void closePool() {
            pool.entrySet()
                .parallelStream()
                .forEach(entry -> {
                    WorkerThread workerThread = entry.getValue();
                    ExecuteParam param = workerThread.executingParam();
                    Operations ops = Operations.PAUSE;

                    // 1、first change the execution param operation
                    boolean success = param.updateOperation(Operations.TRIGGER, ops);

                    // 2、then stop the work thread
                    try {
                        WorkerThreadPool.this.stopWorkerThread(workerThread, true);
                    } catch (Exception e) {
                        LOG.error("Stop worker thread occur error on thread pool close: " + param + " | " + workerThread, e);
                    }

                    // 3、finally update the sched task state
                    if (success) {
                        try {
                            terminateTask(supervisorClient, param, ops, ops.targetState(), null);
                        } catch (Exception e) {
                            LOG.error("Terminate task failed on thread pool close: " + param, e);
                        }
                    } else {
                        LOG.error("Change execution param ops failed on thread pool close: {} | {}", param, ops);
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
         * Supervisor client
         */
        private final SupervisorService supervisorClient;

        /**
         * Thread keep alive time
         */
        private final long keepAliveTime;

        /**
         * Work queue
         */
        private final BlockingQueue<ExecuteParam> workQueue = new SynchronousQueue<>();

        /**
         * Thread is whether stopped status
         */
        private final AtomicBoolean stopped = new AtomicBoolean(false);

        /**
         * Atomic reference object of executing param
         */
        private final AtomicReference<ExecuteParam> executingParam = new AtomicReference<>();

        public WorkerThread(WorkerThreadPool threadPool,
                            SupervisorService supervisorClient,
                            long keepAliveTimeSeconds) {
            this.threadPool = threadPool;
            this.supervisorClient = supervisorClient;
            this.keepAliveTime = TimeUnit.SECONDS.toNanos(keepAliveTimeSeconds);

            super.setDaemon(true);
            super.setName(getClass().getSimpleName() + "-" + NAMED_SEQ.getAndIncrement());
            super.start();
        }

        public final void execute(ExecuteParam param) throws InterruptedException {
            if (stopped.get() || isStopped()) {
                throw new BrokenThreadException("Worker thread already stopped: " + super.getName());
            }
            if (!workQueue.offer(param, 100, TimeUnit.MILLISECONDS)) {
                throw new BrokenThreadException("Put to worker thread queue timeout: " + super.getName());
            }
        }

        public final void toStop() {
            stopped.compareAndSet(false, true);
        }

        public final boolean doStop(int sleepCount, long sleepMillis, long joinMillis) {
            toStop();
            if (!stopped.compareAndSet(false, true)) {
                LOG.error("Repeat do stop worker thread: {}", super.getName());
                return false;
            }

            return Threads.stopThread(this, sleepCount, sleepMillis, joinMillis);
        }

        public final boolean updateExecuteParam(ExecuteParam expect, ExecuteParam update) {
            return executingParam.compareAndSet(expect, update);
        }

        public final ExecuteParam executingParam() {
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
            while (!stopped.get()) {
                if (super.isInterrupted()) {
                    LOG.warn("Worker boss thread interrupted.");
                    threadPool.removeWorkerThread(this);
                    return;
                }

                ExecuteParam executeParam;
                try {
                    executeParam = workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    LOG.error("Poll execution param block interrupted.", e);
                    threadPool.removeWorkerThread(this);
                    Thread.currentThread().interrupt();
                    return;
                }

                if (executeParam == null) {
                    LOG.info("Worker thread exit, idle wait timeout.");
                    threadPool.removeWorkerThread(this);
                    return;
                }

                try {
                    runTask(executeParam);
                    // Thread#stop() will occur "java.lang.ThreadDeath: null" if wrapped in Throwable
                } catch (Exception e) {
                    try {
                        terminateTask(supervisorClient, executeParam, Operations.TRIGGER, EXECUTE_EXCEPTION, toErrorMsg(e));
                    } catch (Exception ex) {
                        LOG.error("Worker thread terminate failed: " + executeParam, ex);
                    }

                    if (e instanceof InterruptedException) {
                        LOG.error("Worker thread execute interrupted: " + executeParam, e);
                        threadPool.removeWorkerThread(this);
                        Thread.currentThread().interrupt();
                        return;
                    } else {
                        LOG.error("Worker thread execute failed: " + executeParam, e);
                    }
                }

                // return this to idle thread pool
                if (!threadPool.returnWorkerThread(this)) {
                    return;
                }
            }

            threadPool.removeWorkerThread(this);
        }

        private void runTask(ExecuteParam param) throws Exception {
            SchedTask task;
            SchedJob schedJob;
            try {
                if ((task = supervisorClient.getTask(param.getTaskId())) == null) {
                    LOG.error("Sched task not found {}", param);
                    return;
                }

                ExecuteState fromState = ExecuteState.of(task.getExecuteState());
                if (fromState != WAITING) {
                    LOG.warn("Task state not executable: {} | {} | {}", task.getTaskId(), fromState, param.operation());
                    return;
                }

                if ((schedJob = supervisorClient.getJob(param.getJobId())) == null) {
                    LOG.error("Sched job not found {}", param);
                    return;
                }

                // update database records start state(sched_instance, sched_task)
                boolean status = supervisorClient.startTask(param);
                if (!status) {
                    LOG.warn("Task start conflicted {}", param);
                    return;
                }
            } catch (Exception e) {
                LOG.warn("Start task fail: " + param, e);
                try {
                    // reset sched_task.worker
                    supervisorClient.updateTaskWorker(Collections.singletonList(param.getTaskId()), "");
                } catch (Exception ex) {
                    // here need to manual handle
                    LOG.error("Reset task worker occur error: " + param, ex);
                }
                return; // discard task
            }

            // 1、prepare
            TaskExecutor<?> taskExecutor;
            try {
                taskExecutor = JobHandlerUtils.newInstance(schedJob.getJobHandler());
            } catch (Exception e) {
                LOG.error("Load job handler error: " + param, e);
                terminateTask(supervisorClient, param, Operations.TRIGGER, INSTANCE_FAILED, toErrorMsg(e));
                return;
            }

            taskExecutor.task(task);
            param.taskExecutor(taskExecutor);

            try {
                taskExecutor.verify();
            } catch (Exception e) {
                LOG.error("Task verify failed: " + param, e);
                terminateTask(supervisorClient, param, Operations.TRIGGER, VERIFY_FAILED, toErrorMsg(e));
                return;
            }

            // 2、init
            try {
                taskExecutor.init();
                LOG.info("Initiated sched task {}", param);
            } catch (Exception e) {
                LOG.error("Task init error: " + param, e);
                terminateTask(supervisorClient, param, Operations.TRIGGER, INIT_EXCEPTION, toErrorMsg(e));
                return;
            }

            // 3、execute
            try {
                Result<?> result;
                if (schedJob.getExecuteTimeout() > 0) {
                    FutureTask<Result<?>> futureTask = new FutureTask<>(() -> taskExecutor.execute(supervisorClient));
                    Thread futureTaskThread = new Thread(futureTask);
                    futureTaskThread.setDaemon(true);
                    futureTaskThread.setName(getClass().getSimpleName() + "#FutureTaskThread" + "-" + FUTURE_TASK_NAMED_SEQ.getAndIncrement());
                    futureTaskThread.start();
                    try {
                        result = futureTask.get(schedJob.getExecuteTimeout(), TimeUnit.MILLISECONDS);
                    } finally {
                        Threads.stopThread(futureTaskThread, 0, 0, 0);
                    }
                } else {
                    result = taskExecutor.execute(supervisorClient);
                }
                LOG.info("Executed sched task {}", param);

                // 4、execute end
                if (result.isSuccess()) {
                    LOG.info("Task executed finished {}", param);
                    terminateTask(supervisorClient, param, Operations.TRIGGER, FINISHED, null);
                } else {
                    LOG.error("Task executed failed {} | {}", param, result);
                    terminateTask(supervisorClient, param, Operations.TRIGGER, EXECUTE_FAILED, result.getMsg());
                }
            } catch (TimeoutException e) {
                LOG.error("Task execute timeout: " + param, e);
                terminateTask(supervisorClient, param, Operations.TRIGGER, EXECUTE_TIMEOUT, toErrorMsg(e));
            } catch (PauseTaskException e) {
                LOG.error("PauseTaskException, do pause: " + param, e);
                terminateInstance(supervisorClient, param, Operations.PAUSE, toErrorMsg(e));
            } catch (CancelTaskException e) {
                LOG.error("CancelTaskException, do manual cancel: " + param, e);
                terminateInstance(supervisorClient, param, Operations.EXCEPTION_CANCEL, toErrorMsg(e));
            } catch (Exception e) {
                LOG.error("Task execute occur error: " + param, e);
                terminateTask(supervisorClient, param, Operations.TRIGGER, EXECUTE_EXCEPTION, toErrorMsg(e));
            } finally {
                // 5、destroy
                try {
                    taskExecutor.destroy();
                    LOG.info("Destroyed sched task: {}", param);
                } catch (Exception e) {
                    LOG.error("Task destroy error: " + param, e);
                }
            } // end of try catch block
        } // end of runTask method
    } // end of worker thread class definition

}
