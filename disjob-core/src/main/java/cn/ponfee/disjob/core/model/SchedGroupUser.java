/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.model;

import cn.ponfee.disjob.common.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * The schedule group user entity, mapped database table sched_group_user
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class SchedGroupUser extends BaseEntity implements Serializable {
    private static final long serialVersionUID = -8078284204149543566L;

    /**
     * 分组名称(同sched_job.group)
     */
    private String group;

    /**
     * 用户
     */
    private String user;

    public SchedGroupUser(String group, String user) {
        this.group = group;
        this.user = user;
    }
}
