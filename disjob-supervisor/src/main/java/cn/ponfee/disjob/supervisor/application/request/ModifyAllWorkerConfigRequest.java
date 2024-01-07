/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Modify all worker config
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ModifyAllWorkerConfigRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -2430927428049714711L;

    private String group;
    private int maximumPoolSize;

    public void check() {
        Assert.isTrue(
            maximumPoolSize > 0 && maximumPoolSize <= ThreadPoolExecutors.MAX_CAP,
            () -> "Worker maximum pool size must be range [1, " + ThreadPoolExecutors.MAX_CAP + "]."
        );
    }
}
