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
import cn.ponfee.disjob.core.exception.GroupNotFoundException;
import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedGroupMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static cn.ponfee.disjob.supervisor.base.SupervisorConstants.AFFECTED_ONE_ROW;

/**
 * Sched group manager
 *
 * @author Ponfee
 */
@Component
public class SchedGroupManager extends SingletonClassConstraint implements Closeable, DisposableBean {

    private static final Lock LOCK = new ReentrantLock();
    private static volatile Map<String, DisjobGroup> all;

    private final SchedGroupMapper schedGroupMapper;
    private final LoopThread refresher;

    public SchedGroupManager(SchedGroupMapper schedGroupMapper) {
        this.schedGroupMapper = schedGroupMapper;
        this.refresher = new LoopThread("group_metadata_refresher", 30, 30, this::refresh);
        refresh();
    }

    public long add(SchedGroup schedGroup) {
        schedGroupMapper.insert(schedGroup);
        refresh();
        return schedGroup.getId();
    }

    public boolean updateWorkerToken(String group, String newWorkerToken, String oldWorkerToken) {
        boolean flag = schedGroupMapper.updateWorkerToken(group, newWorkerToken, oldWorkerToken) == AFFECTED_ONE_ROW;
        refresh();
        return flag;
    }

    public boolean updateSupervisorToken(String group, String newSupervisorToken, String oldSupervisorToken) {
        boolean flag = schedGroupMapper.updateSupervisorToken(group, newSupervisorToken, oldSupervisorToken) == AFFECTED_ONE_ROW;
        refresh();
        return flag;
    }

    public boolean updateAlarmConfig(SchedGroup schedGroup) {
        boolean flag = schedGroupMapper.updateAlarmConfig(schedGroup) == AFFECTED_ONE_ROW;
        refresh();
        return flag;
    }

    public boolean delete(String group) {
        boolean flag = schedGroupMapper.delete(group) == AFFECTED_ONE_ROW;
        refresh();
        return flag;
    }

    // ------------------------------------------------------------other methods

    public static DisjobGroup get(String group) {
        DisjobGroup disjobGroup = all.get(group);
        if (disjobGroup == null) {
            throw new GroupNotFoundException("Not found worker group: " + group);
        }
        return disjobGroup;
    }

    public static Set<String> allGroups() {
        return all.keySet();
    }

    @Override
    public void close() {
        this.refresher.terminate();
    }

    @Override
    public void destroy() {
        close();
    }

    private void refresh() {
        if (!LOCK.tryLock()) {
            return;
        }
        try {
            ImmutableMap.Builder<String, DisjobGroup> builder = ImmutableMap.builder();
            schedGroupMapper.findAll().forEach(e -> builder.put(e.getGroup(), DisjobGroup.of(e)));
            all = builder.build();
        } finally {
            LOCK.unlock();
        }
    }

    @Getter
    public static class DisjobGroup {
        private final String workerToken;
        private final String supervisorToken;
        private final Set<String> alarmSubscribers;
        private final String webHook;

        private DisjobGroup(String workerToken, String supervisorToken, Set<String> alarmSubscribers, String webHook) {
            this.workerToken = workerToken;
            this.supervisorToken = supervisorToken;
            this.alarmSubscribers = alarmSubscribers;
            this.webHook = webHook;
        }

        private static DisjobGroup of(SchedGroup schedGroup) {
            String alarmSubscribers = schedGroup.getAlarmSubscribers();
            return new DisjobGroup(
                schedGroup.getWorkerToken(),
                schedGroup.getSupervisorToken(),
                StringUtils.isBlank(alarmSubscribers) ? null : ImmutableSet.copyOf(alarmSubscribers.split(",")),
                schedGroup.getWebHook()
            );
        }
    }

}
