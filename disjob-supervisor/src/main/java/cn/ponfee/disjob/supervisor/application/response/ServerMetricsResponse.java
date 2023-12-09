/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.response;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Server metrics response
 *
 * @author Ponfee
 */
@Getter
@Setter
public abstract class ServerMetricsResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 2989558365810145061L;

    /**
     * Server host
     */
    private String host;

    /**
     * Server port
     */
    private int port;

    /**
     * 启动时间
     */
    private String startupAt;

    /**
     * Ping time milliseconds
     */
    private Long pingTime;

}
