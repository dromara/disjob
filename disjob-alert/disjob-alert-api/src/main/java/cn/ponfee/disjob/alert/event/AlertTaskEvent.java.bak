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

import cn.ponfee.disjob.core.enums.ExecuteState;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Alert task event
 *
 * @author Ponfee
 */
@Getter
@Setter
public class AlertTaskEvent extends AlertEvent {
    private static final long serialVersionUID = 5550051265102992301L;

    /**
     * The task id
     */
    private long taskId;

    /**
     * 执行状态
     */
    private ExecuteState executeState;

    /**
     * 执行开始时间
     */
    private Date executeStartTime;

    /**
     * 执行结束时间
     */
    private Date executeEndTime;

    /**
     * Worker
     */
    private String worker;

    /**
     * 执行错误信息
     */
    private String errorMsg;

    @Override
    public String buildTitle() {
        return String.format("【任务-%s | %s】", alertType.desc(), formatDate(new Date()));
    }

    @Override
    public String buildContent(String format) {
        StringBuilder content = new StringBuilder();
        // 基本信息
        content.append(String.format(format, "分组名称：", group));
        content.append(String.format(format, "作业名称：", jobName));
        content.append(String.format(format, "作业ID：", jobId));
        content.append(String.format(format, "实例ID：", instanceId));
        content.append(String.format(format, "任务ID：", taskId));
        // 执行信息
        content.append(String.format(format, "执行状态：", executeState.desc()));
        content.append(String.format(format, "执行开始时间：", formatDate(executeStartTime)));
        content.append(String.format(format, "执行结束时间：", formatDate(executeEndTime)));
        content.append(String.format(format, "执行机器：", worker));
        if (StringUtils.isNotBlank(errorMsg)) {
            content.append(String.format(format, "异常信息：", errorMsg));
        }
        return content.toString();
    }

}
