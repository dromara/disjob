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

package cn.ponfee.disjob.dispatch;

import java.util.StringJoiner;

/**
 * Dispatch task param
 *
 * @author Ponfee
 */
class DispatchTaskParam {

    private final ExecuteTaskParam task;
    private final String group;
    private int retried = 0;

    public DispatchTaskParam(ExecuteTaskParam task, String group) {
        this.task = task;
        this.group = group;
    }

    public ExecuteTaskParam task() {
        return task;
    }

    public String group() {
        return group;
    }

    public int retrying() {
        return ++this.retried;
    }

    public int retried() {
        return retried;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DispatchTaskParam.class.getSimpleName() + "[", "]")
            .add("task=" + task)
            .add("group=" + (group != null ? "'" + group + "'" : "null"))
            .add("retried=" + retried)
            .toString();
    }

}
