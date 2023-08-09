/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.service;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.model.PageResponse;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.openapi.supervisor.converter.SchedJobConverter;
import cn.ponfee.disjob.core.openapi.supervisor.request.SchedInstancePageRequest;
import cn.ponfee.disjob.core.openapi.supervisor.request.SchedJobPageRequest;
import cn.ponfee.disjob.core.openapi.supervisor.response.SchedInstanceResponse;
import cn.ponfee.disjob.core.openapi.supervisor.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedInstanceMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedTaskMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Distributed job query
 *
 * @author Ponfee
 */
@Component
public class DistributedJobQuerier {

    private final SchedJobMapper jobMapper;
    private final SchedTaskMapper taskMapper;
    private final SchedInstanceMapper instanceMapper;


    public DistributedJobQuerier(SchedJobMapper jobMapper,
                                 SchedTaskMapper taskMapper,
                                 SchedInstanceMapper instanceMapper) {
        this.jobMapper = jobMapper;
        this.taskMapper = taskMapper;
        this.instanceMapper = instanceMapper;
    }

    public SchedTask getTask(long taskId) {
        return taskMapper.getByTaskId(taskId);
    }

    public List<SchedTask> findLargeInstanceTasks(long instanceId) {
        return taskMapper.findLargeByInstanceId(instanceId);
    }

    public PageResponse<SchedJobResponse> queryJobForPage(SchedJobPageRequest pageRequest) {
        List<SchedJob> list = jobMapper.queryPageRecords(pageRequest);
        List<SchedJobResponse> rows = list.stream().map(SchedJobConverter.INSTANCE::convert).collect(Collectors.toList());
        long total = pageRequest.isPaged() ? jobMapper.queryPageCount(pageRequest) : rows.size();
        return new PageResponse<>(rows, total, pageRequest);
    }

    public PageResponse<SchedInstanceResponse> queryInstanceForPage(SchedInstancePageRequest pageRequest) {
        if (pageRequest.getEndTime() != null) {
            pageRequest.setEndTime(Dates.endOfDay(pageRequest.getEndTime()));
        }

        List<SchedInstance> list = instanceMapper.queryPageRecords(pageRequest);
        List<SchedInstanceResponse> rows = list.stream().map(SchedJobConverter.INSTANCE::convert).collect(Collectors.toList());
        long total = pageRequest.isPaged() ? instanceMapper.queryPageCount(pageRequest) : rows.size();

        if (pageRequest.isParent()) {
            fillIsTreeLeaf(rows);
        }

        return new PageResponse<>(rows, total, pageRequest);
    }

    public List<SchedInstanceResponse> listInstanceChildren(long pnstanceId) {
        List<SchedInstanceResponse> rows = instanceMapper.selectByPnstanceId(pnstanceId)
            .stream()
            .map(SchedJobConverter.INSTANCE::convert)
            .collect(Collectors.toList());
        fillIsTreeLeaf(rows);
        return rows;
    }

    // --------------------------------------------------------------------------private methods

    private void fillIsTreeLeaf(List<SchedInstanceResponse> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<Long> instanceIds = list.stream().map(SchedInstanceResponse::getInstanceId).collect(Collectors.toList());
        Map<Long, Integer> map = instanceMapper.queryChildCount(instanceIds)
            .stream()
            .collect(Collectors.toMap(e -> MapUtils.getLongValue(e, "pnstanceId"), e -> MapUtils.getIntValue(e, "count")));
        list.forEach(e -> {
            Integer count = map.get(e.getInstanceId());
            e.setIsTreeLeaf(count == null || count == 0 ? 0 : 1);
        });
    }

}
