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
 * The collided strategy enum definition.
 * <p>mapped by sched_job.collided_strategy
 *
 * @author Ponfee
 */
public enum CollidedStrategy implements IntValueEnum<CollidedStrategy> {

    /**
     * 可同时并发执行(并行Parallel)
     */
    CONCURRENT(1, "并发执行"),

    /**
     * 按顺序依次执行(串行Serial)
     */
    SEQUENTIAL(2, "顺序执行"),

    /**
     * 覆盖上次任务(取消上次任务，执行本次任务)
     */
    OVERRIDE(3, "覆盖上次任务"),

    /**
     * 丢弃本次任务
     */
    DISCARD(4, "丢弃本次任务"),

    ;

    private final int value;
    private final String desc;

    CollidedStrategy(int value, String desc) {
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

    public static CollidedStrategy of(int value) {
        for (CollidedStrategy e : VALUES) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid collided strategy value: " + value);
    }

    private static final CollidedStrategy[] VALUES = CollidedStrategy.values();

}
