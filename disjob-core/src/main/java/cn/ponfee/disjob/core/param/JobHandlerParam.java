/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.model.SchedJob;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Job handler param
 *
 * @author Ponfee
 */
@Getter
@Setter
public class JobHandlerParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -216622646271234535L;

    private String jobHandler;
    private String jobParam;
    private String jobGroup;
    private JobType jobType;
    private RouteStrategy routeStrategy;

    public JobHandlerParam() {
    }

    public JobHandlerParam(String jobHandler, String jobParam, String jobGroup,
                           JobType jobType, RouteStrategy routeStrategy) {
        this.jobHandler = jobHandler;
        this.jobParam = jobParam;
        this.jobGroup = jobGroup;
        this.jobType = jobType;
        this.routeStrategy = routeStrategy;
    }

    public static JobHandlerParam from(SchedJob job) {
        return from(job, job.getJobHandler());
    }

    public static JobHandlerParam from(SchedJob job, String jobHandler) {
        return new JobHandlerParam(
            jobHandler,
            job.getJobParam(),
            job.getJobGroup(),
            JobType.of(job.getJobType()),
            RouteStrategy.of(job.getRouteStrategy())
        );
    }

}
