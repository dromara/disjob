/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.common.model.CodeMsg;
import cn.ponfee.disjob.common.model.Result;

/**
 * Job handler execute result
 *
 * @author Ponfee
 */
public class ExecuteResult extends Result.ImmutableResult<Void> {
    private static final long serialVersionUID = -6336359114514174838L;
    private static final ExecuteResult SUCCESS = new ExecuteResult(Result.success().getCode(), Result.success().getMsg());

    private ExecuteResult(int code, String msg) {
        super(code, msg, null);
    }

    // -----------------------------------------------static success methods

    public static ExecuteResult success() {
        return SUCCESS;
    }

    public static ExecuteResult success(String msg) {
        return new ExecuteResult(SUCCESS.getCode(), msg);
    }

    // -----------------------------------------------static failure methods

    public static ExecuteResult failure(CodeMsg cm) {
        return failure(cm.getCode(), cm.getMsg());
    }

    public static ExecuteResult failure(int code, String msg) {
        if (code == SUCCESS.getCode()) {
            throw new IllegalStateException("Execute result failure code '" + code + "' cannot be '" + SUCCESS.getCode() + "'.");
        }
        return new ExecuteResult(code, msg);
    }

    @Override
    public Void getData() {
        throw new UnsupportedOperationException("Execute result unsupported data.");
    }

}
