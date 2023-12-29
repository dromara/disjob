/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.response;

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

    /**
     * <pre>
     * 返回给端上浏览器JavaScript Number数值过大时会有问题：Number.MAX_SAFE_INTEGER = 9007199254740991
     * 当数值大于`9007199254740991`时就有可能会丢失精度：1234567891011121314 -> 1234567891011121400
     *
     * 方式一：spring.jackson.generator.write_numbers_as_strings=true
     * 方式二：@JsonSerialize(using = ToStringSerializer.class)
     * 方式三：@JsonFormat(shape = JsonFormat.Shape.STRING)
     * </pre>
     */
    private Long jobId;

    private String group;
    private String jobName;
    private Integer jobType;
    private String jobHandler;
    private Integer jobState;
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
    private Long lastTriggerTime;
    private Long nextTriggerTime;
    private String remark;

    private Integer version;
    private Date updatedAt;
    private Date createdAt;
    private String updatedBy;
    private String createdBy;

}
