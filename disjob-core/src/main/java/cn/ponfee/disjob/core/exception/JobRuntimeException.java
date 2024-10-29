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

package cn.ponfee.disjob.core.exception;

import cn.ponfee.disjob.common.exception.BaseRuntimeException;
import cn.ponfee.disjob.common.model.CodeMsg;

/**
 * Job unchecked exception definition.
 *
 * @author Ponfee
 */
public class JobRuntimeException extends BaseRuntimeException {
    private static final long serialVersionUID = -5627922900462363679L;

    public JobRuntimeException(int code) {
        super(code, null);
    }

    public JobRuntimeException(int code, String message) {
        super(code, message);
    }

    public JobRuntimeException(CodeMsg cm) {
        super(cm.getCode(), cm.getMsg());
    }

    public JobRuntimeException(CodeMsg cm, String msg) {
        super(cm.getCode(), msg);
    }

    public JobRuntimeException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public JobRuntimeException(CodeMsg cm, Throwable cause) {
        super(cm.getCode(), cm.getMsg(), cause);
    }

    public JobRuntimeException(int code,
                               String message,
                               Throwable cause,
                               boolean enableSuppression,
                               boolean writableStackTrace) {
        super(code, message, cause, enableSuppression, writableStackTrace);
    }

}
