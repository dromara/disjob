/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.web.response;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Schedule job response structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedJobResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -989147023126011287L;

    private Long jobId;
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
    private Integer collisionStrategy;
    private Integer misfireStrategy;
    private Integer routeStrategy;
    private Integer weightScore;
    private Long lastTriggerTime;
    private Long nextTriggerTime;
    private String alarmSubscribers;
    private String remark;

    private Integer version;
    private Date updatedAt;
    private Date createdAt;
    private String updatedBy;
    private String createdBy;

}
