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

package cn.ponfee.disjob.core.model;

import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The schedule job entity, mapped database table sched_depend
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class SchedDepend extends BaseEntity {
    private static final long serialVersionUID = 8880747435878186418L;

    /**
     * 父job_id
     */
    private Long parentJobId;

    /**
     * 子job_id
     */
    private Long childJobId;

    /**
     * 序号(从1开始)
     */
    private Integer sequence;

    public SchedDepend(Long parentJobId, Long childJobId, Integer sequence) {
        this.parentJobId = parentJobId;
        this.childJobId = childJobId;
        this.sequence = sequence;
    }

    public static List<Long> parseTriggerValue(String triggerValue) {
        return Arrays.stream(triggerValue.split(Str.COMMA))
            .filter(StringUtils::isNotBlank)
            .map(e -> Long.parseLong(e.trim()))
            .distinct()
            .collect(Collectors.toList());
    }

}
