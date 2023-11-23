/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.exception;

import cn.ponfee.disjob.common.exception.BaseException;
import cn.ponfee.disjob.core.base.JobCodeMsg;

/**
 * Executing task failure then should be pausing.
 *
 * @author Ponfee
 */
public class PauseTaskException extends BaseException {
    private static final long serialVersionUID = 409247238969878885L;

    public PauseTaskException() {
        super(JobCodeMsg.PAUSE_TASK_EXCEPTION);
    }
}
