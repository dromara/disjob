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

import cn.ponfee.disjob.supervisor.application.request.SchedGroupAddRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupUpdateRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedGroupResponse;
import cn.ponfee.disjob.supervisor.model.SchedGroup;
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

    SchedGroupResponse convert(SchedGroup source);

    SchedGroup convert(SchedGroupAddRequest source);

    SchedGroup convert(SchedGroupUpdateRequest source);

}
