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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configure worker param
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class ConfigureWorkerParam extends AuthenticationParam {
    private static final long serialVersionUID = 1023942345935168778L;

    /**
     * Action type
     */
    private Action action;

    /**
     * Action data
     */
    private String data;

    public ConfigureWorkerParam(String supervisorToken) {
        super.setSupervisorToken(supervisorToken);
    }

    public enum Action {
        /**
         * Modify worker thread pool maximum pool size
         */
        MODIFY_MAXIMUM_POOL_SIZE {
            @Override
            public <T> T parse(String data) {
                return (T) new Integer(data);
            }
        },

        /**
         * Remove(deregister) worker
         */
        REMOVE_WORKER {
            @Override
            public <T> T parse(String data) {
                throw new UnsupportedOperationException();
            }
        },

        /**
         * Add(register) worker
         */
        ADD_WORKER {
            @Override
            public <T> T parse(String data) {
                return (T) data;
            }
        },

        ;

        public abstract <T> T parse(String data);
    }

}
