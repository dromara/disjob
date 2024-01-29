/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Worker metrics response
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkerMetricsResponse extends ServerMetricsResponse {
    private static final long serialVersionUID = -8325148543854446360L;

    private String workerId;

    private Boolean alsoSupervisor;
    private Integer jvmThreadActiveCount;
    private Boolean closed;
    private Long keepAliveTime;
    private Integer maximumPoolSize;
    private Integer currentPoolSize;
    private Integer activePoolSize;
    private Integer idlePoolSize;
    private Long queueTaskCount;
    private Long completedTaskCount;

    public WorkerMetricsResponse(String workerId) {
        this.workerId = workerId;
    }

}
