/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param.worker;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Get worker metrics param
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class GetMetricsParam extends AuthenticationParam {

    private static final long serialVersionUID = 6100003437491314940L;

    public GetMetricsParam(String supervisorToken) {
        super.setSupervisorToken(supervisorToken);
    }

}
