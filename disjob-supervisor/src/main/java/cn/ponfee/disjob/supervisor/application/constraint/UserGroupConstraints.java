/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.constraint;

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;

/**
 * User group constraints
 *
 * @author Ponfee
 */
public final class UserGroupConstraints {

    private UserGroupConstraints() { }

    /**
     * SQL中`group IN (a, b, ..., x)`允许的最大长度
     */
    public static final int SQL_GROUP_IN_MAX_SIZE = 50;

    public static Set<String> constrainAndTruncateUserGroup(String user, Set<String> paramGroups) {
        Set<String> permitGroups = SchedGroupService.myGroups(user);
        if (CollectionUtils.isEmpty(paramGroups)) {
            paramGroups = permitGroups;
        } else if (!permitGroups.containsAll(paramGroups)) {
            throw new AuthenticationException("Unauthorized group: " + Sets.difference(paramGroups, permitGroups));
        }
        return truncateUserGroup(paramGroups);
    }

    public static Set<String> truncateUserGroup(Set<String> paramGroups) {
        return Collects.truncate(paramGroups, SQL_GROUP_IN_MAX_SIZE);
    }

    public static void assertPermitGroup(String user, String group) {
        if (!SchedGroupService.myGroups(user).contains(group)) {
            throw new AuthenticationException("Unauthorized group: " + group);
        }
    }

}
