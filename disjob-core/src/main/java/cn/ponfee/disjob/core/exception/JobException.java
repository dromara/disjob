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

import cn.ponfee.disjob.common.exception.BaseException;
import cn.ponfee.disjob.common.model.CodeMsg;

/**
 * Job checked exception definition.
 *
 * @author Ponfee
 */
public class JobException extends BaseException {
    private static final long serialVersionUID = -6568546076593428337L;

    public JobException(int code) {
        super(code, null);
    }

    public JobException(int code, String message) {
        super(code, message);
    }

    public JobException(CodeMsg cm) {
        super(cm.getCode(), cm.getMsg());
    }

    public JobException(CodeMsg cm, String msg) {
        super(cm.getCode(), msg);
    }

    public JobException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public JobException(CodeMsg cm, Throwable cause) {
        super(cm.getCode(), cm.getMsg(), cause);
    }

    public JobException(int code,
                        String message,
                        Throwable cause,
                        boolean enableSuppression,
                        boolean writableStackTrace) {
        super(code, message, cause, enableSuppression, writableStackTrace);
    }

}
