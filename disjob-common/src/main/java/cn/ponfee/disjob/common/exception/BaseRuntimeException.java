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

package cn.ponfee.disjob.common.exception;

import cn.ponfee.disjob.common.model.CodeMsg;

/**
 * Base unchecked(runtime) exception definition(运行时异常)
 *
 * @author Ponfee
 */
public abstract class BaseRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 5960744547215706709L;

    /**
     * Error code
     */
    private final int code;

    protected BaseRuntimeException(int code) {
        this(code, null, null);
    }

    protected BaseRuntimeException(CodeMsg codeMsg) {
        this(codeMsg.getCode(), codeMsg.getMsg(), null);
    }

    /**
     * @param code    error code
     * @param message error message
     */
    protected BaseRuntimeException(int code, String message) {
        this(code, message, null);
    }

    protected BaseRuntimeException(CodeMsg codeMsg, Throwable cause) {
        this(codeMsg.getCode(), codeMsg.getMsg(), cause);
    }

    /**
     * @param code    error code
     * @param message error message
     * @param cause   root cause
     */
    protected BaseRuntimeException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * @param code               error code
     * @param message            error message
     * @param cause              root cause
     * @param enableSuppression  the enableSuppression
     * @param writableStackTrace then writableStackTrace
     */
    protected BaseRuntimeException(int code,
                                   String message,
                                   Throwable cause,
                                   boolean enableSuppression,
                                   boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }

    /**
     * Returns the error code
     *
     * @return int value of error code
     */
    public int getCode() {
        return code;
    }

}
