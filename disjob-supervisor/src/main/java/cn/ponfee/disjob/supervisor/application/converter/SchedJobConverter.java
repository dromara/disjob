/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor.application.converter;

import cn.ponfee.disjob.supervisor.application.request.SchedJobAddRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobUpdateRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedTaskResponse;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Sched job converter.
 *
 * @author Ponfee
 */
@Mapper
public interface SchedJobConverter {

    SchedJobConverter INSTANCE = Mappers.getMapper(SchedJobConverter.class);

    SchedJob convert(SchedJobAddRequest source);

    SchedJob convert(SchedJobUpdateRequest source);

    SchedJobResponse convert(SchedJob source);

    @Mapping(target = "runDuration", expression = "java( CommonConverter.duration(source.getRunStartTime(), source.getRunEndTime()) )")
    SchedInstanceResponse convert(SchedInstance source);

    @Mapping(target = "executeDuration", expression = "java( CommonConverter.duration(source.getExecuteStartTime(), source.getExecuteEndTime()) )")
    SchedTaskResponse convert(SchedTask source);

}
