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
import cn.ponfee.disjob.core.base.JobCodeMsg;

/**
 * Executing task failure then should be canceling.
 *
 * @author Ponfee
 */
public class CancelTaskException extends BaseException {
    private static final long serialVersionUID = -3461401416673580272L;

    public CancelTaskException() {
        super(JobCodeMsg.CANCEL_TASK_EXCEPTION);
    }
}
