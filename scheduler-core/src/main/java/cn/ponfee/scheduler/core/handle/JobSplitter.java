package cn.ponfee.scheduler.core.handle;

import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedJob;

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
     * @param job the schedule job
     * @return list of SplitTask
     * @throws JobException if split failed
     */
    default List<SplitTask> split(SchedJob job) throws JobException {
        return Collections.singletonList(
            new SplitTask(job.getJobParam())
        );
    }

    /**
     * Split task structure.
     */
    class SplitTask {
        private final String taskParam;

        public SplitTask(String taskParam) {
            this.taskParam = taskParam;
        }

        public String getTaskParam() {
            return taskParam;
        }
    }

}
