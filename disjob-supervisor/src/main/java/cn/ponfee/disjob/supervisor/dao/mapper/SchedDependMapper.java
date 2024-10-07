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

package cn.ponfee.disjob.supervisor.dao.mapper;

import cn.ponfee.disjob.supervisor.model.SchedDepend;

import java.util.List;
import java.util.Set;

/**
 * Mybatis mapper of sched_depend database table.
 *
 * @author Ponfee
 */
public interface SchedDependMapper {

    int batchInsert(List<SchedDepend> records);

    List<SchedDepend> findByParentJobId(long parentJobId);

    List<SchedDepend> findByChildJobIds(Set<Long> childJobIds);

    int deleteByParentJobId(long parentJobId);

    int deleteByChildJobId(long parentJobId);

}
