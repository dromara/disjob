/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.model.PageRequest;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;

/**
 * Sched group page request
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedGroupPageRequest extends PageRequest {
    private static final long serialVersionUID = -213388921649759103L;

    /**
     * SQL中`group IN (a, b, ..., x)`的最大长度
     */
    public static final int SQL_GROUP_IN_MAX_SIZE = 50;

    private Set<String> groups;

    public void constrainAndTruncateUserGroup(String user) {
        this.groups = constrainAndTruncateUserGroup(user, this.groups);
    }

    public void truncateUserGroup() {
        this.groups = truncateUserGroup(this.groups);
    }

    static Set<String> constrainAndTruncateUserGroup(String user, Set<String> groups) {
        Set<String> uGroups = SchedGroupService.mapUser(user);
        if (CollectionUtils.isEmpty(groups)) {
            groups = uGroups;
        } else if (!uGroups.containsAll(groups)) {
            throw new IllegalArgumentException("Unauthorized groups: " + Sets.difference(groups, uGroups));
        }
        return truncateUserGroup(groups);
    }

    static Set<String> truncateUserGroup(Set<String> groups) {
        return Collects.truncate(groups, SQL_GROUP_IN_MAX_SIZE);
    }

}
