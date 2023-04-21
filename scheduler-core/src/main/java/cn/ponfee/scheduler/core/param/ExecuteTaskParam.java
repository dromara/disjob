/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.param;

import cn.ponfee.scheduler.common.base.LazyLoader;
import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.base.ToJsonString;
import cn.ponfee.scheduler.common.util.Bytes;
import cn.ponfee.scheduler.common.util.GenericUtils;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.JobType;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.enums.RouteStrategy;
import cn.ponfee.scheduler.core.handle.TaskExecutor;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Task execution parameter.
 *
 * @author Ponfee
 */
@JSONType(deserializer = ExecuteTaskParam.FastjsonDeserializer.class) // fastjson
@JsonDeserialize(using = ExecuteTaskParam.JacksonDeserializer.class)  // jackson
public class ExecuteTaskParam extends ToJsonString implements TimingWheel.Timing<ExecuteTaskParam>, Serializable {
    private static final long serialVersionUID = -6493747747321536680L;

    private final AtomicReference<Operations> operation;
    private final long taskId;
    private final long instanceId;
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
    private volatile transient TaskExecutor<?> taskExecutor;

    /**
     * Constructor
     *
     * @param operation      the operation(if terminate task, this is null value)
     * @param taskId         the task id
     * @param instanceId     the instance id
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
        this.triggerTime = triggerTime;
        this.jobId = jobId;
        this.jobType = jobType;
        this.routeStrategy = routeStrategy;
        this.executeTimeout = executeTimeout;
        this.jobHandler = jobHandler;
    }

    public static ExecuteTaskParamBuilder builder(SchedInstance instance,
                                                  Function<Long, SchedJob> jobLoader) {
        return builder(instance, LazyLoader.of(SchedJob.class, jobLoader, instance.getJobId()));
    }

    public static ExecuteTaskParamBuilder builder(SchedInstance instance, SchedJob schedJob) {
        return new ExecuteTaskParamBuilder(instance, schedJob);
    }

    // ------------------------------------------------getter/setter
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

    // ------------------------------------------------worker

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

    public void taskExecutor(TaskExecutor<?> taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void interrupt() {
        final TaskExecutor<?> executor = this.taskExecutor;
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
        ExecuteTaskParam other = (ExecuteTaskParam) o;
        return this.operation.get() == other.operation.get()
            && this.taskId          == other.taskId
            && this.instanceId      == other.instanceId
            && this.jobId           == other.jobId
            && this.routeStrategy   == other.routeStrategy
            && this.triggerTime     == other.triggerTime
            && this.executeTimeout  == other.executeTimeout
            && this.jobHandler.equals(other.jobHandler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation.get().ordinal(), taskId, instanceId, jobId, routeStrategy, triggerTime, executeTimeout, jobHandler);
    }

    /**
     * Serialize to string
     *
     * @return string of serialized result
     */
    public byte[] serialize() {
        // unnecessary do flip
        byte[] jobHandlerBytes = jobHandler.getBytes(UTF_8);
        return ByteBuffer.allocate(39 + jobHandlerBytes.length)
            .put((byte) operation.get().ordinal()) // 1: operation
            .putLong(taskId)                       // 8: taskId
            .putLong(instanceId)                   // 8: instanceId
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
            buf.getLong(),                           // triggerTime
            buf.getLong(),                           // jobId
            JobType.values()[buf.get()],             // jobType
            RouteStrategy.values()[buf.get()],       // routeStrategy
            buf.getInt(),                            // executeTimeout
            new String(Bytes.remaining(buf), UTF_8)  // jobHandler
        );
    }

    // -----------------------------------------------------custom fastjson deserialize

    /**
     * Custom deserialize ExecuteParam based fastjson.
     */
    public static class FastjsonDeserializer implements ObjectDeserializer {
        @Override
        public ExecuteTaskParam deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
            if (GenericUtils.getRawType(type) != ExecuteTaskParam.class) {
                throw new UnsupportedOperationException("Cannot supported deserialize type: " + type);
            }
            return of(parser.parseObject());
        }

        @Override
        public int getFastMatchToken() {
            return 0 /*JSONToken.RBRACKET*/;
        }
    }

    // -----------------------------------------------------custom jackson deserialize

    /**
     * Custom deserialize ExecuteParam based jackson.
     */
    public static class JacksonDeserializer extends JsonDeserializer<ExecuteTaskParam> {
        @Override
        public ExecuteTaskParam deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return of(p.readValueAs(Jsons.MAP_NORMAL));
        }
    }

    private static ExecuteTaskParam of(Map<String, ?> map) {
        if (map == null) {
            return null;
        }

        Operations operation = EnumUtils.getEnumIgnoreCase(Operations.class, MapUtils.getString(map, "operation"));
        long taskId = MapUtils.getLongValue(map, "taskId");
        long instanceId = MapUtils.getLongValue(map, "instanceId");
        long triggerTime = MapUtils.getLongValue(map, "triggerTime");
        long jobId = MapUtils.getLongValue(map, "jobId");
        JobType jobType = EnumUtils.getEnumIgnoreCase(JobType.class, MapUtils.getString(map, "jobType"));
        RouteStrategy routeStrategy = EnumUtils.getEnumIgnoreCase(RouteStrategy.class, MapUtils.getString(map, "routeStrategy"));
        int executeTimeout = MapUtils.getInteger(map, "executeTimeout");
        String jobHandler = MapUtils.getString(map, "jobHandler");
        Worker worker = Worker.of((Map<String, ?>) map.get("worker"));

        // operation is null if terminate task
        ExecuteTaskParam param = new ExecuteTaskParam(
            operation, taskId, instanceId, triggerTime, jobId,
            jobType, routeStrategy, executeTimeout, jobHandler
        );
        param.setWorker(worker);
        return param;
    }

}
