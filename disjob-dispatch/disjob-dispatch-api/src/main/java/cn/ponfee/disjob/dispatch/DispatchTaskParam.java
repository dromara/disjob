/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch;

import java.util.StringJoiner;

/**
 * Dispatch task param
 *
 * @author Ponfee
 */
class DispatchTaskParam {

    private final ExecuteTaskParam task;
    private final String group;
    private int retried = 0;

    public DispatchTaskParam(ExecuteTaskParam task, String group) {
        this.task = task;
        this.group = group;
    }

    public ExecuteTaskParam task() {
        return task;
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
        return new StringJoiner(", ", DispatchTaskParam.class.getSimpleName() + "[", "]")
            .add("task=" + task)
            .add("group=" + (group != null ? "'" + group + "'" : "null"))
            .add("retried=" + retried)
            .toString();
    }

}
