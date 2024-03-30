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

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.supervisor.application.converter.SchedGroupConverter;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Update sched group request parameter structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedGroupUpdateRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7531416191031943146L;

    private String group;
    private String ownUser;
    private String devUsers;
    private String alarmUsers;
    private String workerContextPath;
    private String webHook;
    private String updatedBy;
    private int version;

    public SchedGroup toSchedGroup() {
        return SchedGroupConverter.INSTANCE.convert(this);
    }

    public void checkAndTrim() {
        Assert.hasText(ownUser, "Own user cannot be blank.");
        Assert.isTrue(!ownUser.contains(Str.COMMA), "Own user cannot contains ','");
        Assert.hasText(updatedBy, "Updated by cannot be blank.");
        this.ownUser = StringUtils.trim(ownUser);
        this.devUsers = prune(devUsers);
        this.alarmUsers = prune(alarmUsers);
        this.workerContextPath = Strings.trimUrlPath(workerContextPath);
        this.webHook = StringUtils.trim(webHook);
    }

    private static String prune(String users) {
        if (users == null) {
            return null;
        }

        return Arrays.stream(users.split(Str.COMMA))
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .collect(Collectors.joining(Str.COMMA));
    }

}
