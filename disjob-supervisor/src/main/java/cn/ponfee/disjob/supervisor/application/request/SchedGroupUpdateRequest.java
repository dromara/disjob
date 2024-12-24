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
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.supervisor.application.converter.SchedGroupConverter;
import cn.ponfee.disjob.supervisor.model.SchedGroup;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Update sched group request parameter structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedGroupUpdateRequest extends SchedGroupAddRequest {
    private static final long serialVersionUID = 7531416191031943146L;

    private String devUsers;
    private String alertUsers;
    private String workerContextPath;
    private String webhook;
    private int version;

    @Override
    public SchedGroup toSchedGroup(String user) {
        SchedGroup schedGroup = SchedGroupConverter.INSTANCE.convert(this);
        schedGroup.setUpdatedBy(user);
        return schedGroup;
    }

    @Override
    public void checkAndTrim() {
        Assert.hasText(group, "Group cannot be blank.");
        this.ownUser = SchedGroup.checkOwnUser(ownUser);
        this.devUsers = prune(devUsers);
        this.alertUsers = prune(alertUsers);
        this.workerContextPath = Strings.trimPath(workerContextPath);
        this.webhook = StringUtils.trim(webhook);
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
