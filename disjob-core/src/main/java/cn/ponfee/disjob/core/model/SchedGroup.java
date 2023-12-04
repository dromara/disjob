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
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * The schedule group entity, mapped database table sched_group
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedGroup extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 130809383427026764L;

    /**
     * 分组名称(同sched_job.group)
     */
    private String group;

    /**
     * Supervisor访问Worker的密钥令牌
     */
    private String supervisorToken;

    /**
     * Worker访问Supervisor的密钥令牌
     */
    private String workerToken;

    /**
     * User访问Supervisor的openapi接口密钥令牌(未部署Admin 或 提供类似开放平台 时使用)
     */
    private String userToken;

    /**
     * Group own user
     */
    private String ownUser;

    /**
     * 告警人员(多个逗号分隔)
     */
    private String alarmUsers;

    /**
     * 告警web hook地址
     */
    private String webHook;

    /**
     * 行记录版本号
     */
    private Integer version;

    // -------------------------------------------------------non table columns

    /**
     * Group dev users
     */
    private List<String> devUsers;

}