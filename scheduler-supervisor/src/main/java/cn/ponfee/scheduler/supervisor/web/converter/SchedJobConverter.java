/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.web.converter;

import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.supervisor.web.request.AddSchedJobRequest;
import cn.ponfee.scheduler.supervisor.web.request.UpdateSchedJobRequest;
import cn.ponfee.scheduler.supervisor.web.response.SchedInstanceResponse;
import cn.ponfee.scheduler.supervisor.web.response.SchedJobResponse;
import cn.ponfee.scheduler.supervisor.web.response.SchedTaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Scheduler job converter.
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
