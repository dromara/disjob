/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.util.Bytes;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.handle.TaskExecutor;
import cn.ponfee.disjob.core.model.InstanceAttach;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Map;
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
@JsonDeserialize(using = ExecuteTaskParam.JacksonDeserializer.class)
public class ExecuteTaskParam extends ToJsonString implements TimingWheel.Timing<ExecuteTaskParam>, Serializable {
    private static final long serialVersionUID = -6493747747321536680L;

    private final AtomicReference<Operations> operation;
    private final long taskId;
    private final long instanceId;
    private final Long wnstanceId;
    private final long triggerTime;

    private final long jobId;
    private final JobType jobType;
    private final RouteStrategy routeStrategy;
    private final int executeTimeout;
    private final String jobHandler;

    /**
     * 任务执行器(JVM进程)
     */
    private Worker worker;

    /**
     * 任务执行处理器
     */
    private volatile transient TaskExecutor taskExecutor;

    /**
     * Constructor
     *
     * @param operation      the operation(if terminate task, this is null value)
     * @param taskId         the task id
     * @param instanceId     the instance id
     * @param wnstanceId     the workflow instance id
     * @param triggerTime    the trigger time
     * @param jobId          the job id
     * @param jobType        the job type
     * @param routeStrategy  the route strategy
     * @param executeTimeout the execution timeout
     * @param jobHandler     the job handler
     */
    public ExecuteTaskParam(Operations operation,
                            long taskId,
                            long instanceId,
                            Long wnstanceId,
                            long triggerTime,
                            long jobId,
                            JobType jobType,
                            RouteStrategy routeStrategy,
                            int executeTimeout,
                            String jobHandler) {
        Assert.notNull(operation, "Operation cannot null.");
        Assert.notNull(routeStrategy, "Route strategy cannot null.");
        this.operation = new AtomicReference<>(operation);
        this.taskId = taskId;
        this.instanceId = instanceId;
        this.wnstanceId = wnstanceId;
        this.triggerTime = triggerTime;
        this.jobId = jobId;
        this.jobType = jobType;
        this.routeStrategy = routeStrategy;
        this.executeTimeout = executeTimeout;
        this.jobHandler = jobHandler;
    }

    public static Builder builder(SchedInstance instance, SchedJob schedJob) {
        return new Builder(instance, schedJob);
    }

    // ---------------------------------------------------------getter/setter

    /**
     * For help to deserialization
     *
     * @return AtomicReference
     */
    public AtomicReference<Operations> getOperation() {
        return operation;
    }

    public long getTaskId() {
        return taskId;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public Long getWnstanceId() {
        return wnstanceId;
    }

    public long getTriggerTime() {
        return triggerTime;
    }

    public long getJobId() {
        return jobId;
    }

    public JobType getJobType() {
        return jobType;
    }

    public RouteStrategy getRouteStrategy() {
        return routeStrategy;
    }

    public int getExecuteTimeout() {
        return executeTimeout;
    }

    public String getJobHandler() {
        return jobHandler;
    }

    // --------------------------------------------------------worker

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

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
            && Objects.equals(this.wnstanceId, other.wnstanceId)
            && this.triggerTime     == other.triggerTime
            && this.jobId           == other.jobId
            && this.jobType         == other.jobType
            && this.routeStrategy   == other.routeStrategy
            && this.executeTimeout  == other.executeTimeout
            && this.jobHandler.equals(other.jobHandler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            operation.get(), taskId, instanceId, wnstanceId, triggerTime,
            jobId, jobType, routeStrategy, executeTimeout, jobHandler
        );
    }

    /**
     * Serialize to string
     *
     * @return string of serialized result
     */
    public byte[] serialize() {
        // unnecessary do flip
        byte[] jobHandlerBytes = jobHandler.getBytes(UTF_8);
        return ByteBuffer.allocate(47 + jobHandlerBytes.length)
            .put((byte) operation.get().ordinal()) // 1: operation
            .putLong(taskId)                       // 8: taskId
            .putLong(instanceId)                   // 8: instanceId
            .putLong(nullZero(wnstanceId))         // 8: wnstanceId
            .putLong(triggerTime)                  // 8: triggerTime
            .putLong(jobId)                        // 8: jobId
            .put((byte) jobType.ordinal())         // 1: jobType
            .put((byte) routeStrategy.ordinal())   // 1: routeStrategy
            .putInt(executeTimeout)                // 4: executeTimeout
            .put(jobHandlerBytes)                  // x: jobHandlerBytes
            .array();
    }

