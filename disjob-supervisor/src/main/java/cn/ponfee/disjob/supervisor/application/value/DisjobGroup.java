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

package cn.ponfee.disjob.supervisor.application.value;

import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.supervisor.model.SchedGroup;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * Disjob group
 *
 * @author Ponfee
 */
@Getter
public class DisjobGroup {

    private final String group;
    private final String supervisorToken;
    private final String workerToken;
    private final String userToken;
    private final String ownUser;
    private final ImmutableSet<String> alertUsers;
    private final ImmutableSet<String> devUsers;
    private final String workerContextPath;
    private final String webhook;

    public DisjobGroup(SchedGroup o) {
        this.group             = o.getGroup();
        this.supervisorToken   = o.getSupervisorToken();
        this.workerToken       = o.getWorkerToken();
        this.userToken         = o.getUserToken();
        this.ownUser           = o.getOwnUser().trim();
        this.devUsers          = parse(o.getDevUsers(), ownUser);
        this.alertUsers        = parse(o.getAlertUsers(), ownUser);
        this.workerContextPath = Strings.trimPath(o.getWorkerContextPath());
        this.webhook           = o.getWebhook();
    }

    // --------------------------------------------------------------private methods

    private static ImmutableSet<String> parse(String str, String ownUser) {
        if (StringUtils.isBlank(str)) {
            return ImmutableSet.of(ownUser);
        }

        String[] array = str.split(Str.COMMA);
        ImmutableSet.Builder<String> builder = ImmutableSet.builderWithExpectedSize(array.length + 1);
        Arrays.stream(array).filter(StringUtils::isNotBlank).map(String::trim).forEach(builder::add);
        return builder.add(ownUser).build();
    }

}
