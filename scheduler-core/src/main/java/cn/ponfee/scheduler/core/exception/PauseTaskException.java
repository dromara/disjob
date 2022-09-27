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
        super(cm);
    }

    public PauseTaskException(CodeMsg cm, String message) {
        super(cm, message);
    }
}
