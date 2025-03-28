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

package cn.ponfee.disjob.core.dto.worker;

import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.enums.RegistryEventType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

/**
 * Supervisor changed event param.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SupervisorEventParam extends AuthenticationParam {
    private static final long serialVersionUID = -216622646271234535L;

    private RegistryEventType eventType;
    private Supervisor supervisor;

    public void check() {
        Assert.notNull(eventType, "Event type cannot be null.");
        Assert.notNull(supervisor, "Supervisor cannot be null.");
    }

    public static SupervisorEventParam of(String group, RegistryEventType eventType, Supervisor supervisor) {
        SupervisorEventParam param = new SupervisorEventParam();
        param.fillSupervisorAuthenticationToken(group);
        param.setEventType(eventType);
        param.setSupervisor(supervisor);

        param.check();
        return param;
    }

}
