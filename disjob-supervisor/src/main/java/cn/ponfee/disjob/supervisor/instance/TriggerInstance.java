/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.instance;

import cn.ponfee.disjob.core.model.SchedInstance;
import lombok.Getter;

/**
 * Trigger instance
 *
 * @author Ponfee
 */
@Getter
public abstract class TriggerInstance {

    private final SchedInstance instance;

    public TriggerInstance(SchedInstance instance) {
        this.instance = instance;
    }
}
