/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.supervisor.api.request;

import cn.ponfee.disjob.common.model.PageRequest;
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
    private Integer runType;
    private Integer runState;
    private Date startTime;
    private Date endTime;
    private boolean parent;

}
