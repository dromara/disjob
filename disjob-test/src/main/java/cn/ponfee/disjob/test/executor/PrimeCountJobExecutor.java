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

package cn.ponfee.disjob.test.executor;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.test.util.Prime;
import cn.ponfee.disjob.worker.exception.PauseTaskException;
import cn.ponfee.disjob.worker.executor.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 统计任意0<m<=n，[m, n]的素数个数
 *
 * @author Ponfee
 */
public class PrimeCountJobExecutor extends JobExecutor {

    /**
     * 默认以每块1亿分批统计
     */
    private static final long DEFAULT_BLOCK_SIZE = 100_000_000L;

    /**
     * Savepoint任务执行状态时间间隔
     */
    private static final long SAVEPOINT_INTERVAL_MS = 10 * 1000L;

    /**
     * 任务分片，自定义控制任务的拆分数量
     *
     * @param param the split param
     * @return task list
     */
    @Override
    public List<String> split(SplitParam param) {
        JobParam jobParam = Jsons.fromJson(param.getJobParam(), JobParam.class);
        long m = jobParam.getM();
        long n = jobParam.getN();
        long blockSize = Optional.ofNullable(jobParam.getBlockSize()).orElse(DEFAULT_BLOCK_SIZE);
        Assert.isTrue(m > 0, "Number M must be greater than zero.");
        Assert.isTrue(n >= m, "Number N cannot less than M.");
        Assert.isTrue(blockSize > 0, "Block size must be greater than zero.");
        Assert.isTrue(jobParam.getParallel() > 0, "Parallel must be greater than zero.");

        int parallel = n == m ? 1 : (int) Math.min(((n - m) + blockSize - 1) / blockSize, jobParam.getParallel());
        List<String> result = new ArrayList<>(parallel);
        for (int i = 0; i < parallel; i++) {
            TaskParam taskParam = new TaskParam();
            taskParam.setStart(m + blockSize * i);
            taskParam.setBlockSize(blockSize);
            taskParam.setStep(blockSize * parallel);
            taskParam.setN(n);
            result.add(Jsons.toJson(taskParam));
        }
        return result;
    }

    /**
     * 执行任务的逻辑实现
     *
     * @param task      the execution task
     * @param savepoint the savepoint
     * @return execute result
     * @throws Exception if execute occur error
     */
    @Override
    public ExecutionResult execute(ExecutionTask task, Savepoint savepoint) throws Exception {
        TaskParam taskParam = Jsons.fromJson(task.getTaskParam(), TaskParam.class);
        long start = taskParam.getStart();
        long blockSize = taskParam.getBlockSize();
        long step = taskParam.getStep();
        long n = taskParam.getN();
        Assert.isTrue(start > 0, "Start must be greater than zero.");
        Assert.isTrue(blockSize > 0, "Block size must be greater than zero.");
        Assert.isTrue(step > 0, "Step must be greater than zero.");
        Assert.isTrue(n > 0, "N must be greater than zero.");

        ExecuteSnapshot execution;
        if (StringUtils.isEmpty(task.getExecuteSnapshot())) {
            execution = new ExecuteSnapshot(start);
        } else {
            execution = Jsons.fromJson(task.getExecuteSnapshot(), ExecuteSnapshot.class);
            if (execution.getNext() == null || execution.isFinished()) {
                Assert.isTrue(execution.isFinished() && execution.getNext() == null, "Invalid execute snapshot data.");
                return ExecutionResult.success();
            }
        }

        long delta = blockSize - 1, next = execution.getNext();
        long nextSavepointTimeMillis = System.currentTimeMillis() + SAVEPOINT_INTERVAL_MS;
        while (next <= n) {
            if (super.isStopped() || Thread.currentThread().isInterrupted()) {
                savepoint.save(Jsons.toJson(execution));
                throw new PauseTaskException();
            }

            long count = Prime.MillerRabin.countPrimes(next, Math.min(next + delta, n));
            Thread.sleep(97 + ThreadLocalRandom.current().nextLong(197));
            execution.increment(count);

            next += step;
            if (next > n) {
                execution.setNext(null);
                execution.setFinished(true);
            } else {
                execution.setNext(next);
            }

            if (execution.isFinished() || nextSavepointTimeMillis < System.currentTimeMillis()) {
                savepoint.save(Jsons.toJson(execution));
                nextSavepointTimeMillis = System.currentTimeMillis() + SAVEPOINT_INTERVAL_MS;
            }
        }
        return ExecutionResult.success();
    }

    @Setter
    @Getter
    public static class JobParam implements Serializable {
        private static final long serialVersionUID = 2525343069219040629L;

        private long m;
        private long n;
        private Long blockSize; // 分块统计：每块的大小
        private int parallel;   // 并行度：子任务数量
    }

    @Setter
    @Getter
    public static class TaskParam implements Serializable {
        private static final long serialVersionUID = -8122704600602000816L;

        private long start;
        private long blockSize;
        private long step;
        private long n;
    }

    @Setter
    @Getter
    public static class ExecuteSnapshot implements Serializable {
        private static final long serialVersionUID = -5866894559175629912L;

        private Long next;
        private long count;
        private boolean finished;

        public ExecuteSnapshot() { }

        public ExecuteSnapshot(long start) {
            this.next = start;
            this.count = 0;
            this.finished = false;
        }

        public void increment(long delta) {
            this.count += delta;
        }
    }

}
