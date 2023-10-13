/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.common.dag.DAGExpressionParser;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.Predicates;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.exception.JobCheckedException;
import cn.ponfee.disjob.core.param.JobHandlerParam;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.core.base.JobCodeMsg.INVALID_JOB_HANDLER;
import static cn.ponfee.disjob.core.base.JobCodeMsg.SPLIT_JOB_FAILED;

/**
 * Job handler utility
 *
 * @author Ponfee
 */
public class JobHandlerUtils {

    public static void verify(JobHandlerParam param) throws JobCheckedException {
        Assert.hasText(param.getJobHandler(), "Job handler cannot be blank.");
        Set<String> jobHandlers;
        if (param.getJobType() == JobType.WORKFLOW) {
            jobHandlers = new DAGExpressionParser(param.getJobHandler())
                .parse()
                .nodes()
                .stream()
                .filter(Predicates.not(DAGNode::isStartOrEnd))
                .map(DAGNode::getName)
                .collect(Collectors.toSet());
            Assert.notEmpty(jobHandlers, () -> "Invalid workflow job handler: " + param.getJobHandler());
        } else {
            jobHandlers = Collections.singleton(param.getJobHandler());
        }

        for (String jobHandler : jobHandlers) {
            if (param.getRouteStrategy() == RouteStrategy.BROADCAST) {
                try {
                    JobHandler<?> handler = load(jobHandler);
                    Assert.isTrue(handler instanceof BroadcastJobHandler, () -> "Not a broadcast job handler: " + jobHandler);
                } catch (JobCheckedException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new JobCheckedException(INVALID_JOB_HANDLER, e.getMessage());
                }
            } else {
                try {
                    param.setJobHandler(jobHandler);
                    Assert.notEmpty(split(param), () -> "Not split any task: " + jobHandler);
                } catch (JobCheckedException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new JobCheckedException(INVALID_JOB_HANDLER, e.getMessage());
                }
            }
        }
    }

    /**
     * Splits job to many sched task.
     *
     * @param param the job handler parameter
     * @return list of SplitTask
     * @throws JobCheckedException if split failed
     */
    public static List<SplitTask> split(JobHandlerParam param) throws JobCheckedException {
        try {
            JobSplitter jobSplitter = load(param.getJobHandler());
            List<SplitTask> splitTasks = jobSplitter.split(param.getJobParam());
            if (CollectionUtils.isEmpty(splitTasks)) {
                throw new JobCheckedException(SPLIT_JOB_FAILED, "Job split none tasks.");
            }
            return splitTasks;
        } catch (JobCheckedException e) {
            throw e;
        } catch (Throwable t) {
            throw new JobCheckedException(SPLIT_JOB_FAILED, "Split job occur error", t);
        }
    }

    /**
     * Load jobHandler instance, String parameter can be qualified class name or source code
     *
     * @param text qualified class name or source code
     * @return JobHandler instance object
     * @throws JobCheckedException if new instance failed
     */
    public static JobHandler<?> load(String text) throws JobCheckedException {
        JobHandler<?> handler = SpringContextHolder.getPrototypeBean(text, JobHandler.class);
        if (handler != null) {
            // must be annotated with @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
            return handler;
        }

        Class<JobHandler<?>> type = ClassUtils.getClass(text);
        if (type == null) {
            throw new JobCheckedException(JobCodeMsg.LOAD_HANDLER_ERROR, "Illegal job handler: " + text);
        }

        // interface type: Modifier.isAbstract(type.getModifiers()) -> true
        if (!JobHandler.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers())) {
            throw new JobCheckedException(JobCodeMsg.LOAD_HANDLER_ERROR, "Invalid job handler: " + ClassUtils.getName(type) + ", " + text);
        }

        handler = SpringContextHolder.getPrototypeBean(type);
        if (handler != null) {
            // must be annotated with @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
            return handler;
        }

        handler = ClassUtils.newInstance(type);
        SpringContextHolder.autowire(handler);
        return handler;
    }

}
