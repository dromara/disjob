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

/**
 * The misfire strategy enum definition.
 * <p>mapped by sched_job.misfire_strategy
 *
 * @author Ponfee
 */
public enum MisfireStrategy implements IntValueEnum<MisfireStrategy> {

    /**
     * 立即触发执行一次，之后按正常的调度时间点执行
     */
    FIRE_ONCE_NOW(1, "立即触发执行一次"),

    /**
     * 跳过所有被错过的，等待下一次的调度时间点执行
     */
    SKIP_ALL_LOST(2, "跳过所有被错过的"),

    /**
     * 执行所有被错过的，之后按正常的调度时间点执行
     */
    FIRE_ALL_LOST(3, "执行所有被错过的"),

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
        return IntValueEnum.of(MisfireStrategy.class, value);
    }

}
