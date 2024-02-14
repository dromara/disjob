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

import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.core.model.TokenType;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mybatis mapper of sched_group database table.
 *
 * @author Ponfee
 */
public interface SchedGroupMapper {

    int insert(SchedGroup schedGroup);

    SchedGroup get(String group);

    List<SchedGroup> findAll();

    int updateToken(@Param("group") String group,
                    @Param("type") TokenType type,
                    @Param("newToken") String newToken,
                    @Param("updatedBy") String updatedBy,
                    @Param("oldToken") String oldToken);

    int updateOwnUser(@Param("group") String group,
                      @Param("ownUser") String ownUser,
                      @Param("updatedBy") String updatedBy);

    int softDelete(@Param("group") String group,
                   @Param("updatedBy") String updatedBy);

    int edit(SchedGroup schedGroup);

    boolean exists(String group);

    List<String> searchGroup(String term);

    // -------------------------------------------------query for page

    long queryPageCount(SchedGroupPageRequest request);

    List<SchedGroup> queryPageRecords(SchedGroupPageRequest request);
}
