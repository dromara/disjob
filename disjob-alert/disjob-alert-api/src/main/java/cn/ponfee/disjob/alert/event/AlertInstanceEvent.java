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
import java.text.DateFormat;

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
        return String.format("%s [%s]",
            DateFormat.getDateTimeInstance().format(new Date()),
            this.getAlertType()
        );
    }

    @Override
    public String buildContent() {
        StringBuilder content = new StringBuilder();
        // 使用大标题
        content.append("# 【实例告警通知】\n\n");

        // 使用分组标题
        content.append("## 基本信息\n");
        content.append("作业名称：").append(this.jobName != null ? this.jobName : "-").append("\n");
        content.append("作业ID：").append(this.jobId).append("\n");
        content.append("实例ID：").append(this.instanceId).append("\n");
        content.append("告警类型：**").append(this.getAlertType()).append("**\n");
        content.append("告警时间：").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n");

        // 分隔线
        content.append("\n---\n");

        // 运行信息
        content.append("## 运行信息\n");
        content.append("运行类型：").append(this.runType != null ? this.runType : "-").append("\n");
        content.append("运行状态：").append(this.runState != null ? "**" + this.runState + "**" : "-").append("\n");
        if (this.retriedCount > 0) {
            content.append("重试次数：**").append(this.retriedCount).append("**\n");
        }

        // 分隔线
        content.append("\n---\n");

        // 时间信息
        content.append("## 时间信息\n");
        content.append("触发时间：").append(this.triggerTime != null
            ? DateFormat.getDateTimeInstance().format(this.triggerTime) : "-").append("\n");
        content.append("开始时间：").append(this.runStartTime != null
            ? DateFormat.getDateTimeInstance().format(this.runStartTime) : "-").append("\n");
        content.append("结束时间：").append(this.runEndTime != null
            ? DateFormat.getDateTimeInstance().format(this.runEndTime) : "-").append("\n");

        // 处理特殊字符，确保消息格式正确
        return content.toString()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

}
