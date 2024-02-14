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

import cn.ponfee.disjob.core.model.SchedWorkflow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mybatis mapper of sched_workflow database table.
 *
 * @author Ponfee
 */
public interface SchedWorkflowMapper {

    int batchInsert(List<SchedWorkflow> records);

    List<SchedWorkflow> findByWnstanceId(long wnstanceId);

    int update(@Param("wnstanceId") long wnstanceId,
               @Param("curNode") String curNode,
               @Param("toState") Integer toState,
               @Param("toInstanceId") Long toInstanceId,
               @Param("fromStates") List<Integer> fromStates,
               @Param("fromInstanceId") Long fromInstanceId);

    int resumeWaiting(long wnstanceId);

    int deleteByWnstanceId(long wnstanceId);
}
