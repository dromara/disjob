/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.supervisor.application.converter.SchedGroupConverter;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Update sched group request parameter structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class UpdateSchedGroupRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7531416191031943146L;

    private String group;
    private String ownUser;
    private String alarmUsers;
    private String devUsers;
    private String webHook;
    private String updatedBy;
    private int version;

    public SchedGroup toSchedGroup() {
        return SchedGroupConverter.INSTANCE.convert(this);
    }

}
