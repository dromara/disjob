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
import cn.ponfee.disjob.supervisor.application.converter.SchedGroupConverter;
import cn.ponfee.disjob.supervisor.model.SchedGroup;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Add sched group request.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedGroupAddRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 8022970678398556635L;

    private String group;
    private String ownUser;
    private String createdBy;

    public SchedGroup toSchedGroup() {
        return SchedGroupConverter.INSTANCE.convert(this);
    }

    public void checkAndTrim() {
        Assert.hasText(group, "Group cannot be blank.");
        Assert.hasText(ownUser, "Own user cannot be blank.");
        Assert.isTrue(!ownUser.contains(Str.COMMA), "Own user cannot contains ','");
        Assert.hasText(createdBy, "Created by cannot be blank.");

        group = group.trim();
        ownUser = ownUser.trim();
    }

}
