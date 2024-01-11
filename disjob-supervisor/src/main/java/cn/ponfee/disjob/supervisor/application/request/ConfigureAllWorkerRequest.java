/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam.Action;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Configure all worker request
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ConfigureAllWorkerRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -2430927428049714711L;

    private String group;

    private Action action;
    private String data;

}
