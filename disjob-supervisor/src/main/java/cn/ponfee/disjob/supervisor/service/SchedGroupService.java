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
import cn.ponfee.disjob.core.model.SchedGroupUser;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedGroupMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedGroupUserMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.common.spring.TransactionUtils.isOneAffectedRow;
import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.TX_MANAGER_SPRING_BEAN_NAME;

/**
 * Sched group service
 *
 * @author Ponfee
 */
@Component
public class SchedGroupService extends SingletonClassConstraint implements Closeable, DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(SchedGroupService.class);

    private static final Lock LOCK = new ReentrantLock();
    private static volatile Map<String, DisjobGroup> groupMap;

    private final SchedGroupMapper schedGroupMapper;
    private final SchedGroupUserMapper schedGroupUserMapper;
    private final LoopThread refresher;

    public SchedGroupService(SchedGroupMapper schedGroupMapper,
                             SchedGroupUserMapper schedGroupUserMapper) {
        this.schedGroupMapper = schedGroupMapper;
        this.schedGroupUserMapper = schedGroupUserMapper;
        this.refresher = new LoopThread("group_metadata_refresher", 60, 60, this::refresh);
        refresh();
    }

    // ------------------------------------------------------------sched group

    @Transactional(transactionManager = TX_MANAGER_SPRING_BEAN_NAME, rollbackFor = Exception.class)
    public long add(SchedGroup schedGroup) {
        schedGroupMapper.insert(schedGroup);
        bindDevUsers(schedGroup.getGroup(), schedGroup.getDevUsers());
        refresh();
        return schedGroup.getId();
    }

    @Transactional(transactionManager = TX_MANAGER_SPRING_BEAN_NAME, rollbackFor = Exception.class)
    public boolean update(SchedGroup schedGroup) {
        if (isOneAffectedRow(schedGroupMapper.update(schedGroup))) {
            schedGroupUserMapper.deleteByGroup(schedGroup.getGroup());
            bindDevUsers(schedGroup.getGroup(), schedGroup.getDevUsers());
            refresh();
            return true;
        } else {
            return false;
        }
    }

    @Transactional(transactionManager = TX_MANAGER_SPRING_BEAN_NAME, rollbackFor = Exception.class)
    public boolean delete(String group) {
        if (isOneAffectedRow(schedGroupMapper.delete(group))) {
            schedGroupUserMapper.deleteByGroup(group);
            refresh();
            return true;
        } else {
            return false;
        }
    }

    public boolean updateSupervisorToken(String group, String newSupervisorToken, String oldSupervisorToken) {
        if (isOneAffectedRow(schedGroupMapper.updateSupervisorToken(group, newSupervisorToken, oldSupervisorToken))) {
            refresh();
            return true;
        } else {
            return false;
        }
    }

    public boolean updateWorkerToken(String group, String newWorkerToken, String oldWorkerToken) {
        if (isOneAffectedRow(schedGroupMapper.updateWorkerToken(group, newWorkerToken, oldWorkerToken))) {
            refresh();
            return true;
        } else {
            return false;
        }
    }

    public boolean updateUserToken(String group, String newUserToken, String oldUserToken) {
        if (isOneAffectedRow(schedGroupMapper.updateUserToken(group, newUserToken, oldUserToken))) {
            refresh();
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------close

    @Override
    public void close() {
        this.refresher.terminate();
    }

    @Override
    public void destroy() {
        close();
    }

    // ------------------------------------------------------------other static methods

    public static DisjobGroup get(String group) {
        DisjobGroup disjobGroup = groupMap.get(group);
        if (disjobGroup == null) {
            throw new GroupNotFoundException("Not found worker group: " + group);
        }
        return disjobGroup;
    }

    // ------------------------------------------------------------private methods

    private void bindDevUsers(String group, List<String> devUsers) {
        if (CollectionUtils.isEmpty(devUsers)) {
            return;
        }
        List<SchedGroupUser> list = devUsers.stream()
            .distinct()
            .map(e -> new SchedGroupUser(group, e))
            .collect(Collectors.toList());
        schedGroupUserMapper.batchInsert(list);
    }

    private void refresh() {
        if (!LOCK.tryLock()) {
            return;
        }
        try {
            Map<String, List<SchedGroupUser>> groupUserMap = schedGroupUserMapper.findAll()
                .stream()
                .collect(Collectors.groupingBy(SchedGroupUser::getGroup));

            List<SchedGroup> list = schedGroupMapper.findAll();
            ImmutableMap.Builder<String, DisjobGroup> builder = ImmutableMap.builderWithExpectedSize(list.size() << 1);
            list.forEach(e -> builder.put(e.getGroup(), DisjobGroup.of(e, groupUserMap.get(e.getGroup()))));
            SchedGroupService.groupMap = builder.build();
        } catch (Throwable t) {
            LOG.error("Refresh sched group error.", t);
            Threads.interruptIfNecessary(t);
        } finally {
            LOCK.unlock();
        }
    }

    @Getter
    public static class DisjobGroup {
        private final String supervisorToken;
        private final String workerToken;
        private final String userToken;
        private final String ownUser;
        private final Set<String> alarmUsers;
        private final Set<String> devUsers;
        private final String webHook;

        private DisjobGroup(String supervisorToken, String workerToken, String userToken,
                            String ownUser, Set<String> alarmUsers, Set<String> devUsers,
                            String webHook) {
            this.supervisorToken = supervisorToken;
            this.workerToken = workerToken;
            this.userToken = userToken;
            this.ownUser = ownUser;
            this.devUsers = devUsers;
            this.alarmUsers = alarmUsers;
            this.webHook = webHook;
        }

        private static DisjobGroup of(SchedGroup schedGroup, List<SchedGroupUser> devUsers) {
            return new DisjobGroup(
                schedGroup.getSupervisorToken(),
                schedGroup.getWorkerToken(),
                schedGroup.getUserToken(),
                schedGroup.getOwnUser(),
                parse(schedGroup.getAlarmUsers()),
                toSet(devUsers, SchedGroupUser::getUser),
                schedGroup.getWebHook()
            );
        }

        private static Set<String> parse(String str) {
            return StringUtils.isBlank(str) ? Collections.emptySet() : ImmutableSet.copyOf(str.split(","));
        }

        private static <S, T> Set<T> toSet(List<S> source, Function<S, T> mapper) {
            if (CollectionUtils.isEmpty(source)) {
                return Collections.emptySet();
            }
            ImmutableSet.Builder<T> builder = ImmutableSet.builderWithExpectedSize(source.size() << 1);
            source.stream().map(mapper).forEach(builder::add);
            return builder.build();
        }
    }

}
