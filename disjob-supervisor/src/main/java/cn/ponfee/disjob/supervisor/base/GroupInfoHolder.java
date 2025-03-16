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

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.core.base.GroupInfoService;

import java.util.Set;

import static cn.ponfee.disjob.supervisor.application.SchedGroupService.getGroup;

/**
 * Group info holder.
 * <p>当`group`还未配置时，会报错：“Not found worker group”
 *
 * @author Ponfee
 */
public final class GroupInfoHolder implements GroupInfoService {

    public static final GroupInfoHolder INSTANCE = new GroupInfoHolder();

    private GroupInfoHolder() { }

    @Override
    public String getWorkerContextPath(String group) {
        return getGroup(group).getWorkerContextPath();
    }

    @Override
    public String getSupervisorToken(String group) {
        return getGroup(group).getSupervisorToken();
    }

    @Override
    public String getWorkerToken(String group) {
        return getGroup(group).getWorkerToken();
    }

    @Override
    public String getUserToken(String group) {
        return getGroup(group).getUserToken();
    }

    @Override
    public String getWebhook(String group) {
        return getGroup(group).getWebhook();
    }

    @Override
    public String getOwnUser(String group) {
        return getGroup(group).getOwnUser();
    }

    @Override
    public Set<String> getDevUsers(String group) {
        return getGroup(group).getDevUsers();
    }

    @Override
    public Set<String> getAlertUsers(String group) {
        return getGroup(group).getAlertUsers();
    }

}
