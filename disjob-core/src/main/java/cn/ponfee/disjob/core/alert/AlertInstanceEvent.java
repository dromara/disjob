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

package cn.ponfee.disjob.core.alert;

import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.enums.RunType;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Alert instance event
 *
 * @author Ponfee
 */
@Getter
@Setter
public class AlertInstanceEvent extends AlertEvent {
    private static final long serialVersionUID = 5213948727010283020L;

    /**
     * The job name
     */
    private String jobName;

    /**
     * The job id
     */
    private long jobId;

    /**
     * The instance id
     */
    private long instanceId;

    /**
     * 运行类型
     */
    private RunType runType;

    /**
     * 运行状态
     */
    private RunState runState;

    /**
     * 触发时间
     */
    private Date triggerTime;

    /**
     * 运行开始时间
     */
    private Date runStartTime;

    /**
     * 运行结束时间
     */
    private Date runEndTime;

    /**
     * 已重试的次数
     */
    private int retriedCount;

    @Override
    public String buildTitle() {
        return String.format("【实例-%s | %s】", alertType.desc(), formatDate(new Date()));
    }

    @Override
    public String buildContent(String format) {
        StringBuilder content = new StringBuilder();
        // 基本信息
        content.append(String.format(format, "分组名称：", group));
        content.append(String.format(format, "作业名称：", jobName));
        content.append(String.format(format, "作业ID：", jobId));
        content.append(String.format(format, "实例ID：", instanceId));
        // 运行信息
        content.append(String.format(format, "运行类型：", runType.desc()));
        content.append(String.format(format, "运行状态：", runState.desc()));
        content.append(String.format(format, "计划触发时间：", formatDate(triggerTime)));
        content.append(String.format(format, "运行开始时间：", formatDate(runStartTime)));
        content.append(String.format(format, "运行结束时间：", formatDate(runEndTime)));
        if (retriedCount > 0) {
            content.append(String.format(format, "已重试的次数：", retriedCount));
        }
        return content.toString();
    }

}
