/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.service;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.core.exception.GroupNotFoundException;
import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.core.model.SchedUserGroup;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedGroupMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedUserGroupMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.supervisor.base.SupervisorConstants.AFFECTED_ONE_ROW;

/**
 * Sched group service
 *
 * @author Ponfee
 */
@Component
public class SchedGroupService extends SingletonClassConstraint implements Closeable, DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(SchedGroupService.class);

    private static final Lock LOCK1 = new ReentrantLock();
    private static final Lock LOCK2 = new ReentrantLock();

    private static volatile Map<String, DisjobGroup> groupMap;
    private static volatile Map<String, Set<String>> userGroupMap;

    private final SchedGroupMapper schedGroupMapper;
    private final SchedUserGroupMapper schedUserGroupMapper;
    private final LoopThread refresher;

    public SchedGroupService(SchedGroupMapper schedGroupMapper,
                             SchedUserGroupMapper schedUserGroupMapper) {
        this.schedGroupMapper = schedGroupMapper;
        this.schedUserGroupMapper = schedUserGroupMapper;
        this.refresher = new LoopThread("group_metadata_refresher", 30, 30, () -> {
            refreshDisjobGroup();
            refreshUserGroup();
        });

        refreshDisjobGroup();
        refreshUserGroup();
    }

    // ------------------------------------------------------------sched group

    public long add(SchedGroup schedGroup) {
        schedGroupMapper.insert(schedGroup);
        refreshDisjobGroup();
        return schedGroup.getId();
    }

    public boolean updateSupervisorToken(String group, String newSupervisorToken, String oldSupervisorToken) {
        boolean flag = schedGroupMapper.updateSupervisorToken(group, newSupervisorToken, oldSupervisorToken) == AFFECTED_ONE_ROW;
        refreshDisjobGroup();
        return flag;
    }

    public boolean updateWorkerToken(String group, String newWorkerToken, String oldWorkerToken) {
        boolean flag = schedGroupMapper.updateWorkerToken(group, newWorkerToken, oldWorkerToken) == AFFECTED_ONE_ROW;
        refreshDisjobGroup();
        return flag;
    }

    public boolean updateUserToken(String group, String newUserToken, String oldUserToken) {
        boolean flag = schedGroupMapper.updateUserToken(group, newUserToken, oldUserToken) == AFFECTED_ONE_ROW;
        refreshDisjobGroup();
        return flag;
    }

    public boolean updateAlarmConfig(SchedGroup schedGroup) {
        boolean flag = schedGroupMapper.updateAlarmConfig(schedGroup) == AFFECTED_ONE_ROW;
        refreshDisjobGroup();
        return flag;
    }

    public boolean delete(String group) {
        boolean flag = schedGroupMapper.delete(group) == AFFECTED_ONE_ROW;
        schedUserGroupMapper.deleteByGroup(group);
        refreshDisjobGroup();
        refreshUserGroup();
        return flag;
    }

    // ------------------------------------------------------------sched user group

    public long add(SchedUserGroup userGroup) {
        if (!schedGroupMapper.exists(userGroup.getGroup())) {
            throw new IllegalArgumentException("Group not found: " + userGroup.getGroup());
        }
        schedUserGroupMapper.insert(userGroup);
        refreshUserGroup();
        return userGroup.getId();
    }

    public boolean deleteByUsername(String username) {
        boolean flag = schedUserGroupMapper.deleteByUsername(username) == AFFECTED_ONE_ROW;
        refreshUserGroup();
        return flag;
    }

    // ------------------------------------------------------------other methods

    public static DisjobGroup get(String group) {
        DisjobGroup disjobGroup = groupMap.get(group);
        if (disjobGroup == null) {
            throw new GroupNotFoundException("Not found worker group: " + group);
        }
        return disjobGroup;
    }

    public static Set<String> allGroups() {
        return groupMap.keySet();
    }

    public static boolean inGroup(String username, String group) {
        Set<String> set = userGroupMap.get(username);
        return set != null && set.contains(group);
    }

    // ------------------------------------------------------------other methods

    @Override
    public void close() {
        this.refresher.terminate();
    }

    @Override
    public void destroy() {
        close();
    }

    // ------------------------------------------------------------private methods

    private void refreshDisjobGroup() {
        if (!LOCK1.tryLock()) {
            return;
        }
        try {
            List<SchedGroup> list = schedGroupMapper.findAll();
            ImmutableMap.Builder<String, DisjobGroup> builder = ImmutableMap.builderWithExpectedSize(list.size() << 1);
            list.forEach(e -> builder.put(e.getGroup(), DisjobGroup.of(e)));
            SchedGroupService.groupMap = builder.build();
        } catch (Throwable t) {
            LOG.error("Refresh group error.", t);
            Threads.interruptIfNecessary(t);
        } finally {
            LOCK1.unlock();
        }
    }

    private void refreshUserGroup() {
        if (!LOCK2.tryLock()) {
            return;
        }
        try {
            List<SchedUserGroup> list = schedUserGroupMapper.findAll();
            ImmutableMap.Builder<String, Set<String>> mapBuilder = ImmutableMap.builderWithExpectedSize(list.size() << 1);

            list.stream()
                .collect(Collectors.groupingBy(SchedUserGroup::getUsername))
                .forEach((k, v) -> {
                    ImmutableSet.Builder<String> setBuilder = ImmutableSet.builderWithExpectedSize(v.size() << 1);
                    v.forEach(e -> setBuilder.add(e.getGroup()));
                    mapBuilder.put(k, setBuilder.build());
                });
            SchedGroupService.userGroupMap = mapBuilder.build();
        } catch (Throwable t) {
            LOG.error("Refresh user group error.", t);
            Threads.interruptIfNecessary(t);
        } finally {
            LOCK2.unlock();
        }
    }

    @Getter
    public static class DisjobGroup {
        private final String supervisorToken;
        private final String workerToken;
        private final String userToken;
        private final Set<String> alarmSubscribers;
        private final String webHook;

        private DisjobGroup(String supervisorToken, String workerToken, String userToken,
                            Set<String> alarmSubscribers, String webHook) {
            this.supervisorToken = supervisorToken;
            this.workerToken = workerToken;
            this.userToken = userToken;
            this.alarmSubscribers = alarmSubscribers;
            this.webHook = webHook;
        }

        private static DisjobGroup of(SchedGroup schedGroup) {
            String alarmSubscribers = schedGroup.getAlarmSubscribers();
            return new DisjobGroup(
                schedGroup.getSupervisorToken(),
                schedGroup.getWorkerToken(),
                schedGroup.getUserToken(),
                StringUtils.isBlank(alarmSubscribers) ? null : ImmutableSet.copyOf(alarmSubscribers.split(",")),
                schedGroup.getWebHook()
            );
        }
    }

}
