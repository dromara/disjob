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

package cn.ponfee.disjob.worker.executor;

import cn.ponfee.disjob.common.model.CodeMsg;
import cn.ponfee.disjob.common.model.Result;

/**
 * Job execution result
 *
 * @author Ponfee
 */
public class ExecutionResult extends Result.ImmutableResult<Void> {
    private static final long serialVersionUID = -6336359114514174838L;
    private static final ExecutionResult SUCCESS = new ExecutionResult(Result.success().getCode(), Result.success().getMsg());

    private ExecutionResult(int code, String msg) {
        super(code, msg, null);
    }

    // -----------------------------------------------static success methods

    public static ExecutionResult success() {
        return SUCCESS;
    }

    public static ExecutionResult success(String msg) {
        return new ExecutionResult(SUCCESS.getCode(), msg);
    }

    // -----------------------------------------------static failure methods

    public static ExecutionResult failure(CodeMsg cm) {
        return failure(cm.getCode(), cm.getMsg());
    }

    public static ExecutionResult failure(int code, String msg) {
        if (code == SUCCESS.getCode()) {
            throw new IllegalStateException("Execution result failure code '" + code + "' cannot be '" + SUCCESS.getCode() + "'.");
        }
        return new ExecutionResult(code, msg);
    }

    @Override
    public Void getData() {
        throw new UnsupportedOperationException("Execution result unsupported data.");
    }

}
