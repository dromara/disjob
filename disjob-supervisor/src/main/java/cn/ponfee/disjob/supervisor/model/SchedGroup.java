/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.base.Symbol;
import cn.ponfee.disjob.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

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

    public static String checkGroup(String group) {
        Assert.hasText(group, "Group cannot be blank.");
        Assert.isTrue((group = group.trim()).length() <= 60, "Group length cannot exceed limit 60.");
        return group;
    }

    public static String checkOwnUser(String ownUser) {
        Assert.hasText(ownUser, "Own user cannot be blank.");
        Assert.isTrue(!ownUser.contains(Symbol.Str.COMMA), "Own user cannot contains character ','");
        Assert.isTrue((ownUser = ownUser.trim()).length() <= 60, "Own user length cannot exceed limit 60.");
        return ownUser;
    }

}
