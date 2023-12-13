/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.constraint;

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;

/**
 * User group constraints
 *
 * @author Ponfee
 */
public class UserGroupConstraints {

    /**
     * SQL中`group IN (a, b, ..., x)`允许的最大长度
     */
    public static final int SQL_GROUP_IN_MAX_SIZE = 50;

    public static Set<String> constrainAndTruncateUserGroup(String user, Set<String> groups) {
        Set<String> uGroups = SchedGroupService.mapUser(user);
        if (CollectionUtils.isEmpty(groups)) {
            groups = uGroups;
        } else if (!uGroups.containsAll(groups)) {
            throw new IllegalArgumentException("Unauthorized groups: " + Sets.difference(groups, uGroups));
        }
        return truncateUserGroup(groups);
    }

    public static Set<String> truncateUserGroup(Set<String> groups) {
        return Collects.truncate(groups, SQL_GROUP_IN_MAX_SIZE);
    }

}
