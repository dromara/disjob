/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.converter;

import cn.ponfee.disjob.core.base.SupervisorMetrics;
import cn.ponfee.disjob.core.base.WorkerMetrics;
import cn.ponfee.disjob.supervisor.application.response.SupervisorMetricsResponse;
import cn.ponfee.disjob.supervisor.application.response.WorkerMetricsResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Server metrics converter
 *
 * @author Ponfee
 */
@Mapper(uses = CommonMapper.class)
public interface ServerMetricsConverter {

    ServerMetricsConverter INSTANCE = Mappers.getMapper(ServerMetricsConverter.class);

    SupervisorMetricsResponse convert(SupervisorMetrics source);

    @Mapping(target = ".", source = "threadPool")
    WorkerMetricsResponse convert(WorkerMetrics source);

}
