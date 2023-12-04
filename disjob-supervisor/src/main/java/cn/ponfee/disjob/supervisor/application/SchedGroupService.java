/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.core.exception.GroupNotFoundException;
import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.core.model.SchedGroupUser;
import cn.ponfee.disjob.supervisor.application.converter.SchedGroupConverter;
import cn.ponfee.disjob.supervisor.application.request.AddSchedGroupRequest;
import cn.ponfee.disjob.supervisor.application.request.UpdateSchedGroupRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedGroupResponse;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedGroupMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedGroupUserMapper;
import cn.ponfee.disjob.supervisor.application.model.DisjobGroup;
import cn.ponfee.disjob.supervisor.application.request.SchedGroupPageRequest;
import cn.ponfee.disjob.supervisor.provider.openapi.converter.SchedJobConverter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public long add(AddSchedGroupRequest request) {
        Assert.hasText(request.getCreatedBy(), "Created by cannot be blank.");
        SchedGroup schedGroup = request.toSchedGroup();
        schedGroup.setUpdatedBy(schedGroup.getCreatedBy());
        schedGroupMapper.insert(schedGroup);
        bindDevUsers(schedGroup.getGroup(), schedGroup.getDevUsers());
        refresh();
        return schedGroup.getId();
    }

    @Transactional(transactionManager = TX_MANAGER_SPRING_BEAN_NAME, rollbackFor = Exception.class)
    public boolean update(UpdateSchedGroupRequest request) {
        Assert.hasText(request.getUpdatedBy(), "Updated by cannot be blank.");
        SchedGroup schedGroup = request.toSchedGroup();
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
        if (isOneAffectedRow(schedGroupMapper.softDelete(group))) {
            schedGroupUserMapper.deleteByGroup(group);
            refresh();
            return true;
        } else {
            return false;
        }
    }

    public SchedGroupResponse getGroup(String group) {
        SchedGroup schedGroup = schedGroupMapper.get(group);
        return SchedGroupConverter.INSTANCE.convert(schedGroup);
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

    public PageResponse<SchedGroupResponse> queryForPage(SchedGroupPageRequest pageRequest) {
        return pageRequest.query(
            schedGroupMapper::queryPageCount,
            schedGroupMapper::queryPageRecords,
            SchedGroupConverter.INSTANCE::convert
        );
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

    private void bindDevUsers(String group, String devUsers) {
        if (StringUtils.isEmpty(devUsers)) {
            return;
        }
        List<SchedGroupUser> list = Stream.of(devUsers.split(","))
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

}
