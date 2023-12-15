/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.exception;

import cn.ponfee.disjob.common.exception.BaseRuntimeException;
import cn.ponfee.disjob.core.base.JobCodeMsg;

/**
 * Ket not exists exception
 *
 * @author Ponfee
 */
public class KeyNotExistsException extends BaseRuntimeException {
    private static final long serialVersionUID = -5304388166455122511L;

    public KeyNotExistsException() {
        super(JobCodeMsg.KEY_NOT_FOUND.getCode(), "Key already exists.");
    }

    public KeyNotExistsException(String message) {
        super(JobCodeMsg.KEY_NOT_FOUND.getCode(), message);
    }
}
