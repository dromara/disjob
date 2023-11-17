/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.model.CodeMsg;

/**
 * Job code message definitions.
 *
 * @author Ponfee
 */
public enum JobCodeMsg implements CodeMsg {

    INVALID_PARAM(400, "Invalid param."),
    UN_AUTHENTICATED(401, "Un authenticated."),
    SERVER_ERROR(500, "Server error."),

    LOAD_HANDLER_ERROR(1001, "Load job handler error."),
    INVALID_JOB_HANDLER(1002, "Invalid job handler."),
    SPLIT_JOB_FAILED(1003, "Split job failed."),
    NOT_DISCOVERED_WORKER(1004, "Not Discovered worker."),

    JOB_EXECUTE_FAILED(2001, "Job execute failed."),
    JOB_EXECUTE_ERROR(2002, "Job execute failed."),
    PAUSE_TASK_EXCEPTION(2003, "Pause task exception."),
    CANCEL_TASK_EXCEPTION(2004, "Cancel task exception."),

    NOT_PAUSABLE_INSTANCE(3005, "Not pausable instance state."),
    NOT_CANCELABLE_INSTANCE(3006, "Not cancelable instance state."),
    NOT_RESUMABLE_INSTANCE(3007, "Not resumable instance state."),

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

}
