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
 * Executing task failure then should be canceling.
 *
 * @author Ponfee
 */
public class CancelTaskException extends BaseCheckedException {
    private static final long serialVersionUID = -3461401416673580272L;

    public CancelTaskException(CodeMsg cm) {
        super(cm.getCode(), cm.getMsg());
    }

    public CancelTaskException(CodeMsg cm, String message) {
        super(cm.getCode(), message);
    }
}
