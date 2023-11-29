/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.model.CodeMsg;
import cn.ponfee.disjob.common.util.Enums;

/**
 * Job code message definitions.
 *
 * @author Ponfee
 */
public enum JobCodeMsg implements CodeMsg {

    INVALID_PARAM(400, "Invalid param."),
    UN_AUTHENTICATED(401, "Un authenticated."),
    GROUP_NOT_FOUND(404, "Worker group not found."),
    LOAD_HANDLER_ERROR(450, "Load job handler error."),
    INVALID_JOB_HANDLER(451, "Invalid job handler."),
    SPLIT_JOB_FAILED(452, "Split job failed."),
    NOT_PAUSABLE_INSTANCE(453, "Not pausable instance state."),
    NOT_CANCELABLE_INSTANCE(454, "Not cancelable instance state."),
    NOT_RESUMABLE_INSTANCE(455, "Not resumable instance state."),

    SERVER_ERROR(500, "Server error."),
    NOT_DISCOVERED_WORKER(550, "Not Discovered worker."),
    JOB_EXECUTE_FAILED(561, "Job execute failed."),
    JOB_EXECUTE_ERROR(562, "Job execute error."),
    PAUSE_TASK_EXCEPTION(563, "Pause task when exception."),
    CANCEL_TASK_EXCEPTION(564, "Cancel task when exception."),

    ;

    static {
        Enums.checkDuplicated(JobCodeMsg.class, JobCodeMsg::getCode);
    }

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

}
