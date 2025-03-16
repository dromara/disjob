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
    public String buildContent(String indent, String lineSeparator) {
        StringBuilder content = new StringBuilder();
        // 基本信息
        content.append(indent).append("分组名称：").append(group).append(lineSeparator);
        content.append(indent).append("作业名称：").append(jobName).append(lineSeparator);
        content.append(indent).append("作业ID：").append(jobId).append(lineSeparator);
        content.append(indent).append("实例ID：").append(instanceId).append(lineSeparator);
        // 运行信息
        content.append(indent).append("运行类型：").append(runType.desc()).append(lineSeparator);
        content.append(indent).append("运行状态：").append(runState.desc()).append(lineSeparator);
        content.append(indent).append("计划触发时间：").append(formatDate(triggerTime)).append(lineSeparator);
        content.append(indent).append("运行开始时间：").append(formatDate(runStartTime)).append(lineSeparator);
        content.append(indent).append("运行结束时间：").append(formatDate(runEndTime)).append(lineSeparator);
        if (retriedCount > 0) {
            content.append(indent).append("已重试的次数：").append(retriedCount).append(lineSeparator);
        }
        return content.toString();
    }

}
