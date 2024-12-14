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

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.supervisor.application.converter.SchedGroupConverter;
import cn.ponfee.disjob.supervisor.model.SchedGroup;
import lombok.Getter;
import lombok.Setter;

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

    protected String group;
    protected String ownUser;

    public SchedGroup toSchedGroup(String user) {
        SchedGroup schedGroup = SchedGroupConverter.INSTANCE.convert(this);
        schedGroup.setCreatedBy(user);
        schedGroup.setUpdatedBy(user);
        return schedGroup;
    }

    public void checkAndTrim() {
        this.group = SchedGroup.checkGroup(group);
        this.ownUser = SchedGroup.checkOwnUser(ownUser);
    }

}
