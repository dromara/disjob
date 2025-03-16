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

package cn.ponfee.disjob.alert.event;

import cn.ponfee.disjob.alert.enums.AlertType;
import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.common.date.Dates;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Alert event
 *
 * @author Ponfee
 */
@Getter
@Setter
public abstract class AlertEvent extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 6379056446763475308L;

    /**
     * The alert type
     */
    protected AlertType alertType;

    /**
     * The group
     */
    protected String group;

    /**
     * The job name
     */
    protected String jobName;

    /**
     * The job id
     */
    protected long jobId;

    /**
     * The instance id
     */
    protected long instanceId;

    /**
     * Build alert title.
     *
     * @return alert title
     */
    public abstract String buildTitle();

    /**
     * Build alert content.
     *
     * @param indent        the indent
     * @param lineSeparator the line separator
     * @return content
     */
    public abstract String buildContent(String indent, String lineSeparator);

    public String buildRateLimitKey() {
        return jobId + ":" + alertType;
    }

    public static String formatDate(Date date) {
        return (date == null) ? "" : Dates.format(date);
    }

}
