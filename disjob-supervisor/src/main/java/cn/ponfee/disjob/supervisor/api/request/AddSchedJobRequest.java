/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.api.request;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.supervisor.api.converter.SchedJobConverter;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Add sched job request parameter structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class AddSchedJobRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -2765296042332021176L;

    private String jobGroup;
    private String jobName;
    private String jobHandler;
    private Integer jobState;
    private Integer jobType;
    private String jobParam;
    private Integer retryType;
    private Integer retryCount;
    private Integer retryInterval;
    private Date startTime;
    private Date endTime;
    private Integer triggerType;
    private String triggerValue;
    private Integer executeTimeout;
    private Integer collidedStrategy;
    private Integer misfireStrategy;
    private Integer routeStrategy;
    private String remark;

    public SchedJob tosSchedJob() {
        return SchedJobConverter.INSTANCE.convert(this);
    }

}
