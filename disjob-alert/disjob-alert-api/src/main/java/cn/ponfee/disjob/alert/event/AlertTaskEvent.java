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

import java.util.Date;
import java.text.DateFormat;

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
        return String.format("%s [%s]",
            DateFormat.getDateTimeInstance().format(new Date()),
            this.getAlertType()
        );
    }

    @Override
    public String buildContent() {
        StringBuilder content = new StringBuilder();
        content.append("【任务告警通知】\n\n");

        // 任务基本信息
        content.append("任务ID：").append(this.taskId).append("\n");
        content.append("作业名称：").append(this.jobName).append("\n");
        content.append("作业ID：").append(this.jobId).append("\n");
        content.append("实例ID：").append(this.instanceId).append("\n");
        content.append("告警类型：").append(this.getAlertType()).append("\n");
        content.append("告警时间：").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n");

        // 执行信息
        if (this.worker != null) {
            content.append("执行节点：").append(this.worker).append("\n");
        }
        if (this.executeStartTime != null) {
            content.append("开始时间：").append(DateFormat.getDateTimeInstance().format(this.executeStartTime)).append("\n");
        }
        if (this.executeEndTime != null) {
            content.append("结束时间：").append(DateFormat.getDateTimeInstance().format(this.executeEndTime)).append("\n");
        }

        // 状态信息
        if (this.executeState != null) {
            content.append("执行状态：").append(this.executeState).append("\n");
        }
        if (this.errorMsg != null && !this.errorMsg.isEmpty()) {
            content.append("\n异常信息：\n").append(this.errorMsg);
        }

        return content.toString();
    }

}
