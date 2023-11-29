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
 * The schedule user group entity, mapped database table sched_user_group
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class SchedUserGroup extends BaseEntity implements Serializable {

    /**
     * 用户名
     */
    private String username;

    /**
     * 分组名称(同sched_job.group)
     */
    private String group;

}
