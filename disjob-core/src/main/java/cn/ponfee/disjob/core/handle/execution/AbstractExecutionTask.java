/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle.execution;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Abstract execution task
 *
 * @author Ponfee
 */
@Getter
@Setter
public abstract class AbstractExecutionTask implements Serializable {
    private static final long serialVersionUID = 6002495716472663520L;

    /**
     * 全局唯一ID
     */
    private Long taskId;

    /**
     * 任务序号(从1开始)
     */
    private Integer taskNo;

    /**
     * 任务总数量
     */
    private Integer taskCount;

    /**
     * 保存的执行快照数据
     */
    private String executeSnapshot;

}
