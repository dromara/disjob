/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.param;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.model.SchedJob;
import lombok.Getter;

/**
 * Split job param
 *
 * @author Ponfee
 */
@Getter
public class SplitJobParam extends ToJsonString {

    private final long jobId;
    private final String jobGroup;
    private final String jobHandler;
    private final int routeStrategy;
    private final String jobParam;

    private SplitJobParam(long jobId, String jobGroup, String jobHandler, int routeStrategy, String jobParam) {
        this.jobId = jobId;
        this.jobGroup = jobGroup;
        this.jobHandler = jobHandler;
        this.routeStrategy = routeStrategy;
        this.jobParam = jobParam;
    }

    public static SplitJobParam from(SchedJob job) {
        return new SplitJobParam(job.getJobId(), job.getJobGroup(), job.getJobHandler(), job.getRouteStrategy(), job.getJobParam());
    }
    public static SplitJobParam from(SchedJob job, String jobHandler) {
        return new SplitJobParam(job.getJobId(), job.getJobGroup(), jobHandler, job.getRouteStrategy(), job.getJobParam());
    }

}
