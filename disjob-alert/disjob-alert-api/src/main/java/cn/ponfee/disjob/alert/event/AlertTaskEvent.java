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
    public String buildContent(String indent, String lineSeparator) {
        StringBuilder content = new StringBuilder();
        // 基本信息
        content.append(indent).append("分组名称：").append(group).append(lineSeparator);
        content.append(indent).append("作业名称：").append(jobName).append(lineSeparator);
        content.append(indent).append("作业ID：").append(jobId).append(lineSeparator);
        content.append(indent).append("实例ID：").append(instanceId).append(lineSeparator);
        content.append(indent).append("任务ID：").append(taskId).append(lineSeparator);
        // 执行信息
        content.append(indent).append("执行状态：").append(executeState.desc()).append(lineSeparator);
        content.append(indent).append("执行开始时间：").append(formatDate(executeStartTime)).append(lineSeparator);
        content.append(indent).append("执行结束时间：").append(formatDate(executeEndTime)).append(lineSeparator);
        content.append(indent).append("执行机器：").append(worker).append(lineSeparator);
        if (StringUtils.isNotEmpty(errorMsg)) {
            content.append(indent).append("异常信息：").append(errorMsg).append(lineSeparator);
        }
        return content.toString();
    }

}
