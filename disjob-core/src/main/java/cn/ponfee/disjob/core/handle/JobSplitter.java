/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.core.exception.JobException;

import java.util.Collections;
import java.util.List;

/**
 * Split schedule job to one instance and many tasks.
 *
 * @author Ponfee
 */
public interface JobSplitter {

    /**
     * Provides default split single task.
     * <p>Subclass can override this method to customize implementation.
     *
     * @param jobParam the job param
     * @return list of SplitTask
     * @throws JobException if split failed
     */
    default List<SplitTask> split(String jobParam) throws JobException {
        return Collections.singletonList(new SplitTask(jobParam));
    }

}
