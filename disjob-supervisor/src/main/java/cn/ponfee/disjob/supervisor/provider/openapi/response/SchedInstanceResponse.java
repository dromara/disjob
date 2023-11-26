/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.provider.openapi.response;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.provider.openapi.converter.SchedJobConverter;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Schedule instance response structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedInstanceResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -6772222626245934369L;

    private Long instanceId;
    private Long rnstanceId;
    private Long pnstanceId;
    private Long wnstanceId;
    private Long jobId;
    private Long triggerTime;
    private Integer runType;
    private Integer runState;
    private Date runStartTime;
    private Date runEndTime;
    private Long runDuration;
    private Integer retriedCount;
    private String attach;

    /**
     * 是否有子节点：0-无；1-有；
     */
    private Integer isTreeLeaf;

    private List<SchedTaskResponse> tasks;

    public static SchedInstanceResponse of(SchedInstance instance, List<SchedTask> tasks) {
        if (instance == null) {
            return null;
        }

        SchedInstanceResponse instanceResponse = SchedJobConverter.INSTANCE.convert(instance);
        instanceResponse.setTasks(Collects.convert(tasks, SchedJobConverter.INSTANCE::convert));
        return instanceResponse;
    }

}
