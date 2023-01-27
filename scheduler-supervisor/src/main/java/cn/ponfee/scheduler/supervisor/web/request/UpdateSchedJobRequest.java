/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.web.request;

import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.supervisor.web.converter.SchedJobConverter;
import lombok.Getter;
import lombok.Setter;

/**
 * Update sched job request parameter structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class UpdateSchedJobRequest extends AddSchedJobRequest {
    private static final long serialVersionUID = -1481890923435762900L;

    private Long jobId;
    private Integer version;

    @Override
    public SchedJob tosSchedJob() {
        return SchedJobConverter.INSTANCE.convert(this);
    }

}
