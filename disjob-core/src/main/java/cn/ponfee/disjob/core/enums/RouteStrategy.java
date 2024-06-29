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
 * The route strategy enum definition.
 * <p>mapped by sched_job.route_strategy
 *
 * @author Ponfee
 */
public enum RouteStrategy implements IntValueEnum<RouteStrategy> {

    /**
     * 轮询
     */
    ROUND_ROBIN(1, "轮询"),

    /**
     * 随机
     */
    RANDOM(2, "随机"),

    /**
     * 简单哈希
     */
    SIMPLE_HASH(3, "简单哈希"),

    /**
     * 一致性哈希
     */
    CONSISTENT_HASH(4, "一致性哈希"),

    /**
     * 本地优先(当supervisor同时也是worker角色时生效)
     */
    LOCAL_PRIORITY(5, "本地优先"),

    /**
     * 广播
     */
    BROADCAST(6, "广播"),

    ;

    private final int value;
    private final String desc;

    RouteStrategy(int value, String desc) {
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

    public boolean isBroadcast() {
        return this == BROADCAST;
    }

    public boolean isNotBroadcast() {
        return !isBroadcast();
    }

    public boolean isRoundRobin() {
        return this == ROUND_ROBIN;
    }

    public boolean isNotRoundRobin() {
        return !isRoundRobin();
    }

    public static RouteStrategy of(Integer value) {
        return Objects.requireNonNull(Const.MAPPING.get(value), () -> "Invalid route strategy value: " + value);
    }

    private static final class Const {
        private static final Map<Integer, RouteStrategy> MAPPING = Enums.toMap(RouteStrategy.class, RouteStrategy::value);
    }

}
