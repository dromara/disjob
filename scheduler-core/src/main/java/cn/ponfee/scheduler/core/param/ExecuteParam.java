package cn.ponfee.scheduler.core.param;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.base.ToJsonString;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.handle.TaskExecutor;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Task execution parameter.
 *
 * @author Ponfee
 */
public class ExecuteParam extends ToJsonString implements TimingWheel.Timing<ExecuteParam>, Serializable {

    private static final long serialVersionUID = -6493747747321536680L;

    private final AtomicReference<Operations> operation;
    private final long taskId;
    private final long trackId;
    private final long jobId;
    private final long triggerTime;

    /**
     * 工作进程(JVM进程)
     */
    private Worker worker;

    /**
     * 任务执行处理器
     */
    private volatile transient TaskExecutor taskExecutor;

    public ExecuteParam(Operations operation, long taskId, long trackId, long jobId, long triggerTime) {
        this(operation, taskId, trackId, jobId, triggerTime, null);
    }

    /**
     * With worker argument for help to deserialization.
     *
     * @param operation   the operation
     * @param taskId      the task id
     * @param trackId     the track id
     * @param jobId       the job id
     * @param triggerTime the trigger time
     * @param worker      the worker
     */
    public ExecuteParam(Operations operation, long taskId, long trackId, long jobId, long triggerTime, Worker worker) {
        //Assert.isTrue(operation != null, "Operation cannot null.");
        this.operation = new AtomicReference<>(operation);
        this.taskId = taskId;
        this.trackId = trackId;
        this.jobId = jobId;
        this.triggerTime = triggerTime;
        this.worker = worker;
    }

    // ------------------------------------------------getter/setter
    public long getTaskId() {
        return taskId;
    }

    public long getTrackId() {
        return trackId;
    }

    public long getJobId() {
        return jobId;
    }

    public long getTriggerTime() {
        return triggerTime;
    }

    /**
     * For help to deserialization
     *
     * @return AtomicReference
     */
    public AtomicReference<Operations> getOperation() {
        return operation;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    // ------------------------------------------------other methods
    public boolean updateOperation(Operations expect, Operations update) {
        return this.operation.compareAndSet(expect, update);
    }

    public Operations operation() {
        return this.operation.get();
    }

    public void taskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void interrupt() {
        TaskExecutor executor = this.taskExecutor;
        if (executor != null) {
            executor.interrupt();
        }
    }

    @Override
    public long timing() {
        return triggerTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExecuteParam other = (ExecuteParam) o;
        return operation.get() == other.operation.get()
            && taskId == other.taskId
            && trackId == other.trackId
            && jobId == other.jobId
            && triggerTime == other.triggerTime;
    }

    /**
     * Returns is whether same trigger task.
     *
     * @param other the other task
     * @return {@code true} if same trigger task
     */
    public boolean same(ExecuteParam other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        return taskId == other.taskId
            && trackId == other.trackId
            && jobId == other.jobId
            && triggerTime == other.triggerTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation.get().ordinal(), taskId, trackId, jobId, triggerTime);
    }

    /**
     * Serialize to string
     *
     * @return string of serialized result
     */
    public byte[] serialize() {
        // unnecessary do flip
        return ByteBuffer.allocate(33)
            .put((byte) operation.get().ordinal())
            .putLong(taskId)
            .putLong(trackId)
            .putLong(jobId)
            .putLong(triggerTime)
            .array();
    }

    /**
     * Deserialize from string.
     *
     * @param bytes the serialized byte array
     * @return TaskParam of deserialized result
     */
    public static ExecuteParam deserialize(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        return new ExecuteParam(
            Operations.of(buf.get()),
            buf.getLong(),
            buf.getLong(),
            buf.getLong(),
            buf.getLong()
        );
    }

}
