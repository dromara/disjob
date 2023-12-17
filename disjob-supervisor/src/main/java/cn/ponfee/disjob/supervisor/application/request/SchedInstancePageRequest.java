/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.model.PageRequest;
import cn.ponfee.disjob.supervisor.application.AuthorizeGroupService;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Sched instance page request
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedInstancePageRequest extends PageRequest {
    private static final long serialVersionUID = 2550102303488212001L;

    private Long jobId;
    private Long instanceId;
    private Integer runType;
    private Integer runState;
    private Date startTime;
    private Date endTime;
    private boolean parent;

    public void authorize(String user, AuthorizeGroupService authorizeGroupService) {
        if (jobId == null && instanceId == null) {
            throw new IllegalArgumentException("Job和InstanceId请至少输入一项");
        }
        if (jobId != null) {
            authorizeGroupService.authorizeJob(user, jobId);
        }
        if (instanceId != null) {
            authorizeGroupService.authorizeInstance(user, instanceId);
        }
    }

}
