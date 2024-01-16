/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.supervisor.application.converter.SchedJobConverter;
import lombok.Getter;
import lombok.Setter;

/**
 * Update sched job request parameter structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedJobUpdateRequest extends AbstractSchedJobRequest {
    private static final long serialVersionUID = -1481890923435762900L;

    private Long jobId;
    private String updatedBy;
    private Integer version;

    public SchedJob tosSchedJob() {
        return SchedJobConverter.INSTANCE.convert(this);
    }

}
