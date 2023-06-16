/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.param.JobHandlerParam;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Modifier;
import java.util.List;

import static cn.ponfee.disjob.core.base.JobCodeMsg.INVALID_JOB_HANDLER;
import static cn.ponfee.disjob.core.base.JobCodeMsg.SPLIT_JOB_FAILED;

/**
 * Job handler utility
 *
 * @author Ponfee
 */
public class JobHandlerUtils {

    public static void verify(JobHandlerParam param) throws JobException {
        String jobHandler = param.getJobHandler();
        Assert.hasText(jobHandler, "Job handler cannot be blank.");
        if (param.getRouteStrategy() == RouteStrategy.BROADCAST) {
            JobHandler<?> handler;
            try {
                handler = JobHandlerUtils.load(jobHandler);
            } catch (Throwable e) {
                throw new JobException(INVALID_JOB_HANDLER, e.getMessage());
            }
            if (!(handler instanceof BroadcastJobHandler)) {
                throw new JobException(INVALID_JOB_HANDLER, "Not a broadcast job handler.");
            }
        } else {
            List<SplitTask> tasks;
            try {
                tasks = split(param);
            } catch (Throwable e) {
                throw new JobException(INVALID_JOB_HANDLER, e.getMessage());
            }
            if (CollectionUtils.isEmpty(tasks)) {
                throw new JobException(INVALID_JOB_HANDLER, "Not split any task.");
            }
        }
    }

    /**
     * Splits job to many sched task.
     *
     * @param param the job handler parameter
     * @return list of SplitTask
     * @throws JobException if split failed
     */
    public static List<SplitTask> split(JobHandlerParam param) throws JobException {
        try {
            JobSplitter jobSplitter = JobHandlerUtils.load(param.getJobHandler());
            List<SplitTask> splitTasks = jobSplitter.split(param.getJobParam());
            if (CollectionUtils.isEmpty(splitTasks)) {
                throw new JobException(SPLIT_JOB_FAILED, "Job split none tasks.");
            }
            return splitTasks;
        } catch (JobException e) {
            throw e;
        } catch (Throwable t) {
            throw new JobException(SPLIT_JOB_FAILED, "Split job occur error", t);
        }
    }

    /**
     * Load jobHandler instance, String parameter can be qualified class name or source code
     *
     * @param text qualified class name or source code
     * @return JobHandler instance object
     * @throws JobException if new instance failed
     */
    public static JobHandler<?> load(String text) throws JobException {
        JobHandler<?> handler = SpringContextHolder.getPrototypeBean(text, JobHandler.class);
        if (handler != null) {
            // must be annotated with @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
            return handler;
        }

        Class<JobHandler<?>> type = ClassUtils.getClass(text);
        if (type == null) {
            throw new JobException(JobCodeMsg.LOAD_HANDLER_ERROR, "Illegal class text: " + text);
        }

        // interface type: Modifier.isAbstract(type.getModifiers()) -> true
        if (!JobHandler.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers())) {
            throw new JobException(JobCodeMsg.LOAD_HANDLER_ERROR, "Invalid class type: " + ClassUtils.getName(type) + ", " + text);
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
