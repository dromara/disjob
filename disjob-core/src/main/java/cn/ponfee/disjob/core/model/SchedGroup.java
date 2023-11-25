/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.model;

import cn.ponfee.disjob.common.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * The schedule group entity, mapped database table sched_group
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class SchedGroup extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 130809383427026764L;

    /**
     * 分组名称(同sched_job.group)
     */
    private String group;

    /**
     * worker访问supervisor的密钥令牌
     */
    private String workerToken;

    /**
     * supervisor访问worker的密钥令牌
     */
    private String supervisorToken;

    /**
     * 告警订阅人员列表
     */
    private String alarmSubscribers;

    /**
     * 告警web hook地址
     */
    private String webHook;

    /**
     * 行记录版本号
     */
    private Integer version;
}
