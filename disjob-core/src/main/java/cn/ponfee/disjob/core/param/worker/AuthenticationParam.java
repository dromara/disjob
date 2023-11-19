/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param.worker;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Worker authentication param
 *
 * @author Ponfee
 */
@Getter
@Setter
public abstract class AuthenticationParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -6147946999789807933L;

    /**
     * 从Supervisor侧传过来的token
     */
    protected String supervisorToken;

}