    /**
     * Deserialize from string.
     *
     * @param bytes the serialized byte array
     * @return TaskParam of deserialized result
     */
    public static ExecuteTaskParam deserialize(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        return new ExecuteTaskParam(
            Operations.values()[buf.get()],          // operation
            buf.getLong(),                           // taskId
            buf.getLong(),                           // instanceId
            zeroNull(buf.getLong()),                 // wnstanceId
            buf.getLong(),                           // triggerTime
            buf.getLong(),                           // jobId
            JobType.values()[buf.get()],             // jobType
            RouteStrategy.values()[buf.get()],       // routeStrategy
            buf.getInt(),                            // executeTimeout
            new String(Bytes.remaining(buf), UTF_8)  // jobHandler
        );
    }

    // --------------------------------------------------------custom jackson deserialize

    /**
     * Custom deserialize ExecuteParam based jackson.
     */
    public static class JacksonDeserializer extends JsonDeserializer<ExecuteTaskParam> {
        @Override
        public ExecuteTaskParam deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return ofMap(p.readValueAs(Jsons.MAP_NORMAL));
        }
    }

    private static ExecuteTaskParam ofMap(Map<String, ?> map) {
        if (map == null) {
            return null;
        }

        Operations operation = EnumUtils.getEnum(Operations.class, MapUtils.getString(map, "operation"));
        long taskId = MapUtils.getLongValue(map, "taskId");
        long instanceId = MapUtils.getLongValue(map, "instanceId");
        Long wnstanceId = MapUtils.getLong(map, "wnstanceId");
        long triggerTime = MapUtils.getLongValue(map, "triggerTime");
        long jobId = MapUtils.getLongValue(map, "jobId");
        JobType jobType = EnumUtils.getEnum(JobType.class, MapUtils.getString(map, "jobType"));
        RouteStrategy routeStrategy = EnumUtils.getEnum(RouteStrategy.class, MapUtils.getString(map, "routeStrategy"));
        int executeTimeout = MapUtils.getInteger(map, "executeTimeout");
        String jobHandler = MapUtils.getString(map, "jobHandler");
        Worker worker = Worker.deserialize(MapUtils.getString(map, "worker"));

        // operation is null if terminate task
        ExecuteTaskParam param = new ExecuteTaskParam(
            operation, taskId, instanceId, wnstanceId, triggerTime,
            jobId, jobType, routeStrategy, executeTimeout, jobHandler
        );
        param.setWorker(worker);
        return param;
    }

    public static class Builder {
        private final SchedInstance instance;
        private final SchedJob job;

        private Builder(SchedInstance instance, SchedJob schedJob) {
            this.instance = instance;
            this.job = schedJob;
        }

        public ExecuteTaskParam build(Operations ops, long taskId, long triggerTime, Worker worker) {
            String jobHandler;
            if (instance.getWnstanceId() != null) {
                Assert.hasText(instance.getAttach(), () -> "Workflow node instance attach cannot be null: " + instance.getInstanceId());
                InstanceAttach attach = Jsons.fromJson(instance.getAttach(), InstanceAttach.class);
                jobHandler = DAGNode.fromString(attach.getCurNode()).getName();
            } else {
                jobHandler = job.getJobHandler();
            }

            ExecuteTaskParam param = new ExecuteTaskParam(
                ops,
                taskId,
                instance.getInstanceId(),
                instance.getWnstanceId(),
                triggerTime,
                instance.getJobId(),
                JobType.of(job.getJobType()),
                RouteStrategy.of(job.getRouteStrategy()),
                job.getExecuteTimeout(),
                jobHandler
            );
            param.setWorker(worker);

            return param;
        }
    }

}
