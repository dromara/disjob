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
        super(cm);
    }

    public CancelTaskException(CodeMsg cm, String message) {
        super(cm, message);
    }
}
