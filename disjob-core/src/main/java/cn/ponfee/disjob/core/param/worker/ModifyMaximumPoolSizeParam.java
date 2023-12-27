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
 * Modify maximum pool size param
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class ModifyMaximumPoolSizeParam extends AuthenticationParam {
    private static final long serialVersionUID = 1023942345935168778L;

    private int maximumPoolSize;

    public ModifyMaximumPoolSizeParam(int maximumPoolSize, String supervisorToken) {
        this.maximumPoolSize = maximumPoolSize;
        super.setSupervisorToken(supervisorToken);
    }

}
