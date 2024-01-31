/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Supervisor metrics
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SupervisorMetrics extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -3330041841878987072L;

    /**
     * 使用的框架代码的版本号
     */
    private String version;

    /**
     * 启动时间
     */
    private Date startupAt;

    /**
     * 是否也是Worker角色
     */
    private boolean alsoWorker;

}
