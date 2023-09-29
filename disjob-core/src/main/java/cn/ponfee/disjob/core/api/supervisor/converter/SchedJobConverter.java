/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.api.supervisor.converter;

import cn.ponfee.disjob.core.api.supervisor.request.AddSchedJobRequest;
import cn.ponfee.disjob.core.api.supervisor.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.core.api.supervisor.response.SchedInstanceResponse;
import cn.ponfee.disjob.core.api.supervisor.response.SchedJobResponse;
import cn.ponfee.disjob.core.api.supervisor.response.SchedTaskResponse;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Sched job converter.
 *
 * @author Ponfee
 */
@Mapper
public interface SchedJobConverter {

    SchedJobConverter INSTANCE = Mappers.getMapper(SchedJobConverter.class);

    SchedJob convert(AddSchedJobRequest req);

    SchedJob convert(UpdateSchedJobRequest req);

    SchedJobResponse convert(SchedJob schedJob);

    SchedInstanceResponse convert(SchedInstance schedInstance);

    SchedTaskResponse convert(SchedTask schedTask);

}
