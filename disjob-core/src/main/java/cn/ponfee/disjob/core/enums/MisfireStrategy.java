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

package cn.ponfee.disjob.core.enums;

import cn.ponfee.disjob.common.base.IntValueEnum;
import cn.ponfee.disjob.common.util.Enums;

import java.util.Map;
import java.util.Objects;

/**
 * The misfire strategy enum definition.
 * <p>mapped by sched_job.misfire_strategy
 *
 * @author Ponfee
 */
public enum MisfireStrategy implements IntValueEnum<MisfireStrategy> {

    /**
     * 触发最近一次misfire
     */
    LAST(1, "触发最近一次"),

    /**
     * 丢弃所有misfire
     */
    DISCARD(2, "丢弃所有"),

    /**
     * 触发所有misfire
     */
    EVERY(3, "触发所有"),

    ;

    private final int value;
    private final String desc;

    MisfireStrategy(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public String desc() {
        return desc;
    }

    public static MisfireStrategy of(Integer value) {
        return Objects.requireNonNull(Const.MAPPING.get(value), () -> "Invalid misfire strategy value: " + value);
    }

    private static final class Const {
        private static final Map<Integer, MisfireStrategy> MAPPING = Enums.toMap(MisfireStrategy.class, MisfireStrategy::value);
    }

}
