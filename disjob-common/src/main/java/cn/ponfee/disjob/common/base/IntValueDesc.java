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

package cn.ponfee.disjob.common.base;

import lombok.Getter;

import java.io.Serializable;

/**
 * Int value & description model
 *
 * @author Ponfee
 */
@Getter
public class IntValueDesc extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 86017761570053534L;

    private final int value;
    private final String desc;

    public IntValueDesc(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

}
