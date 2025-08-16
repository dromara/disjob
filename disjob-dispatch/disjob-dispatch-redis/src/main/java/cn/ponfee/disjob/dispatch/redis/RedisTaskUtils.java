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

package cn.ponfee.disjob.dispatch.redis;

import cn.ponfee.disjob.common.util.Bytes;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.enums.ShutdownStrategy;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;

import java.nio.ByteBuffer;

import static cn.ponfee.disjob.common.util.Numbers.nullIfZero;
import static cn.ponfee.disjob.common.util.Numbers.zeroIfNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Redis task dispatch utility.
 *
 * @author Ponfee
 */
final class RedisTaskUtils {

    private static final Operation[] OPERATION_VALUES = Operation.values();

    static String buildTaskDispatchKey(Worker worker) {
        return JobConstants.DISJOB_KEY_PREFIX + ".task.dispatch." + worker.serialize();
    }


    /**
     * Serialize to string
     *
     * @return string of serialized result
     */
    static byte[] serialize(ExecuteTaskParam param) {
        byte[] authTokenBytes = Bytes.toBytes(param.getSupervisorAuthenticationToken(), UTF_8);
        byte[] workerBytes = param.getWorker().serialize().getBytes(UTF_8);
        byte[] jobExecutorBytes = param.getJobExecutor().getBytes(UTF_8);

        int authTokenBytesLength = (authTokenBytes == null) ? -1 : authTokenBytes.length;
        int length = 64 + Math.max(0, authTokenBytesLength) + workerBytes.length + jobExecutorBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(length)
            .put((byte) param.getOperation().ordinal())      // 1: operation
            .putLong(param.getTaskId())                      // 8: taskId
            .putLong(param.getInstanceId())                  // 8: instanceId
            .putLong(zeroIfNull(param.getWnstanceId()))      // 8: wnstanceId
            .putLong(param.getTriggerTime())                 // 8: triggerTime
            .putLong(param.getJobId())                       // 8: jobId
            .putInt(param.getRetryCount())                   // 4: retryCount
            .putInt(param.getRetriedCount())                 // 4: retriedCount
            .put((byte) param.getJobType().value())          // 1: jobType
            .put((byte) param.getRouteStrategy().value())    // 1: routeStrategy
            .put((byte) param.getShutdownStrategy().value()) // 1: shutdownStrategy
            .putInt(param.getExecuteTimeout());              // 4: executeTimeout
        buffer.putInt(authTokenBytesLength);                 // 4: supervisorAuthenticationToken byte array length
        Bytes.put(buffer, authTokenBytes);                   // x: byte array of supervisorAuthenticationToken data
        buffer.putInt(workerBytes.length);                   // 4: worker byte array length int value
        buffer.put(workerBytes);                             // x: byte array of worker data
        buffer.put(jobExecutorBytes);                        // x: byte array of jobExecutor data

        // buffer.flip(): unnecessary do flip
        return buffer.array();
    }

    /**
     * Deserialize from string.
     *
     * @param bytes the serialized byte array
     * @return ExecuteTaskParam of deserialized result
     */
    static ExecuteTaskParam deserialize(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        ExecuteTaskParam param = new ExecuteTaskParam();
        param.setOperation(OPERATION_VALUES[buf.get()]);           //   1: operation
        param.setTaskId(buf.getLong());                            //   8: taskId
        param.setInstanceId(buf.getLong());                        //   8: instanceId
        param.setWnstanceId(nullIfZero(buf.getLong()));            //   8: wnstanceId
        param.setTriggerTime(buf.getLong());                       //   8: triggerTime
        param.setJobId(buf.getLong());                             //   8: jobId
        param.setRetryCount(buf.getInt());                         //   4: retryCount
        param.setRetriedCount(buf.getInt());                       //   4: retriedCount
        param.setJobType(JobType.of(buf.get()));                   //   1: jobType
        param.setRouteStrategy(RouteStrategy.of(buf.get()));       //   1: routeStrategy
        param.setShutdownStrategy(ShutdownStrategy.of(buf.get())); //   1: shutdownStrategy
        param.setExecuteTimeout(buf.getInt());                     //   4: executeTimeout
        param.setSupervisorAuthenticationToken(getString(buf));    // 4+x: supervisorAuthenticationToken
        param.setWorker(Worker.deserialize(getString(buf)));       // 4+x: worker
        param.setJobExecutor(getRemainedString(buf));              //   x: jobExecutorBytes
        return param;
    }

    private static String getString(ByteBuffer buf) {
        return Bytes.toString(Bytes.get(buf, buf.getInt()), UTF_8);
    }

    private static String getRemainedString(ByteBuffer buf) {
        return Bytes.toString(Bytes.remained(buf), UTF_8);
    }

}
