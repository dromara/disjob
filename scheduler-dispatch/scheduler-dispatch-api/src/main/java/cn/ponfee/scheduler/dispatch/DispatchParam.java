/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.dispatch;

import cn.ponfee.scheduler.core.param.ExecuteTaskParam;

import java.util.StringJoiner;

/**
 * Dispatch param
 *
 * @author Ponfee
 */
class DispatchParam {

    private final ExecuteTaskParam executeTaskParam;
    private final String group;
    private int retried = 0;

    public DispatchParam(ExecuteTaskParam executeTaskParam, String group) {
        this.executeTaskParam = executeTaskParam;
        this.group = group;
    }

    public ExecuteTaskParam executeTaskParam() {
        return executeTaskParam;
    }

    public String group() {
        return group;
    }

    public void retrying() {
        this.retried++;
    }

    public int retried() {
        return retried;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DispatchParam.class.getSimpleName() + "[", "]")
            .add("executeTaskParam=" + executeTaskParam)
            .add(group == null ? "group=null" : "group='" + group + "'")
            .add("retried=" + retried)
            .toString();
    }

}
