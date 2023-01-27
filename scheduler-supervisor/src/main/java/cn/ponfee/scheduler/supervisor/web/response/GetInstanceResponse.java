/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.web.response;

import cn.ponfee.scheduler.common.base.ToJsonString;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.model.SchedInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * Get schedule instance response structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetInstanceResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 3495155528208478258L;

    private SchedInstance instance;
    private List<SchedTask> tasks;
}
