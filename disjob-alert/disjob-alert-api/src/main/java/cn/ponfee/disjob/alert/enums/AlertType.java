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

package cn.ponfee.disjob.alert.enums;

/**
 * Alert type
 *
 * @author Ponfee
 */
public enum AlertType {

    /**
     * 异常警报
     */
    ALARM("警报"),

    /**
     * 正常通知
     */
    NOTICE("通知"),

    ;

    private final String desc;

    AlertType(String desc) {
        this.desc = desc;
    }

    public String desc() {
        return desc;
    }

}
