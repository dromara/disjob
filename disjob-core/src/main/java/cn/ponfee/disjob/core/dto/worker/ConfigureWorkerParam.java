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
import lombok.Setter;

/**
 * Configure worker param
 *
 * @author Ponfee
 */
@Getter
@Setter
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

    public static ConfigureWorkerParam of(String group, Action action, String data) {
        ConfigureWorkerParam param = new ConfigureWorkerParam();
        param.fillSupervisorAuthenticationToken(group);
        param.setAction(action);
        param.setData(data);
        return param;
    }

    public enum Action {
        /**
         * Modify worker thread pool maximum pool size
         */
        MODIFY_MAXIMUM_POOL_SIZE {
            @SuppressWarnings("unchecked")
            @Override
            public <T> T parse(String data) {
                return (T) (Integer) Integer.parseInt(data);
            }
        },

        /**
         * Remove(deregister) worker
         */
        REMOVE_WORKER,

        /**
         * Add(register) worker
         */
        ADD_WORKER,

        ;

        public <T> T parse(String data) {
            throw new UnsupportedOperationException();
        }
    }

}
