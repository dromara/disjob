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
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Execution task converter
 *
 * @author Ponfee
 */
@Mapper(uses = {ExecuteState.class})
public interface ExecutionTaskConverter {

    ExecutionTaskConverter INSTANCE = Mappers.getMapper(ExecutionTaskConverter.class);

    ExecutingTask toExecutingTask(SchedTask task);

    ExecutedTask toExecutedTask(SchedTask task);

}
