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

package cn.ponfee.disjob.common.date;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Date period config value.
 *
 * @author Ponfee
 */
@Setter
@Getter
public class DatePeriodValue extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -8395535372974631095L;

    /**
     * Period type
     */
    private DatePeriods period;

    /**
     * Calculate start date time
     */
    private Date start;

    /**
     * Period step
     */
    private int step = 1;

    public boolean verify() {
        return period != null && start != null && step > 0;
    }

}
