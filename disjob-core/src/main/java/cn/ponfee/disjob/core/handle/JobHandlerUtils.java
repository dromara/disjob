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
import cn.ponfee.disjob.core.exception.JobException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Modifier;
import java.util.List;

import static cn.ponfee.disjob.core.base.JobCodeMsg.SPLIT_JOB_FAILED;

/**
 * Job handler utility
 *
 * @author Ponfee
 */
public class JobHandlerUtils {

    public static boolean verify(String jobHandler, String jobParams) {
        if (StringUtils.isBlank(jobHandler)) {
            return false;
        }
        try {
            List<SplitTask> tasks = split(jobHandler, jobParams);
            return CollectionUtils.isNotEmpty(tasks);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Splits job to many sched task.
     *
     * @param jobHandler the job handler
     * @param jobParams  the job param
     * @return list of SplitTask
     * @throws JobException if split failed
     */
    public static List<SplitTask> split(String jobHandler, String jobParams) throws JobException {
        try {
            JobSplitter jobSplitter = JobHandlerUtils.load(jobHandler);
            List<SplitTask> splitTasks = jobSplitter.split(jobParams);
            if (CollectionUtils.isEmpty(splitTasks)) {
                throw new JobException(SPLIT_JOB_FAILED, "Job split none tasks.");
            }
            return splitTasks;
        } catch (JobException e) {
            throw e;
        } catch (Exception e) {
            throw new JobException(SPLIT_JOB_FAILED, "Split job occur error", e);
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
