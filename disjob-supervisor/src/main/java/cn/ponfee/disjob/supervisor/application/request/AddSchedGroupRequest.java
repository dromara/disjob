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
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Add sched group request.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class AddSchedGroupRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 8022970678398556635L;

    private String group;
    private String ownUser;
    private String createdBy;

    public SchedGroup toSchedGroup() {
        return SchedGroupConverter.INSTANCE.convert(this);
    }

    public void checkAndTrim() {
        Assert.hasText(group, "Group cannot be blank.");
        Assert.hasText(ownUser, "Own user cannot be blank.");
        Assert.hasText(createdBy, "Created by cannot be blank.");

        group = group.trim();
        ownUser = ownUser.trim();
    }

}
