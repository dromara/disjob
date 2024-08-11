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
import lombok.Getter;

import java.beans.Transient;
import java.io.Serializable;

/**
 * Job execution result
 *
 * @author Ponfee
 */
@Getter
public class ExecutionResult implements Serializable {

    private static final long serialVersionUID = -6336359114514174838L;
    private static final ExecutionResult SUCCESS = new ExecutionResult(0, "OK");

    /**
     * The code
     */
    private final int code;

    /**
     * The message
     */
    private final String msg;

    private ExecutionResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Transient
    public boolean isSuccess() {
        return this.code == SUCCESS.code;
    }

    @Transient
    public boolean isFailure() {
        return !isSuccess();
    }

    // -----------------------------------------------static methods

    public static ExecutionResult success() {
        return SUCCESS;
    }

    public static ExecutionResult success(String msg) {
        return new ExecutionResult(SUCCESS.code, msg);
    }

    public static ExecutionResult failure(CodeMsg cm) {
        return failure(cm.getCode(), cm.getMsg());
    }

    public static ExecutionResult failure(int code, String msg) {
        if (code == SUCCESS.code) {
            throw new IllegalStateException("Invalid execution result failure code: " + code);
        }
        return new ExecutionResult(code, msg);
    }

    @Override
    public String toString() {
        return code + ": " + msg;
    }

}
