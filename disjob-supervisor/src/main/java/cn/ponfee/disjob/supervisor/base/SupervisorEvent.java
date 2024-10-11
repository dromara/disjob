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

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Supervisor event
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SupervisorEvent extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -4173560082801958499L;

    private Type type;
    private String data;

    public static SupervisorEvent of(Type type, String data) {
        SupervisorEvent event = new SupervisorEvent();
        event.setType(Objects.requireNonNull(type));
        event.setData(data);
        return event;
    }

    public enum Type {
        /**
         * Refresh group data
         */
        REFRESH_GROUP {
            @Override
            public <T> T parse(String data) {
                throw new UnsupportedOperationException("Refresh group not support args.");
            }
        },

        ;

        public abstract <T> T parse(String data);
    }

}
