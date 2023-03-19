/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.param;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.base.ToJsonString;
import cn.ponfee.scheduler.common.util.GenericUtils;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.JobType;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.handle.TaskExecutor;
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

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

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
    private final long jobId;
    private final JobType jobType;
    private final long triggerTime;

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
     * @param operation   the operation(if terminate task, this is null value)
     * @param taskId      the task id
     * @param instanceId  the instance id
     * @param jobId       the job id
     * @param jobType     the job type
     * @param triggerTime the trigger time
     * @param worker      the worker
     */
    public ExecuteTaskParam(@NotNull Operations operation,
                            long taskId,
                            long instanceId,
                            long jobId,
                            @NotNull JobType jobType,
                            long triggerTime,
                            @Nullable Worker worker) {
        Assert.notNull(operation, "Operation cannot null.");
        Assert.notNull(jobType, "Job type cannot null.");
        this.operation = new AtomicReference<>(operation);
        this.taskId = taskId;
        this.instanceId = instanceId;
        this.jobId = jobId;
        this.jobType = jobType;
        this.triggerTime = triggerTime;
        this.worker = worker;
    }

    // ------------------------------------------------getter/setter

    public long getTaskId() {
        return taskId;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public long getJobId() {
        return jobId;
    }

    public JobType getJobType() {
        return jobType;
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
            && this.jobType         == other.jobType
            && this.triggerTime     == other.triggerTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation.get().ordinal(), taskId, instanceId, jobId, jobType, triggerTime);
    }

    /**
     * Serialize to string
     *
     * @return string of serialized result
     */
    public byte[] serialize() {
        // unnecessary do flip
        return ByteBuffer.allocate(34)
            .put((byte) operation.get().ordinal())
            .putLong(taskId)
            .putLong(instanceId)
            .putLong(jobId)
            .put((byte) jobType.ordinal())
            .putLong(triggerTime)
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
            Operations.values()[buf.get()],
            buf.getLong(),
            buf.getLong(),
            buf.getLong(),
            JobType.values()[buf.get()],
            buf.getLong(),
            null
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
        long jobId = MapUtils.getLongValue(map, "jobId");
        JobType jobType = EnumUtils.getEnumIgnoreCase(JobType.class, MapUtils.getString(map, "jobType"));
        long triggerTime = MapUtils.getLongValue(map, "triggerTime");
        Worker worker = Worker.of((Map<String, ?>) map.get("worker"));

        // operation is null if terminate task
        return new ExecuteTaskParam(operation, taskId, instanceId, jobId, jobType, triggerTime, worker);
    }

}
