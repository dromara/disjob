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
import cn.ponfee.disjob.common.concurrent.PeriodExecutor;
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import cn.ponfee.disjob.supervisor.component.JobQuerier;
import cn.ponfee.disjob.supervisor.exception.KeyNotExistsException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors.commonScheduledPool;

/**
 * Authorize group service
 *
 * @author Ponfee
 */
@Service
public class AuthorizeGroupService extends SingletonClassConstraint {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorizeGroupService.class);

    /**
     * SQL中`group IN (a, b, ..., x)`允许的最大长度
     */
    public static final int SQL_GROUP_IN_MAX_SIZE = 20;

    private final JobQuerier jobQuerier;
    private final Cache<Long, String> jobGroupCache = CacheBuilder.newBuilder()
        .initialCapacity(1_000)
        .maximumSize(1_000_000)
        // job被删除后无需同步删除缓存：此处只是校验group权限，即便缓存未删除也不会有业务影响
        // 当job被删除后便不再被使用，1天后没有访问会自动过期
        // 惰性过期策略：[被访问时判断过期则被删除] 或 [超过容量时使用LRU算法选择删除]
        .expireAfterAccess(Duration.ofDays(1))
        .recordStats()
        .build();
    @SuppressWarnings("all")
    private final PeriodExecutor cacheStatsPrinter =
        new PeriodExecutor(120_000, () -> LOG.info("\n\n" + CoreUtils.buildCacheStats(jobGroupCache, "Job group cache")));

    public AuthorizeGroupService(JobQuerier jobQuerier) {
        this.jobQuerier = jobQuerier;
        commonScheduledPool().scheduleWithFixedDelay(jobGroupCache::cleanUp, 1, 1, TimeUnit.DAYS);
    }

    public static Set<String> authorizeAndTruncateGroup(String user, Set<String> paramGroups) {
        Set<String> permitGroups = SchedGroupService.myGroups(user);
        if (CollectionUtils.isEmpty(paramGroups)) {
            paramGroups = permitGroups;
        } else if (!permitGroups.containsAll(paramGroups)) {
            throw new AuthenticationException("Unauthorized group: " + Sets.difference(paramGroups, permitGroups));
        }
        return Collects.truncate(paramGroups, SQL_GROUP_IN_MAX_SIZE);
    }

    public static void authorizeGroup(String user, String dataGroup) {
        if (!SchedGroupService.myGroups(user).contains(dataGroup)) {
            throw new AuthenticationException("Unauthorized group: " + dataGroup);
        }
    }

    public static void authorizeGroup(String user, String authGroup, String dataGroup) {
        if (!authGroup.equals(dataGroup)) {
            throw new AuthenticationException("Inconsistent group: " + authGroup + " != " + dataGroup);
        }
        authorizeGroup(user, dataGroup);
    }

    public void authorizeJob(String user, long jobId) {
        String dataGroup = getJobGroup(jobId);
        if (StringUtils.isEmpty(dataGroup)) {
            throw new KeyNotExistsException("Job id not exists: " + jobId);
        }
        authorizeGroup(user, dataGroup);
    }

    public void authorizeJob(String user, String authGroup, long jobId) {
        String dataGroup = getJobGroup(jobId);
        if (StringUtils.isEmpty(dataGroup)) {
            throw new KeyNotExistsException("Job id not exists: " + jobId);
        }
        authorizeGroup(user, authGroup, dataGroup);
    }

    public void authorizeInstance(String user, long instanceId) {
        Long jobId = jobQuerier.getInstanceJobId(instanceId);
        if (jobId == null) {
            throw new KeyNotExistsException("Instance id not exists: " + instanceId);
        }
        authorizeJob(user, jobId);
    }

    public void authorizeInstance(String user, String authGroup, long instanceId) {
        Long jobId = jobQuerier.getInstanceJobId(instanceId);
        if (jobId == null) {
            throw new KeyNotExistsException("Instance id not exists: " + instanceId);
        }
        authorizeJob(user, authGroup, jobId);
    }

    // -----------------------------------------------------------private methods

    private String getJobGroup(Long jobId) {
        String group = jobGroupCache.getIfPresent(jobId);
        cacheStatsPrinter.execute();
        if (group != null) {
            return group;
        }

        group = jobQuerier.getJobGroup(jobId);
        if (group != null) {
            LOG.info("Loaded caching group: {}, {}", jobId, group);
            jobGroupCache.put(jobId, group);
        } else {
            LOG.warn("Loading job not exists: {}", jobId);
        }
        return group;
    }

}
