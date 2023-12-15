/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import cn.ponfee.disjob.core.exception.KeyNotExistsException;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedInstanceMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Authorize group service
 *
 * @author Ponfee
 */
@Service
public class AuthorizeGroupService extends SingletonClassConstraint {

    /**
     * SQL中`group IN (a, b, ..., x)`允许的最大长度
     */
    public static final int SQL_GROUP_IN_MAX_SIZE = 50;

    private final SchedJobMapper schedJobMapper;
    private final SchedInstanceMapper schedInstanceMapper;

    public AuthorizeGroupService(SchedJobMapper schedJobMapper,
                                 SchedInstanceMapper schedInstanceMapper) {
        this.schedJobMapper = schedJobMapper;
        this.schedInstanceMapper = schedInstanceMapper;
    }

    public static Set<String> authorizeAndTruncateGroup(String user, Set<String> paramGroups) {
        Set<String> permitGroups = SchedGroupService.myGroups(user);
        if (CollectionUtils.isEmpty(paramGroups)) {
            paramGroups = permitGroups;
        } else if (!permitGroups.containsAll(paramGroups)) {
            throw new AuthenticationException("Unauthorized group: " + Sets.difference(paramGroups, permitGroups));
        }
        return truncateGroup(paramGroups);
    }

    public static Set<String> truncateGroup(Set<String> paramGroups) {
        return Collects.truncate(paramGroups, SQL_GROUP_IN_MAX_SIZE);
    }

    public static void authorizeGroup(String user, String group) {
        if (!SchedGroupService.myGroups(user).contains(group)) {
            throw new AuthenticationException("Unauthorized group: " + group);
        }
    }

    public void authorizeJob(String user, long jobId) {
        String group = schedJobMapper.getGroup(jobId);
        if (StringUtils.isEmpty(group)) {
            throw new KeyNotExistsException();
        }
        if (!SchedGroupService.myGroups(user).contains(group)) {
            throw new AuthenticationException("Unauthorized group: " + group);
        }
    }

    public void authorizeInstance(String user, long instanceId) {
        Long jobId = schedInstanceMapper.getJobId(instanceId);
        if (jobId == null) {
            throw new KeyNotExistsException();
        }
        authorizeJob(user, jobId);
    }

}
