/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.common.util.Functions;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.TokenType;
import cn.ponfee.disjob.registry.Discovery;
import cn.ponfee.disjob.supervisor.application.converter.SchedGroupConverter;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupAddRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupUpdateRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedGroupResponse;
import cn.ponfee.disjob.supervisor.application.value.DisjobGroup;
import cn.ponfee.disjob.supervisor.base.SupervisorEvent;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedGroupMapper;
import cn.ponfee.disjob.supervisor.exception.GroupNotFoundException;
import cn.ponfee.disjob.supervisor.exception.KeyExistsException;
import cn.ponfee.disjob.supervisor.model.SchedGroup;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors.commonScheduledPool;
import static cn.ponfee.disjob.common.spring.TransactionUtils.isOneAffectedRow;

/**
 * Sched group service
 *
 * @author Ponfee
 */
@Service
public class SchedGroupService extends SingletonClassConstraint {
    private static final Logger LOG = LoggerFactory.getLogger(SchedGroupService.class);

    private static final Lock LOCK = new ReentrantLock();
    private static final AtomicReference<Cache> CACHE = new AtomicReference<>(Cache.of());

    private final SchedGroupMapper groupMapper;
    private final Discovery<Worker> discoverWorker;
    private final ServerInvokeService serverInvokeService;

    public SchedGroupService(SchedGroupMapper groupMapper,
                             Discovery<Worker> discoverWorker,
                             ServerInvokeService serverInvokeService,
                             SupervisorProperties supervisorConf) {
        this.groupMapper = groupMapper;
        this.discoverWorker = discoverWorker;
        this.serverInvokeService = serverInvokeService;

        supervisorConf.check();
        int periodSeconds = Math.max(supervisorConf.getGroupRefreshPeriodSeconds(), 30);
        commonScheduledPool().scheduleWithFixedDelay(this::refresh, periodSeconds, periodSeconds, TimeUnit.SECONDS);
        refresh();
    }

    // ------------------------------------------------------------sched group

    public long add(SchedGroupAddRequest request) {
        request.checkAndTrim();
        if (groupMapper.exists(request.getGroup())) {
            throw new KeyExistsException("Group already exists: " + request.getGroup());
        }
        SchedGroup schedGroup = request.toSchedGroup();
        schedGroup.setUpdatedBy(schedGroup.getCreatedBy());
        groupMapper.insert(schedGroup);
        refreshAndPublish();
        return schedGroup.getId();
    }

    public boolean delete(String group, String updatedBy) {
        List<Worker> list = discoverWorker.getDiscoveredServers(group);
        if (CollectionUtils.isNotEmpty(list)) {
            throw new KeyExistsException("Group '" + group + "' has registered workers, cannot delete.");
        }
        return Functions.doIfTrue(
            isOneAffectedRow(groupMapper.softDelete(group, updatedBy)),
            this::refreshAndPublish
        );
    }

    public boolean edit(SchedGroupUpdateRequest request) {
        request.checkAndTrim();
        return Functions.doIfTrue(
            isOneAffectedRow(groupMapper.edit(request.toSchedGroup())),
            this::refreshAndPublish
        );
    }

    public SchedGroupResponse get(String group) {
        return SchedGroupConverter.INSTANCE.convert(groupMapper.get(group));
    }

    public boolean updateToken(String group, TokenType type, String newToken, String updatedBy, String oldToken) {
        return Functions.doIfTrue(
            isOneAffectedRow(groupMapper.updateToken(group, type, newToken, updatedBy, oldToken)),
            this::refreshAndPublish
        );
    }

    public boolean updateOwnUser(String group, String ownUser, String updatedBy) {
        Assert.hasText(ownUser, "Own user cannot be blank.");
        return Functions.doIfTrue(
            isOneAffectedRow(groupMapper.updateOwnUser(group, ownUser.trim(), updatedBy)),
            this::refreshAndPublish
        );
    }

    public List<String> searchGroup(String term) {
        return groupMapper.searchGroup(term);
    }

    public PageResponse<SchedGroupResponse> queryForPage(SchedGroupPageRequest pageRequest) {
        PageResponse<SchedGroupResponse> page = pageRequest.query(
            groupMapper::queryPageCount,
            groupMapper::queryPageRecords,
            SchedGroupConverter.INSTANCE::convert
        );

        page.forEachRow(SchedGroupResponse::maskToken);
        return page;
    }

    // ------------------------------------------------------------other static methods

    public static ImmutableSet<String> myGroups(String user) {
        return CACHE.get().myGroups(user);
    }

    public static DisjobGroup getGroup(String group) {
        return CACHE.get().getGroup(group);
    }

    public static boolean isDeveloper(String group, String user) {
        return getGroup(group).isDeveloper(user);
    }

    void refresh() {
        if (LOCK.tryLock()) {
            try {
                CACHE.set(Cache.of(groupMapper.findAll()));
            } catch (Throwable t) {
                LOG.error("Refresh sched group error.", t);
                Threads.interruptIfNecessary(t);
            } finally {
                LOCK.unlock();
            }
        }
    }

    // ------------------------------------------------------------private methods

    private void refreshAndPublish() {
        refresh();
        SupervisorEvent event = SupervisorEvent.of(SupervisorEvent.Type.REFRESH_GROUP, null);
        serverInvokeService.publishOtherSupervisors(event);
    }

    private static Map<String, DisjobGroup> toGroupMap(List<DisjobGroup> list) {
        return Collects.toMap(list, DisjobGroup::getGroup, Function.identity());
    }

    private static Map<String, ImmutableSet<String>> toUserMap(List<DisjobGroup> list) {
        return list.stream()
            .flatMap(e -> e.getDevUsers().stream().map(u -> Pair.of(u, e.getGroup())))
            .collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(Pair::getRight, ImmutableSet.toImmutableSet())));
    }

    private static class Cache {
        /**
         * Map<group, DisjobGroup>
         */
        final Map<String, DisjobGroup> groupMap;
        /**
         * Map<user, groups>
         */
        final Map<String, ImmutableSet<String>> userMap;

        Cache(Map<String, DisjobGroup> groupMap, Map<String, ImmutableSet<String>> userMap) {
            this.groupMap = groupMap;
            this.userMap = userMap;
        }

        static Cache of() {
            return new Cache(Collections.emptyMap(), Collections.emptyMap());
        }

        static Cache of(List<SchedGroup> groups) {
            List<DisjobGroup> list = groups.stream().map(DisjobGroup::new).collect(Collectors.toList());
            return new Cache(toGroupMap(list), toUserMap(list));
        }

        DisjobGroup getGroup(String group) {
            DisjobGroup disjobGroup = groupMap.get(group);
            if (disjobGroup == null) {
                throw new GroupNotFoundException("Not found worker group: " + group);
            }
            return disjobGroup;
        }

        ImmutableSet<String> myGroups(String user) {
            ImmutableSet<String> groups = userMap.get(user);
            return groups == null ? ImmutableSet.of() : groups;
        }
    }

}
