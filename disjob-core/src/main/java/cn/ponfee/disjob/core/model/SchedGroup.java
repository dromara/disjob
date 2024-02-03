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

/**
 * The schedule group entity, mapped database table sched_group
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedGroup extends BaseEntity {
    private static final long serialVersionUID = 130809383427026764L;

    /**
     * 分组名称(同sched_job.group)
     */
    private String group;

    /**
     * 负责人
     */
    private String ownUser;

    /**
     * Supervisor访问Worker的密钥令牌
     */
    private String supervisorToken;

    /**
     * Worker访问Supervisor的密钥令牌
     */
    private String workerToken;

    /**
     * User访问Supervisor Openapi接口的密钥令牌(`未部署Admin` 或 `提供类似开放平台` 时使用)
     */
    private String userToken;

    /**
     * 开发人员(多个逗号分隔)
     */
    private String devUsers;

    /**
     * 告警接收人员(多个逗号分隔)
     */
    private String alarmUsers;

    /**
     * 该组下的Worker服务的context-path
     */
    private String workerContextPath;

    /**
     * 告警web hook地址
     */
    private String webHook;

    /**
     * 行记录版本号
     */
    private Integer version;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 创建人
     */
    private String createdBy;

}
