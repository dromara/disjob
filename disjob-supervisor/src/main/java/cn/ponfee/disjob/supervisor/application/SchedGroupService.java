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
import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.common.util.Functions;
import cn.ponfee.disjob.core.base.Tokens;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.dto.supervisor.EventParam;
import cn.ponfee.disjob.core.dto.worker.AuthenticationParam;
import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.core.model.TokenType;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.application.converter.SchedGroupConverter;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupAddRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupUpdateRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedGroupResponse;
import cn.ponfee.disjob.supervisor.application.value.DisjobGroup;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedGroupMapper;
import cn.ponfee.disjob.supervisor.exception.GroupNotFoundException;
import cn.ponfee.disjob.supervisor.exception.KeyExistsException;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static volatile Map<String, DisjobGroup> groupMap;
    private static volatile Map<String, Set<String>> userMap;

    private final SchedGroupMapper schedGroupMapper;
    private final SupervisorRegistry supervisorRegistry;
    private final ServerInvokeService serverInvokeService;

    public SchedGroupService(SchedGroupMapper schedGroupMapper,
                             SupervisorRegistry supervisorRegistry,
                             ServerInvokeService serverInvokeService,
                             SupervisorProperties supervisorProperties) {
        this.schedGroupMapper = schedGroupMapper;
        this.supervisorRegistry = supervisorRegistry;
        this.serverInvokeService = serverInvokeService;
        int periodSeconds = Math.max(supervisorProperties.getGroupRefreshPeriodSeconds(), 30);
        commonScheduledPool().scheduleWithFixedDelay(this::refresh, periodSeconds, periodSeconds, TimeUnit.SECONDS);
        refresh();
    }

    // ------------------------------------------------------------sched group

    public long add(SchedGroupAddRequest request) {
        request.checkAndTrim();
        if (schedGroupMapper.exists(request.getGroup())) {
            throw new KeyExistsException("Group already exists: " + request.getGroup());
        }
        SchedGroup schedGroup = request.toSchedGroup();
        schedGroup.setUpdatedBy(schedGroup.getCreatedBy());
        schedGroupMapper.insert(schedGroup);
        refresh0();
        return schedGroup.getId();
    }

    public boolean delete(String group, String updatedBy) {
        List<Worker> list = supervisorRegistry.getDiscoveredServers(group);
        if (CollectionUtils.isNotEmpty(list)) {
            throw new KeyExistsException("Group '" + group + "' has registered workers, cannot delete.");
        }
        return Functions.doIfTrue(
            isOneAffectedRow(schedGroupMapper.softDelete(group, updatedBy)),
            this::refresh0
        );
    }

    public boolean edit(SchedGroupUpdateRequest request) {
        request.checkAndTrim();
        return Functions.doIfTrue(
            isOneAffectedRow(schedGroupMapper.edit(request.toSchedGroup())),
            this::refresh0
        );
    }

    public SchedGroupResponse get(String group) {
        SchedGroup schedGroup = schedGroupMapper.get(group);
        return SchedGroupConverter.INSTANCE.convert(schedGroup);
    }

    public boolean updateToken(String group, TokenType type, String newToken, String updatedBy, String oldToken) {
        return Functions.doIfTrue(
            isOneAffectedRow(schedGroupMapper.updateToken(group, type, newToken, updatedBy, oldToken)),
            this::refresh0
        );
    }

    public boolean updateOwnUser(String group, String ownUser, String updatedBy) {
        Assert.hasText(ownUser, "Own user cannot be blank.");
        return Functions.doIfTrue(
            isOneAffectedRow(schedGroupMapper.updateOwnUser(group, ownUser.trim(), updatedBy)),
            this::refresh0
        );
    }

    public List<String> searchGroup(String term) {
        return schedGroupMapper.searchGroup(term);
    }

    public PageResponse<SchedGroupResponse> queryForPage(SchedGroupPageRequest pageRequest) {
        PageResponse<SchedGroupResponse> page = pageRequest.query(
            schedGroupMapper::queryPageCount,
            schedGroupMapper::queryPageRecords,
            SchedGroupConverter.INSTANCE::convert
        );

        page.forEachRow(SchedGroupResponse::maskToken);
        return page;
    }

    // ------------------------------------------------------------other static methods

    public static Set<String> myGroups(String user) {
        Set<String> groups = userMap.get(user);
        return groups == null ? Collections.emptySet() : groups;
    }

    public static DisjobGroup getGroup(String group) {
        DisjobGroup disjobGroup = groupMap.get(group);
        if (disjobGroup == null) {
            throw new GroupNotFoundException("Not found worker group: " + group);
        }
        return disjobGroup;
    }

    public static boolean isDeveloper(String group, String user) {
        return getGroup(group).isDeveloper(user);
    }

    public static String createSupervisorAuthenticationToken(String group) {
        String supervisorToken = getGroup(group).getSupervisorToken();
        return Tokens.createAuthentication(supervisorToken, TokenType.supervisor, group);
    }

    public static void fillSupervisorAuthenticationToken(String group, AuthenticationParam param) {
        param.setSupervisorToken(createSupervisorAuthenticationToken(group));
    }

    public static boolean verifyWorkerAuthenticationToken(String tokenSecret, String group) {
        String workerToken = getGroup(group).getWorkerToken();
        return Tokens.verifyAuthentication(tokenSecret, workerToken, TokenType.worker, group);
    }

    public static boolean verifyUserAuthenticationToken(String tokenSecret, String group) {
        String userToken = getGroup(group).getUserToken();
        return Tokens.verifyAuthentication(tokenSecret, userToken, TokenType.user, group);
    }

    public static boolean verifyWorkerSignatureToken(String tokenSecret, String group) {
        String workerToken = getGroup(group).getWorkerToken();
        return Tokens.verifySignature(tokenSecret, workerToken, TokenType.worker, group);
    }

    // ------------------------------------------------------------private methods

    void refresh() {
        if (!LOCK.tryLock()) {
            return;
        }

        try {
            List<SchedGroup> list = schedGroupMapper.findAll();
            Map<String, DisjobGroup> groupMap0 = list.stream().collect(Collectors.toMap(SchedGroup::getGroup, DisjobGroup::of));
            Map<String, Set<String>> userMap0 = toUserMap(list);

            SchedGroupService.groupMap = groupMap0;
            SchedGroupService.userMap = userMap0;
        } catch (Throwable t) {
            LOG.error("Refresh sched group error.", t);
            Threads.interruptIfNecessary(t);
        } finally {
            LOCK.unlock();
        }
    }

    private void refresh0() {
        refresh();
        serverInvokeService.publishOtherSupervisors(new EventParam(EventParam.Type.REFRESH_GROUP));
    }

    private static Map<String, Set<String>> toUserMap(List<SchedGroup> list) {
        Map<String, ?> userMap = list.stream()
            .flatMap(e -> {
                String group = e.getGroup();
                String devUsers = e.getDevUsers();
                if (StringUtils.isBlank(devUsers)) {
                    return Stream.of(Pair.of(e.getOwnUser(), group));
                }

                String[] array = devUsers.split(Str.COMMA);
                List<Pair<String, String>> users = new ArrayList<>(array.length + 1);
                users.add(Pair.of(e.getOwnUser(), group));
                Arrays.stream(array)
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .forEach(u -> users.add(Pair.of(u, group)));
                return users.stream();
            })
            .collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(Pair::getRight, ImmutableSet.toImmutableSet())));
        return (Map<String, Set<String>>) userMap;
    }

}
