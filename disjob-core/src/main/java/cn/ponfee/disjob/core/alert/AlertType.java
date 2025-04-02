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

package cn.ponfee.disjob.core.alert;

import cn.ponfee.disjob.common.base.IntValueEnum;

import java.util.Arrays;

/**
 * Alert type, value must be a power of 2 number, such as: 1, 2, 4, 8, 16, ...
 *
 * @author Ponfee
 */
public enum AlertType implements IntValueEnum<AlertType> {

    /**
     * 异常警报
     */
    ALARM(1, "警报"),

    /**
     * 正常通知
     */
    NOTICE(2, "通知"),

    ;

    private final int value;
    private final String desc;

    AlertType(int value, String desc) {
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

    public boolean isAlarm() {
        return this == ALARM;
    }

    public boolean matches(Integer val) {
        // 通过位运算判断是否已匹配当前警告类型
        return val != null && (val & value) == value;
    }

    public static void check(Integer val) {
        // 0表示未选，MAX_VAL表示全选
        if (val == null || val < 0 || val > MAX_VAL) {
            throw new IllegalArgumentException("Invalid alert type val: " + val);
        }
    }

    private static final int MAX_VAL = Arrays.stream(values()).mapToInt(AlertType::value).reduce(0, (a, b) -> a ^ b);

}
