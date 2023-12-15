/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.common.util.Functions;
import cn.ponfee.disjob.core.exception.GroupNotFoundException;
import cn.ponfee.disjob.core.exception.KeyExistsException;
import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.supervisor.application.converter.SchedGroupConverter;
import cn.ponfee.disjob.supervisor.application.request.AddSchedGroupRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import cn.ponfee.disjob.supervisor.application.request.UpdateSchedGroupRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedGroupResponse;
import cn.ponfee.disjob.supervisor.application.value.DisjobGroup;
import cn.ponfee.disjob.supervisor.application.value.TokenName;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedGroupMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.ponfee.disjob.common.spring.TransactionUtils.isOneAffectedRow;

/**
 * Sched group service
 *
 * @author Ponfee
 */
@Service
public class SchedGroupService extends SingletonClassConstraint implements Closeable, DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(SchedGroupService.class);

    private static final Lock LOCK = new ReentrantLock();
    private static Map<String, DisjobGroup> groupMap;
    private static Map<String, Set<String>> userMap;

    private final SchedGroupMapper schedGroupMapper;
    private final LoopThread refresher;

    public SchedGroupService(SchedGroupMapper schedGroupMapper) {
        this.schedGroupMapper = schedGroupMapper;
        this.refresher = new LoopThread("group_metadata_refresher", 60, 60, this::refresh);
        refresh();
    }

    // ------------------------------------------------------------sched group

    public long add(AddSchedGroupRequest request) {
        request.checkAndTrim();
        if (schedGroupMapper.exists(request.getGroup())) {
            throw new KeyExistsException("Group already exists: " + request.getGroup());
        }
        SchedGroup schedGroup = request.toSchedGroup();
        schedGroup.setUpdatedBy(schedGroup.getCreatedBy());
        schedGroupMapper.insert(schedGroup);
        refresh();
        return schedGroup.getId();
    }

    public boolean delete(String group, String updatedBy) {
        return Functions.doIfTrue(
            isOneAffectedRow(schedGroupMapper.softDelete(group, updatedBy)),
            this::refresh
        );
    }

    public boolean edit(UpdateSchedGroupRequest request) {
        request.checkAndTrim();
        return Functions.doIfTrue(
            isOneAffectedRow(schedGroupMapper.edit(request.toSchedGroup())),
            this::refresh
        );
    }

    public SchedGroupResponse get(String group) {
        SchedGroup schedGroup = schedGroupMapper.get(group);
        return SchedGroupConverter.INSTANCE.convert(schedGroup);
    }

    public boolean updateToken(String group, TokenName name, String newToken, String updatedBy, String oldToken) {
        return Functions.doIfTrue(
            isOneAffectedRow(schedGroupMapper.updateToken(group, name, newToken, updatedBy, oldToken)),
            this::refresh
        );
    }

    public boolean updateOwnUser(String group, String ownUser, String updatedBy) {
        Assert.hasText(ownUser, "Own user cannot be blank.");
        return Functions.doIfTrue(
            isOneAffectedRow(schedGroupMapper.updateOwnUser(group, ownUser.trim(), updatedBy)),
            this::refresh
        );
    }

    public List<String> matchGroup(String term) {
        return schedGroupMapper.matchGroup(term);
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

    public static DisjobGroup getGroup(String group) {
        DisjobGroup disjobGroup = groupMap.get(group);
        if (disjobGroup == null) {
            throw new GroupNotFoundException("Not found worker group: " + group);
        }
        return disjobGroup;
    }

    public static Set<String> myGroups(String user) {
        Set<String> groups = userMap.get(user);
        return groups == null ? Collections.emptySet() : groups;
    }

    // ------------------------------------------------------------private methods

    /**
     * synchronized的执行过程
     *  1）获得同步锁
     *  2）清空工作内存
     *  3）从主内存中复制数据副本到工作内存
     *  4）执行代码
     *  5）刷新数据到主内存
     *  6）释放锁
     */
    private synchronized void refresh() {
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
                Stream.of(array)
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .forEach(u -> users.add(Pair.of(u, group)));
                return users.stream();
            })
            .collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(Pair::getRight, ImmutableSet.toImmutableSet())));
        return (Map<String, Set<String>>) userMap;
    }

}
