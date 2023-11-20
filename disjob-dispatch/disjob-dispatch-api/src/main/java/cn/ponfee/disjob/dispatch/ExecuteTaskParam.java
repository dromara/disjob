/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.util.Bytes;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.handle.TaskExecutor;
import cn.ponfee.disjob.core.model.InstanceAttach;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.param.worker.AuthenticationParam;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static cn.ponfee.disjob.common.util.Numbers.nullZero;
import static cn.ponfee.disjob.common.util.Numbers.zeroNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Task execution parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ExecuteTaskParam extends AuthenticationParam implements TimingWheel.Timing<ExecuteTaskParam> {
    private static final long serialVersionUID = -6493747747321536680L;

    private AtomicReference<Operations> operation;
    private long taskId;
    private long instanceId;
    private Long wnstanceId;
    private long triggerTime;
    private long jobId;
    private JobType jobType;
    private RouteStrategy routeStrategy;
    private int executeTimeout;
    private String jobHandler;
    private Worker worker;

    /**
     * 任务执行处理器
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private volatile transient TaskExecutor taskExecutor;

    // --------------------------------------------------------other methods

    public boolean updateOperation(Operations expect, Operations update) {
        return this.operation.compareAndSet(expect, update);
    }

    public Operations operation() {
        return this.operation.get();
    }

    public void taskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void stop() {
        final TaskExecutor executor = this.taskExecutor;
        if (executor != null) {
            executor.stop();
        }
    }

    public static Builder builder(SchedInstance instance, SchedJob schedJob, String supervisorToken) {
        return new Builder(instance, schedJob, supervisorToken);
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
        ExecuteTaskParam other = (ExecuteTaskParam) o;
        return this.operation.get() == other.operation.get()
            && this.taskId          == other.taskId
            && this.instanceId      == other.instanceId
            && this.triggerTime     == other.triggerTime
            && this.jobId           == other.jobId
            && Objects.equals(this.wnstanceId, other.wnstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation.get(), taskId, instanceId, triggerTime, jobId, wnstanceId);
    }

    /**
     * Serialize to string
     *
     * @return string of serialized result
     */
    public byte[] serialize() {
        byte[] supervisorTokenBytes = (supervisorToken != null) ? supervisorToken.getBytes(UTF_8) : null;
        byte[] workerBytes = worker.serialize().getBytes(UTF_8);
        byte[] jobHandlerBytes = jobHandler.getBytes(UTF_8);

        int supervisorTokenBytesLength = supervisorTokenBytes == null ? -1 : supervisorTokenBytes.length;
        int length = 55 + Math.max(0, supervisorTokenBytesLength) + workerBytes.length + jobHandlerBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(length)
            .put((byte) operation.get().ordinal()) // 1: operation
            .putLong(taskId)                       // 8: taskId
            .putLong(instanceId)                   // 8: instanceId
            .putLong(nullZero(wnstanceId))         // 8: wnstanceId
            .putLong(triggerTime)                  // 8: triggerTime
            .putLong(jobId)                        // 8: jobId
            .put((byte) jobType.ordinal())         // 1: jobType
            .put((byte) routeStrategy.ordinal())   // 1: routeStrategy
            .putInt(executeTimeout);               // 4: executeTimeout

        buffer.putInt(supervisorTokenBytesLength); // 4: supervisorToken byte array length
        Bytes.put(buffer, supervisorTokenBytes);   // x: byte array of supervisorToken data
        buffer.putInt(workerBytes.length);         // 4: worker byte array length int value
        buffer.put(workerBytes);                   // x: byte array of worker data
        buffer.put(jobHandlerBytes);               // x: byte array of jobHandler data

        // unnecessary do flip
        return buffer.array();
    }

    /**
     * Deserialize from string.
     *
     * @param bytes the serialized byte array
     * @return TaskParam of deserialized result
     */
    public static ExecuteTaskParam deserialize(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        ExecuteTaskParam param = new ExecuteTaskParam();
        param.setOperation(new AtomicReference<>(Operations.values()[buf.get()])); //   1: operation
        param.setTaskId(buf.getLong());                                            //   8: taskId
        param.setInstanceId(buf.getLong());                                        //   8: instanceId
        param.setWnstanceId(zeroNull(buf.getLong()));                              //   8: wnstanceId
        param.setTriggerTime(buf.getLong());                                       //   8: triggerTime
        param.setJobId(buf.getLong());                                             //   8: jobId
        param.setJobType(JobType.values()[buf.get()]);                             //   1: jobType
        param.setRouteStrategy(RouteStrategy.values()[buf.get()]);                 //   1: routeStrategy
        param.setExecuteTimeout(buf.getInt());                                     //   4: executeTimeout

        param.setSupervisorToken(Strings.of(Bytes.get(buf, buf.getInt()), UTF_8)); // 4+x: supervisorToken
        param.setWorker(Worker.deserialize(Bytes.get(buf, buf.getInt()), UTF_8));  // 4+x: worker
        param.setJobHandler(Strings.of(Bytes.remained(buf), UTF_8));               //   x: jobHandlerBytes
        return param;
    }

    public static class Builder {
        private final SchedInstance instance;
        private final SchedJob job;
        private final String supervisorToken;

        private Builder(SchedInstance instance, SchedJob job, String supervisorToken) {
            Assert.isTrue(
                instance.getJobId().equals(job.getJobId()),
                () -> "Invalid instance job id: " + instance.getJobId() + "!=" + job.getJobId()
            );
            this.instance = instance;
            this.job = job;
            this.supervisorToken = supervisorToken;
        }

        public ExecuteTaskParam build(Operations ops, long taskId, long triggerTime, Worker worker) {
            ExecuteTaskParam param = new ExecuteTaskParam();
            param.setOperation(new AtomicReference<>(ops));
            param.setTaskId(taskId);
            param.setInstanceId(instance.getInstanceId());
            param.setWnstanceId(instance.getWnstanceId());
            param.setTriggerTime(triggerTime);
            param.setJobId(job.getJobId());
            param.setJobType(JobType.of(job.getJobType()));
            param.setRouteStrategy(RouteStrategy.of(job.getRouteStrategy()));
            param.setExecuteTimeout(job.getExecuteTimeout());
            param.setSupervisorToken(supervisorToken);
            param.setWorker(worker);
            param.setJobHandler(obtainJobHandler());
            return param;
        }

        private String obtainJobHandler() {
            if (instance.getWnstanceId() == null) {
                Assert.hasText(job.getJobHandler(), () -> "General job handler cannot be null: " + job.getJobId());
                return job.getJobHandler();
            }
            Assert.hasText(instance.getAttach(), () -> "Workflow node instance attach cannot be null: " + instance.getInstanceId());
            InstanceAttach attach = Jsons.fromJson(instance.getAttach(), InstanceAttach.class);
            return DAGNode.fromString(attach.getCurNode()).getName();
        }
    }

}
