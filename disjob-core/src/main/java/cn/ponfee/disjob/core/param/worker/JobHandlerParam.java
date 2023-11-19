/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param.worker;

import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.model.SchedJob;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Job handler param
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class JobHandlerParam extends AuthenticationParam {
    private static final long serialVersionUID = -216622646271234535L;

    private String jobGroup;
    private String jobHandler;
    private String jobParam;
    private JobType jobType;
    private RouteStrategy routeStrategy;

    public JobHandlerParam(String jobGroup, String jobHandler, String jobParam,
                           JobType jobType, RouteStrategy routeStrategy) {
        this.jobGroup = jobGroup;
        this.jobHandler = jobHandler;
        this.jobParam = jobParam;
        this.jobType = jobType;
        this.routeStrategy = routeStrategy;
    }

    public static JobHandlerParam from(SchedJob job) {
        return from(job, job.getJobHandler());
    }

    public static JobHandlerParam from(SchedJob job, String jobHandler) {
        return new JobHandlerParam(
            job.getJobGroup(),
            jobHandler,
            job.getJobParam(),
            JobType.of(job.getJobType()),
            RouteStrategy.of(job.getRouteStrategy())
        );
    }

}
