/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.converter;

import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.supervisor.application.request.AddSchedGroupRequest;
import cn.ponfee.disjob.supervisor.application.request.UpdateSchedGroupRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedGroupResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Sched group converter.
 *
 * @author Ponfee
 */
@Mapper
public interface SchedGroupConverter {

    SchedGroupConverter INSTANCE = Mappers.getMapper(SchedGroupConverter.class);

    SchedGroupResponse convert(SchedGroup schedGroup);

    SchedGroup convert(AddSchedGroupRequest addSchedGroupRequest);

    SchedGroup convert(UpdateSchedGroupRequest updateSchedGroupRequest);

}
