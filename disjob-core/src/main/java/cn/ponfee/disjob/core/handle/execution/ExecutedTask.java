/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle.execution;

import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.model.SchedTask;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Executed task
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ExecutedTask extends AbstractExecutionTask {
    private static final long serialVersionUID = -4625053001297718912L;

    /**
     * 执行状态
     */
    private ExecuteState executeState;

    public static List<ExecutedTask> convert(List<SchedTask> tasks) {
        if (tasks == null) {
            return null;
        }
        return tasks.stream()
            .map(ExecutionTaskConverter.INSTANCE::toExecutedTask)
            .collect(Collectors.toList());
    }

}
