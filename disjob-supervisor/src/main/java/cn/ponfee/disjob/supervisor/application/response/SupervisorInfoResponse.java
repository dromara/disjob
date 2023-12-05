/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Supervisor info response
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SupervisorInfoResponse extends ServerInfoResponse {
    private static final long serialVersionUID = 1128162112784547814L;

    private Boolean isWorker;
}
