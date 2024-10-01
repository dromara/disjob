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

package cn.ponfee.disjob.dispatch;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.util.Bytes;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.dto.worker.AuthenticationParam;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.enums.ShutdownStrategy;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.nio.ByteBuffer;
import java.util.Objects;

import static cn.ponfee.disjob.common.util.Numbers.nullZero;
import static cn.ponfee.disjob.common.util.Numbers.zeroNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Task execute parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ExecuteTaskParam extends AuthenticationParam implements TimingWheel.Timing<ExecuteTaskParam> {
    private static final long serialVersionUID = -6493747747321536680L;
    private static final Operation[] OPERATION_VALUES = Operation.values();

    private Operation operation;
    private long taskId;
    private long instanceId;
    private Long wnstanceId;
    private long triggerTime;
    private long jobId;
    private int retryCount;
    private int retriedCount;
    private JobType jobType;
    private RouteStrategy routeStrategy;
    private ShutdownStrategy shutdownStrategy;
    private int executeTimeout;
    private String jobExecutor;
    private Worker worker;

    @Override
    public long timing() {
        return triggerTime;
    }

    /**
     * Serialize to string
     *
     * @return string of serialized result
     */
    public byte[] serialize() {
        String supervisorToken = super.getSupervisorToken();
        byte[] supervisorTokenBytes = (supervisorToken != null) ? supervisorToken.getBytes(UTF_8) : null;
        byte[] workerBytes = worker.serialize().getBytes(UTF_8);
        byte[] jobExecutorBytes = jobExecutor.getBytes(UTF_8);

        int supervisorTokenBytesLength = supervisorTokenBytes == null ? -1 : supervisorTokenBytes.length;
        int length = 64 + Math.max(0, supervisorTokenBytesLength) + workerBytes.length + jobExecutorBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(length)
            .put((byte) operation.ordinal())        // 1: operation
            .putLong(taskId)                        // 8: taskId
            .putLong(instanceId)                    // 8: instanceId
            .putLong(nullZero(wnstanceId))          // 8: wnstanceId
            .putLong(triggerTime)                   // 8: triggerTime
            .putLong(jobId)                         // 8: jobId
            .putInt(retryCount)                     // 4: retryCount
            .putInt(retriedCount)                   // 4: retriedCount
            .put((byte) jobType.value())            // 1: jobType
            .put((byte) routeStrategy.value())      // 1: routeStrategy
            .put((byte) shutdownStrategy.value())   // 1: shutdownStrategy
            .putInt(executeTimeout);                // 4: executeTimeout

        buffer.putInt(supervisorTokenBytesLength);  // 4: supervisorToken byte array length
        Bytes.put(buffer, supervisorTokenBytes);    // x: byte array of supervisorToken data
        buffer.putInt(workerBytes.length);          // 4: worker byte array length int value
        buffer.put(workerBytes);                    // x: byte array of worker data
        buffer.put(jobExecutorBytes);               // x: byte array of jobExecutor data

        // buffer.flip(): unnecessary do flip
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
        param.setOperation(OPERATION_VALUES[buf.get()]);                           //   1: operation
        param.setTaskId(buf.getLong());                                            //   8: taskId
        param.setInstanceId(buf.getLong());                                        //   8: instanceId
        param.setWnstanceId(zeroNull(buf.getLong()));                              //   8: wnstanceId
        param.setTriggerTime(buf.getLong());                                       //   8: triggerTime
        param.setJobId(buf.getLong());                                             //   8: jobId
        param.setRetryCount(buf.getInt());                                         //   4: retryCount
        param.setRetriedCount(buf.getInt());                                       //   4: retriedCount
        param.setJobType(JobType.of(buf.get()));                                   //   1: jobType
        param.setRouteStrategy(RouteStrategy.of(buf.get()));                       //   1: routeStrategy
        param.setShutdownStrategy(ShutdownStrategy.of(buf.get()));                 //   1: shutdownStrategy
        param.setExecuteTimeout(buf.getInt());                                     //   4: executeTimeout

        param.setSupervisorToken(Strings.of(Bytes.get(buf, buf.getInt()), UTF_8)); // 4+x: supervisorToken
        param.setWorker(Worker.deserialize(Bytes.get(buf, buf.getInt()), UTF_8));  // 4+x: worker
        param.setJobExecutor(Strings.of(Bytes.remained(buf), UTF_8));              //   x: jobExecutorBytes
        return param;
    }

    public static Builder builder(SchedInstance instance, SchedJob job, String supervisorToken) {
        return new Builder(instance, job, supervisorToken);
    }

    public static class Builder {
        private final SchedInstance instance;
        private final SchedJob job;
        private final String supervisorToken;

        private Builder(SchedInstance instance, SchedJob job, String supervisorToken) {
            if (!instance.getJobId().equals(job.getJobId())) {
                throw new IllegalArgumentException("Inconsistent job id: " + instance.getJobId() + "!=" + job.getJobId());
            }
            this.instance = instance;
            this.job = job;
            this.supervisorToken = supervisorToken;
        }

        public ExecuteTaskParam build(Operation operation, long taskId, long triggerTime, Worker worker) {
            ExecuteTaskParam param = new ExecuteTaskParam();
            param.setOperation(Objects.requireNonNull(operation, "Operation cannot be null."));
            param.setTaskId(taskId);
            param.setInstanceId(instance.getInstanceId());
            param.setWnstanceId(instance.getWnstanceId());
            param.setTriggerTime(triggerTime);
            param.setJobId(job.getJobId());
            param.setRetryCount(job.getRetryCount());
            param.setRetriedCount(instance.getRetriedCount());
            param.setJobType(JobType.of(job.getJobType()));
            param.setRouteStrategy(RouteStrategy.of(job.getRouteStrategy()));
            param.setShutdownStrategy(ShutdownStrategy.of(job.getShutdownStrategy()));
            param.setExecuteTimeout(job.getExecuteTimeout());
            param.setSupervisorToken(supervisorToken);
            param.setWorker(worker);
            param.setJobExecutor(obtainJobExecutor());
            return param;
        }

        private String obtainJobExecutor() {
            if (!instance.isWorkflow()) {
                Assert.hasText(job.getJobExecutor(), () -> "General job executor cannot be null: " + job.getJobId());
                return job.getJobExecutor();
            }

            String curJobExecutor = instance.parseAttach().parseCurNode().getName();
            Assert.hasText(curJobExecutor, () -> "Curr node job executor cannot be empty: " + instance.getInstanceId());
            return curJobExecutor;
        }
    }

}
