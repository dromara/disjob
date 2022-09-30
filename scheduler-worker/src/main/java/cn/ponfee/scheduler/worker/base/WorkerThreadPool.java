package cn.ponfee.scheduler.worker.base;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.concurrent.MultithreadExecutors;
import cn.ponfee.scheduler.common.concurrent.ThreadPoolExecutors;
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

import java.util.HashMap;
import java.util.Iterator;
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
     * This jdk thread pool for asnyc suspend(pause or cancel) task
     */
    private static final ThreadPoolExecutor SUSPEND_TASK_POOL = ThreadPoolExecutors.create(
        1, 10, 300, 50, ThreadPoolExecutors.ALWAYS_CALLER_RUNS
    );

    /**
     * Supervisor service
     */
    private final SupervisorService supervisorService;

    /**
     * Maximum pool size
     */
    private final int maximumPoolSize;

    /**
     * Worker thread keep alive time seconds
     */
    private final long keepAliveTimeSeconds;

    /**
     * Active thread pool
     */
    private final ActiveThreadPool                 activePool = new ActiveThreadPool();

    /**
     * Idle thread pool
     */
    private final LinkedBlockingDeque<WorkerThread>  idlePool = new LinkedBlockingDeque<>();

    /**
     * Task execution param queue
     */
    private final LinkedBlockingDeque<ExecuteParam> taskQueue = new LinkedBlockingDeque<>();

    /**
     * Counts thread number
     */
    private final AtomicInteger threadCounter = new AtomicInteger(0);

    /**
     * Close pool operation
     */
    private final AtomicBoolean close = new AtomicBoolean(false);

    /**
     * Closed state
     */
    private volatile boolean closed = false;

    public WorkerThreadPool(int maximumPoolSize,
                            long keepAliveTimeSeconds,
                            SupervisorService supervisorService) {
        Assert.isTrue(maximumPoolSize > 0, "Maximum pool size must be positive number.");
        Assert.isTrue(keepAliveTimeSeconds > 0, "Keep alive time seconds must be positive number.");
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
        this.supervisorService = supervisorService;

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
        if (closed) {
            return false;
        }

        Operations toOps = param.operation();
        switch (toOps) {
            case TRIGGER:
                return taskQueue.offerLast(param);
            default:
                SUSPEND_TASK_POOL.execute(() -> suspend(param.getTaskId(), toOps));
                return false;
        }
    }

    /**
     * Suspend(Pause or Cancel) specified task
     *
     * @param taskId the task id
     * @param toOps  the target operations
     */
    public void suspend(long taskId, Operations toOps) {
        Assert.isTrue(toOps != null && toOps != Operations.TRIGGER, "Invalid suspend operation: " + toOps);

        if (closed) {
            return;
        }

        Pair<WorkerThread, ExecuteParam> pair = activePool.suspendTask(taskId, toOps);
        if (pair == null) {
            LOG.warn("Not found suspendable task {} - {}", taskId, toOps);
            return;
        }

        WorkerThread thread = pair.getLeft();
        ExecuteParam param = pair.getRight();
        LOG.info("Suspend task: {} - {} - {}", taskId, toOps, thread.getName());
        try {
            param.interrupt();
            // stop the work thread
            stopWorkerThread(thread, true);
        } finally {
            try {
                terminateTask(supervisorService, param, toOps);
            } catch (Exception e) {
                LOG.error("Terminate task occur error: {} - {} - {}", taskId, toOps, thread.getName());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        if (!close.compareAndSet(false, true)) {
            LOG.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        LOG.info("Close worker thread pool start.");

        // 1、prepare close
        // 1.1、change executing pool thread state
        activePool.stopPool();

        // 1.2、change idle pool thread state
        idlePool.forEach(WorkerThread::toStop);

        // 1.3、clear task execution param queue
        taskQueue.clear();

        // 2、do close
        // 2.1、stop this boss thread
        MultithreadExecutors.stopThread(this, 10, 100, 1000);

        // 2.2、stop idle pool thread
        idlePool.forEach(e -> stopWorkerThread(e, true));
        idlePool.clear();

        // 2.3、stop executing pool thread
        activePool.closePool();
        threadCounter.set(0);

        // 2.4、shutdown jdk thread pool
        ThreadPoolExecutors.shutdown(SUSPEND_TASK_POOL, 3);

        LOG.info("Close worker thread pool end.");
    }

    @Override
    protected void finalize() {
        close();
    }

    @Override
    public void run() {
        try {
            while (!closed) {
                ExecuteParam param = taskQueue.takeFirst();

                // take a worker
                WorkerThread thread = idlePool.pollFirst();
                if (thread == null) {
                    thread = createWorkerThreadIfNecessary();
                }
                if (thread == null) {
                    thread = idlePool.takeFirst();
                }

                if (thread.isStopped()) {
                    LOG.info("Worker thread already stopped.");
                    // re-execute this execution param
                    taskQueue.putFirst(param);
                    // destroy this worker thread
                    stopWorkerThread(thread, true);
                    continue;
                }

                try {
                    activePool.doExecute(thread, param);
                } catch (InterruptedException e) {
                    LOG.error("Do execute occur thread interrupted.", e);
                    // discard this execution param
                    param = null;
                    // destroy this worker thread
                    stopWorkerThread(thread, true);
                    throw e;
                } catch (BrokenThreadException e) {
                    LOG.error("Do execute broken thread.", e);
                    // re-execute this execution param
                    taskQueue.putFirst(param);
                    // destroy this worker thread
                    stopWorkerThread(thread, true);
                } catch (IllegalTaskException e) {
                    LOG.error(e.getMessage());
                    // discard this execute param
                    param = null;
                    // return this worker thread
                    idlePool.putFirst(thread);
                } catch (DuplicateTaskException e) {
                    LOG.error(e.getMessage());
                    try {
                        // cancel this execution param
                        terminateTask(supervisorService, param, VERIFY_FAILED, toErrorMsg(e));
                    } catch (Exception ex) {
                        LOG.error("Cancel duplicate task occur error: " + param, ex);
                    }
                    // return this worker thread
                    idlePool.putFirst(thread);
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
            maximumPoolSize, threadCounter.get(), activePool.size(), idlePool.size(), taskQueue.size()
        );
    }

    // ----------------------------------------------------------------------private methods

    /**
     * Task executed finished, then return the worker thread to idle pool.
     * <p>Called this method current thread is WorkerThread
     * 
     * @param thread the worker thread
     * @return {@code true} if return to idle pool successfully
     */
    private boolean returnWorkerThread(WorkerThread thread) {
        if (activePool.removeThread(thread) == null) {
            LOG.warn("Return thread failed, because not found: {}", thread.getName());
            return false;
        }

        // return to idle pool
        try {
            idlePool.putFirst(thread);
            return true;
        } catch (InterruptedException e) {
            LOG.error("Return thread to idle pool interrupted.", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Remove the worker thread from active pool and destroy it.
     *
     * @param thread the worker thread
     */
    private void removeWorkerThread(WorkerThread thread) {
        thread.toStop();

        boolean hasRemoved = activePool.removeThread(thread) != null;

        if (!hasRemoved) {
            for (Iterator<WorkerThread> iter = idlePool.iterator(); iter.hasNext(); ) {
                if (iter.next() == thread) {
                    iter.remove();
                    hasRemoved = true;
                    break;
                }
            }
        }

        if (!hasRemoved) {
            LOG.warn("Not found removable thread: {}", thread.getName());
        }

        stopWorkerThread(thread, false);
    }

    /**
     * Stop and discard the worker thread(death)
     *
     * @param thread the worker
     * @param doStop if whether do stop the thread
     */
    private void stopWorkerThread(WorkerThread thread, boolean doStop) {
        threadCounter.decrementAndGet();
        if (doStop) {
            LOG.info("Worker thread death: {}", thread.getName());
            thread.doStop(10, 100, 1000);
        }
    }

    /**
     * 实际上是单线程在调用此方法，可以不加synchronized
     * <p>但"判断是否已经是最大线程数"与"创新建线程"是要合为原子操作的，所以加上synchronized是作语义上的考虑
     *
     * @return created worker thread object
     */
    private synchronized WorkerThread createWorkerThreadIfNecessary() {
        if (threadCounter.get() >= maximumPoolSize) {
            return null;
        }

        WorkerThread thread = new WorkerThread(this, supervisorService, keepAliveTimeSeconds);
        LOG.info("Created worker thread, current size: {}", threadCounter.incrementAndGet());
        return thread;
    }

    // -------------------------------------------------------------------private static definitions
    private static void pauseTask(SupervisorService supervisorService, ExecuteParam param, Operations toOps, String errorMsg) throws Exception {
        Operations fromOps = param.operation();
        if (fromOps != Operations.TRIGGER || !param.updateOperation(fromOps, toOps)) {
            return;
        }

        LOG.info("Pause the current sched task {} - {}", param.getTrackId(), param.getTaskId());
        terminateTask(supervisorService, param, toOps);
        supervisorService.updateTaskErrorMsg(param.getTaskId(), errorMsg);

        LOG.info("Pause the sched track other tasks: {}", param.getTrackId());
        supervisorService.pauseTrack(param.getTrackId());

    }

    private static void cancelTask(SupervisorService supervisorService, ExecuteParam param, Operations toOps, String errorMsg) throws Exception {
        Operations fromOps = param.operation();
        if (fromOps != Operations.TRIGGER || !param.updateOperation(fromOps, toOps)) {
            return;
        }

        LOG.info("Cancel the current sched task {} - {}", param.getTrackId(), param.getTaskId());
        terminateTask(supervisorService, param, toOps);
        supervisorService.updateTaskErrorMsg(param.getTaskId(), errorMsg);

        LOG.info("Cancel the sched track other tasks: {}", param);
        supervisorService.cancelTrack(param.getTrackId(), toOps);
    }

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

    private static boolean terminateTask(SupervisorService supervisorService, ExecuteParam param,
                                         ExecuteState toState, String errorMsg) throws Exception {
        return terminateTask(supervisorService, param, Operations.TRIGGER, toState, errorMsg);
    }

    private static boolean terminateTask(SupervisorService supervisorService, ExecuteParam param, Operations fromOps) throws Exception {
        return terminateTask(supervisorService, param, fromOps, fromOps.targetState(), null);
    }

    private static boolean terminateTask(SupervisorService supervisorService, ExecuteParam param,
                                         Operations currentOps, ExecuteState targetState,
                                         String errorMsg) throws Exception {
        if (currentOps == null || param.operation() == null || currentOps != param.operation()) {
            return false;
        }

        // update to 'null' operations
        if (!param.updateOperation(currentOps, null)) {
            LOG.warn(
                "Change execution param operation conflict: {} - {} - {} - {}", 
                param.getTaskId(), currentOps, param.operation(), targetState
            );
            return false;
        }

        LOG.info("Change execution param operation success: {} - {} - {}", param.getTaskId(), currentOps, targetState);
        switch (currentOps) {
            case TRIGGER:
                return supervisorService.terminateExecutingTask(param, targetState, errorMsg);
            case PAUSE:
                return supervisorService.pauseExecutingTask(param, errorMsg);
            default:
                return supervisorService.cancelExecutingTask(param, targetState, errorMsg);
        }
    }

    /**
     * Active thread pool
     */
    private class ActiveThreadPool {
        //private final BiMap<Long, WorkerThread> activePool = Maps.synchronizedBiMap(HashBiMap.create());
        private final Map<Long, WorkerThread> pool = new HashMap<>();

        synchronized void doExecute(WorkerThread thread, ExecuteParam param) throws InterruptedException {
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

            if (!thread.updateExecuteParam(null, param)) {
                throw new BrokenThreadException("Execute worker thread conflict: " + thread.getName() + ", " + thread.executingParam());
            }

            try {
                thread.execute(param);
            } catch (Exception e) {
                thread.updateExecuteParam(param, null);
                throw e;
            }
            pool.put(param.getTaskId(), thread);
        }

        synchronized Pair<WorkerThread, ExecuteParam> suspendTask(long taskId, Operations toOps) {
            WorkerThread thread = pool.get(taskId);
            ExecuteParam param;
            if (thread == null || (param = thread.executingParam()) == null) {
                return null;
            }

            Operations fromOps = Operations.TRIGGER;
            if (param.operation() != fromOps || !param.updateOperation(fromOps, toOps)) {
                return null;
            }

            thread.updateExecuteParam(param, null);
            pool.remove(taskId);
            LOG.info("Removed active pool worker thread: {} - {}", taskId, thread.getName());
            return Pair.of(thread, param);
        }

        synchronized ExecuteParam removeThread(WorkerThread thread) {
            ExecuteParam param = thread.executingParam();
            if (param == null) {
                return null;
            }

            thread.updateExecuteParam(param, null);
            WorkerThread removed = pool.remove(param.getTaskId());

            // cannot happen
            Assert.isTrue(thread == removed, "Inconsistent worker thread: " + param.getTaskId() + ", " + thread.getName() + ", " + removed.getName());
            return param;
        }

        synchronized void stopPool() {
            pool.forEach((id, thread) -> {
                thread.toStop();
                thread.executingParam().interrupt();
            });
        }

        synchronized void closePool() {
            pool.entrySet()
                .parallelStream()
                .forEach(entry -> {
                    WorkerThread thread = entry.getValue();
                    ExecuteParam param = thread.executingParam();
                    Operations fromOps = param.operation(), toOps = Operations.PAUSE;
                    boolean status = (fromOps == Operations.TRIGGER) && param.updateOperation(fromOps, toOps);
                    if (!status) {
                        LOG.error("Change execution param ops failed on thread pool close: {} - {} - {}", param, fromOps, toOps);
                    }
                    try {
                        // stop the work thread
                        WorkerThreadPool.this.stopWorkerThread(thread, true);
                    } catch (Exception e) {
                        LOG.error("Stop worker thread occur error on thread pool close: " + param + " - " + thread, e);
                    }
                    if (status) {
                        try {
                            terminateTask(supervisorService, param, toOps);
                        } catch (Exception e) {
                            LOG.error("Terminate task failed on thread pool close: " + param, e);
                        }
                    }

                    thread.updateExecuteParam(param, null);
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
         * Worker thread pool
         */
        private final WorkerThreadPool threadPool;

        /**
         * Supervisor client
         */
        private final SupervisorService supervisorService;

        /**
         * Thread keep alive time
         */
        private final long keepAliveTime;

        /**
         * Work queue
         */
        private final BlockingQueue<ExecuteParam> workQueue = new SynchronousQueue<>();

        /**
         * Do stop operation state
         */
        private final AtomicBoolean stop = new AtomicBoolean(false);

        /**
         * Atomic reference object of executing param
         */
        private final AtomicReference<ExecuteParam> executingParam = new AtomicReference<>();

        /**
         * State of whether stop
         */
        private volatile boolean stopped = false;

        public WorkerThread(WorkerThreadPool threadPool,
                            SupervisorService supervisorService,
                            long keepAliveTimeSeconds) {
            this.threadPool = threadPool;
            this.supervisorService = supervisorService;
            this.keepAliveTime = TimeUnit.SECONDS.toNanos(keepAliveTimeSeconds);

            super.setDaemon(true);
            super.setName(getClass().getSimpleName() + "-" + NAMED_SEQ.getAndIncrement());
            super.start();
        }

        public final void execute(ExecuteParam param) throws InterruptedException {
            if (stopped || isStopped()) {
                throw new BrokenThreadException("Worker thread already stopped: " + super.getName());
            }
            if (!workQueue.offer(param, 100, TimeUnit.MILLISECONDS)) {
                throw new BrokenThreadException("Put to worker thread queue timeout: " + super.getName());
            }
        }

        public final void toStop() {
            stopped = true;
        }

        public final boolean doStop(int sleepCount, long sleepMillis, long joinMillis) {
            toStop();
            if (!stop.compareAndSet(false, true)) {
                LOG.error("Repeat do stop worker thread: {}", super.getName());
                return false;
            }

            return MultithreadExecutors.stopThread(
                this, sleepCount, sleepMillis, joinMillis
            );
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
            return MultithreadExecutors.isStopped(this);
        }

        @Override
        public void run() {
            while (!stopped) {
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
                        terminateTask(supervisorService, executeParam, EXECUTE_EXCEPTION, toErrorMsg(e));
                    } catch (Exception ex) {
                        LOG.error("Worker thread terminate failed: " + executeParam, ex);
                    }

                    if (e instanceof InterruptedException) {
                        LOG.error("Worker thread execute interrupted: " + executeParam, e);
                        threadPool.removeWorkerThread(this);
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
            SchedTask task = supervisorService.getTask(param.getTaskId());
            if (task == null) {
                LOG.error("Sched task not found {}", param);
                return;
            }

            ExecuteState fromState = ExecuteState.of(task.getExecuteState());
            if (fromState != WAITING) {
                LOG.warn("Task state not executable: {} - {} - {}", task.getTaskId(), fromState, param.operation());
                return;
            }

            SchedJob schedJob = supervisorService.getJob(param.getJobId());
            if (schedJob == null) {
                LOG.error("Sched job not found {}", param);
                return;
            }

            try {
                // update database records start state(sched_track, sched_task)
                boolean status = supervisorService.startTask(param);
                if (!status) {
                    LOG.warn("Task start conflicted {}", param);
                    return;
                }
            } catch (Exception e) {
                LOG.warn("Task start fail.", e);
                return;
            }

            // 1、prepare
            TaskExecutor<?> taskExecutor;
            try {
                taskExecutor = JobHandlerUtils.newInstance(schedJob.getJobHandler());
            } catch (Exception e) {
                LOG.error("Load job handler error: " + param, e);
                terminateTask(supervisorService, param, INSTANCE_FAILED, toErrorMsg(e));
                return;
            }

            taskExecutor.task(task);
            param.taskExecutor(taskExecutor);

            try {
                taskExecutor.verify();
            } catch (Exception e) {
                LOG.error("Task verify failed: " + param, e);
                terminateTask(supervisorService, param, VERIFY_FAILED, toErrorMsg(e));
                return;
            }

            // 2、init
            try {
                taskExecutor.init();
                LOG.info("Initiated sched task {}", param);
            } catch (Exception e) {
                LOG.error("Task init error: " + param, e);
                terminateTask(supervisorService, param, INIT_EXCEPTION, toErrorMsg(e));
                return;
            }

            // 3、execute
            try {
                Result<?> result;
                if (schedJob.getExecuteTimeout() > 0) {
                    FutureTask<Result<?>> futureTask = new FutureTask<>(() -> taskExecutor.execute(supervisorService));
                    Thread futureTaskThread = new Thread(futureTask);
                    futureTaskThread.setDaemon(true);
                    futureTaskThread.setName(getClass().getSimpleName() + "#FutureTaskThread" + "-" + FUTURE_TASK_NAMED_SEQ.getAndIncrement());
                    futureTaskThread.start();
                    try {
                        result = futureTask.get(schedJob.getExecuteTimeout(), TimeUnit.MILLISECONDS);
                    } finally {
                        MultithreadExecutors.stopThread(futureTaskThread, 0, 0, 0);
                    }
                } else {
                    result = taskExecutor.execute(supervisorService);
                }
                LOG.info("Executed sched task {}", param);

                // 4、execute end
                if (result.isSuccess()) {
                    LOG.info("Task executed finished {}", param);
                    terminateTask(supervisorService, param, FINISHED, null);
                } else {
                    LOG.error("Task executed failed {} - {}", param, result);
                    terminateTask(supervisorService, param, EXECUTE_FAILED, result.getMsg());
                }
            } catch (TimeoutException e) {
                LOG.error("Task execute timeout: " + param, e);
                terminateTask(supervisorService, param, EXECUTE_TIMEOUT, toErrorMsg(e));
            } catch (PauseTaskException e) {
                LOG.error("PauseTaskException, do pause: " + param, e);
                pauseTask(supervisorService, param, Operations.PAUSE, toErrorMsg(e));
            } catch (CancelTaskException e) {
                LOG.error("CancelTaskException, do manual cancel: " + param, e);
                cancelTask(supervisorService, param, Operations.EXCEPTION_CANCEL, toErrorMsg(e));
            } catch (Exception e) {
                LOG.error("Task execute occur error: " + param, e);
                terminateTask(supervisorService, param, EXECUTE_EXCEPTION, toErrorMsg(e));
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
