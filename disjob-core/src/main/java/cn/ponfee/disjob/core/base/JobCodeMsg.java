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
    SERVER_ERROR(500, "Server error."),

    LOAD_HANDLER_ERROR(1001, "Load job handler error."),
    SPLIT_JOB_FAILED(1002, "Split job failed."),
    NOT_DISCOVERED_WORKER(1003, "Not Discovered worker."),

    JOB_EXECUTE_FAILED(2001, "Job execute failed."),
    PAUSE_TASK_EXCEPTION(2002, "Pause task exception."),
    CANCEL_TASK_EXCEPTION(2003, "Cancel task exception."),

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
