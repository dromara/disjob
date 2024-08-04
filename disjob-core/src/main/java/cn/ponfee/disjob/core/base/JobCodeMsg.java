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

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.model.CodeMsg;
import cn.ponfee.disjob.common.util.Enums;

/**
 * Job code message definitions.
 *
 * @author Ponfee
 */
@SuppressWarnings("all")
public enum JobCodeMsg implements CodeMsg {

    INVALID_PARAM(400, "Invalid param."),
    UN_AUTHENTICATED(401, "Un authenticated."),
    KEY_NOT_FOUND(404, "Key not found."),
    LOAD_JOB_EXECUTOR_ERROR(450, "Load job executor error."),
    INVALID_JOB_EXECUTOR(451, "Invalid job executor."),
    SPLIT_JOB_FAILED(452, "Split job failed."),
    NOT_PAUSABLE_INSTANCE(453, "Not pausable instance state."),
    NOT_CANCELABLE_INSTANCE(454, "Not cancelable instance state."),
    NOT_RESUMABLE_INSTANCE(455, "Not resumable instance state."),

    SERVER_ERROR(500, "Server error."),
    NOT_DISCOVERED_WORKER(550, "Not Discovered worker."),
    JOB_EXECUTE_FAILED(561, "Job execute failed."),
    JOB_EXECUTE_ERROR(562, "Job execute error."),

    ;

    private final int code;
    private final String msg;

    JobCodeMsg(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    static {
        Enums.checkDuplicated(JobCodeMsg.class, JobCodeMsg::getCode);
    }

}
