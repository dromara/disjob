package cn.ponfee.scheduler.core.handle;

import cn.ponfee.scheduler.core.exception.JobException;

import java.util.Collections;
import java.util.List;

/**
 * Split schedule job to one track and many tasks.
 *
 * @author Ponfee
 */
public interface JobSplitter {

    /**
     * Provides default tasks split.
     * <p>Subclass can override this method to provide.
     *
     * @param jobParam the job param
     * @return list of SplitTask
     * @throws JobException if split failed
     */
    default List<SplitTask> split(String jobParam) throws JobException {
        return Collections.singletonList(
            new SplitTask(jobParam)
        );
    }

}
