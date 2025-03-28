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

package cn.ponfee.disjob.worker.util;

import cn.ponfee.disjob.common.dag.DAGExpression;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.dto.worker.VerifyJobParam;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.exception.JobRuntimeException;
import cn.ponfee.disjob.worker.executor.JobExecutor;
import cn.ponfee.disjob.worker.executor.SplitParam;
import cn.ponfee.disjob.worker.executor.VerifyParam;
import org.springframework.util.Assert;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Job executor utility
 *
 * @author Ponfee
 */
public class JobExecutorUtils {

    public static void verify(VerifyJobParam param) throws JobException {
        try {
            Set<String> jobExecutors = parseJobExecutor(param);
            VerifyParam verifyParam = new VerifyParam(param.getRouteStrategy().isBroadcast(), param.getJobParam());
            for (String jobExecutorStr : jobExecutors) {
                JobExecutor jobExecutor = loadJobExecutor(jobExecutorStr);
                boolean result = jobExecutor.verify(verifyParam);
                Assert.isTrue(result, () -> "Verify job failed: " + param);
            }
        } catch (JobException | JobRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new JobException(JobCodeMsg.INVALID_JOB_EXECUTOR, e.getMessage());
        }
    }

    public static Set<String> parseJobExecutor(VerifyJobParam param) {
        Set<String> jobExecutors;
        if (param.getJobType().isWorkflow()) {
            jobExecutors = DAGExpression.parse(param.getJobExecutor())
                .nodes()
                .stream()
                .filter(e -> !e.isStartOrEnd())
                .map(DAGNode::getName)
                .collect(Collectors.toSet());
            Assert.notEmpty(jobExecutors, () -> "Invalid workflow job executor: " + param.getJobExecutor());
        } else {
            jobExecutors = Collections.singleton(param.getJobExecutor());
        }
        return jobExecutors;
    }

    /**
     * Splits job to many task.
     *
     * @param param the param
     * @return list of task param
     * @throws JobException if split failed
     */
    public static List<String> split(SplitJobParam param) throws JobException {
        try {
            int workerCount = param.getWorkerCount();
            Assert.isTrue(workerCount > 0, () -> "Worker count must greater than zero: " + workerCount);
            JobExecutor jobExecutor = loadJobExecutor(param.getJobExecutor());
            List<String> taskParams = jobExecutor.split(convert(param));
            if (param.getRouteStrategy().isBroadcast()) {
                int size = (taskParams == null) ? 0 : taskParams.size();
                Assert.isTrue(size == workerCount, () -> "Split job task size not equals worker count: " + size + ", " + workerCount);
            } else {
                Assert.notEmpty(taskParams, "Split job task cannot be empty.");
            }
            taskParams.forEach(e -> CoreUtils.checkClobMaximumLength(e, "Splitting task param"));
            return taskParams;
        } catch (JobException | JobRuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new JobException(JobCodeMsg.SPLIT_JOB_FAILED.getCode(), "Split job task failed: " + t.getMessage(), t);
        }
    }

    /**
     * Load JobExecutor instance, String parameter can be spring bean name or qualified class name or source code
     *
     * @param text spring bean name or qualified class name or source code
     * @return JobExecutor instance object
     * @throws JobException if new instance failed
     */
    public static JobExecutor loadJobExecutor(String text) throws JobException {
        if (SpringContextHolder.isNotNull()) {
            // must be annotated with @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
            // get by spring bean name
            JobExecutor executor = SpringContextHolder.getPrototypeBean(text, JobExecutor.class);
            if (executor != null) {
                return executor;
            }

            Class<? extends JobExecutor> jobExecutorClass = getJobExecutorClass(text);
            executor = SpringContextHolder.getPrototypeBean(jobExecutorClass);
            if (executor != null) {
                return executor;
            }

            executor = ClassUtils.newInstance(jobExecutorClass);
            SpringContextHolder.autowireBean(executor);
            return executor;
        } else {
            Class<? extends JobExecutor> jobExecutorClass = getJobExecutorClass(text);
            return ClassUtils.newInstance(jobExecutorClass);
        }
    }

    // ---------------------------------------------------------------------------------private methods

    private static Class<? extends JobExecutor> getJobExecutorClass(String text) throws JobException {
        Class<? extends JobExecutor> type = GroovyUtils.getClass(text);
        if (type == null) {
            throw new JobException(JobCodeMsg.LOAD_JOB_EXECUTOR_ERROR, "Illegal job executor class: " + text);
        }

        // interface type: Modifier.isAbstract(type.getModifiers()) -> true
        if (!JobExecutor.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers())) {
            throw new JobException(JobCodeMsg.LOAD_JOB_EXECUTOR_ERROR, "Invalid job executor '" + type + "': " + text);
        }
        return type;
    }

    private static SplitParam convert(SplitJobParam param) {
        SplitParam splitParam = new SplitParam();
        splitParam.setBroadcast(param.getRouteStrategy().isBroadcast());
        splitParam.setJobParam(param.getJobParam());
        splitParam.setRetryCount(param.getRetryCount());
        splitParam.setRetriedCount(param.getRetriedCount());
        splitParam.setWorkerCount(param.getWorkerCount());
        splitParam.setPredecessorInstances(param.getPredecessorInstances());
        return splitParam;
    }

}
