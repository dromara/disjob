/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.exception;

import cn.ponfee.scheduler.common.base.exception.BaseCheckedException;
import cn.ponfee.scheduler.common.base.model.CodeMsg;

/**
 * Executing task failure then should be pausing.
 *
 * @author Ponfee
 */
public class PauseTaskException extends BaseCheckedException {
    private static final long serialVersionUID = 409247238969878885L;

    public PauseTaskException(CodeMsg cm) {
        super(cm.getCode(), cm.getMsg());
    }

    public PauseTaskException(CodeMsg cm, String message) {
        super(cm.getCode(), message);
    }
}
