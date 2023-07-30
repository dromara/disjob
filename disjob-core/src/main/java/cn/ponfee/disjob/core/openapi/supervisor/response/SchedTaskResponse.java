/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.openapi.supervisor.response;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Schedule task response structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedTaskResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 3629610339544019607L;

    private Long taskId;
    private Long instanceId;
    private Integer taskNo;
    private Integer taskCount;
    private String taskParam;
    private Date executeStartTime;
    private Date executeEndTime;
    private Long executeDuration;
    private Integer executeState;
    private String executeSnapshot;
    private String worker;
    private String errorMsg;

}
