/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.test.job.dao;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.handle.impl.ScriptJobHandler;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import cn.ponfee.disjob.supervisor.util.TriggerTimeUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author Ponfee
 */
public class SchedJobMapperTest extends SpringBootTestBase<SchedJobMapper> {

    @Resource
    private IdGenerator idGenerator;

    @Resource
    private SchedJobMapper jobMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testInsert12() {
        System.out.println(jdbcTemplate.queryForList("Select distinct job_id from sched_job limit 2"));
    }

    @Test
    public void testQuerySql() {
        String jobParam = jobMapper.getByJobId(3988904755500L).getJobParam();
        ScriptJobHandler.ScriptParam scriptParam = Jsons.fromJson(jobParam, ScriptJobHandler.ScriptParam.class);
        System.out.println("----------------------");
        System.out.println(scriptParam.getScript());
        System.out.println("----------------------");
    }

    @Test
    @Disabled
    public void testInsert1() {
        SchedJob job = new SchedJob();
        job.setJobId(idGenerator.generateId());
        job.setJobGroup("default");
        job.setJobName("test");
        job.setJobHandler("cn.ponfee.disjob.core.handle.impl.NoopJobHandler");
        job.setJobState(JobState.DISABLE.value());
        job.setJobParam("test param");
        job.setRetryType(RetryType.NONE.value());
        job.setRetryCount(0);
        job.setRetryInterval(0);
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerValue("0/10 * * * * ?");
        job.setExecuteTimeout(3600000);
        job.setCollisionStrategy(CollisionStrategy.CONCURRENT.value());
        job.setMisfireStrategy(MisfireStrategy.DISCARD.value());
        job.setRouteStrategy(RouteStrategy.ROUND_ROBIN.value());
        job.setWeightScore(1);
        job.setRemark("test remark");
        job.setLastTriggerTime(null);
        job.setNextTriggerTime(TriggerTimeUtils.computeNextTriggerTime(job));
        job.setAlarmSubscribers("");
        job.setUpdatedBy("0");
        job.setCreatedBy("0");
        job.setUpdatedAt(new Date());
        job.setCreatedAt(new Date());

        int insert = bean.insert(job);
        Assertions.assertEquals(1, insert);
    }

    @Test
    @Disabled
    public void testInsert2() {
        SchedJob job = new SchedJob();
        job.setJobId(idGenerator.generateId());
        job.setJobGroup("default");
        job.setJobName(RandomStringUtils.randomAlphanumeric(5));
        job.setJobHandler("cn.ponfee.disjob.core.handle.impl.NoopJobHandler");
        job.setJobState(JobState.DISABLE.value());


        job.setJobParam("");

        job.setRetryType(RetryType.NONE.value());
        job.setRetryCount(0);
        job.setRetryInterval(0);
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.ONCE.value());

        String date = "2022-06-16 11:37:00";
        job.setTriggerValue(date);
        job.setNextTriggerTime(Dates.toDate(date).getTime());
        job.setExecuteTimeout(3600000);
        job.setMisfireStrategy(MisfireStrategy.LAST.value());
        job.setCollisionStrategy(CollisionStrategy.CONCURRENT.value());
        job.setRouteStrategy(RouteStrategy.ROUND_ROBIN.value());
        job.setWeightScore(1);
        job.setRemark("test remark");
        job.setLastTriggerTime(null);
        job.setNextTriggerTime(TriggerTimeUtils.computeNextTriggerTime(job));
        job.setAlarmSubscribers("");
        job.setUpdatedBy("0");
        job.setCreatedBy("0");
        job.setUpdatedAt(new Date());
        job.setCreatedAt(new Date());

        int insert = bean.insert(job);
        Assertions.assertEquals(1, insert);
    }

    @Test
    @Disabled
    public void testInsert3() {
        SchedJob job = new SchedJob();
        job.setJobId(idGenerator.generateId());
        job.setJobGroup("default");
        job.setJobName(RandomStringUtils.randomAlphanumeric(5));
        job.setJobHandler("cn.ponfee.disjob.core.handle.impl.NoopJobHandler");
        job.setJobState(JobState.DISABLE.value());

        job.setJobParam("est");

        job.setRetryType(RetryType.NONE.value());
        job.setRetryCount(0);
        job.setRetryInterval(0);
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.ONCE.value());

        String date = "2022-06-17 18:02:00";
        job.setTriggerValue(date);
        job.setNextTriggerTime(Dates.toDate(date).getTime());
        job.setExecuteTimeout(3600000);
        job.setMisfireStrategy(MisfireStrategy.LAST.value());
        job.setCollisionStrategy(CollisionStrategy.CONCURRENT.value());
        job.setRouteStrategy(RouteStrategy.ROUND_ROBIN.value());
        job.setWeightScore(1);
        job.setRemark("test remark");
        job.setLastTriggerTime(null);
        job.setNextTriggerTime(TriggerTimeUtils.computeNextTriggerTime(job));
        job.setAlarmSubscribers("");
        job.setUpdatedBy("0");
        job.setCreatedBy("0");
        job.setUpdatedAt(new Date());
        job.setCreatedAt(new Date());

        int insert = bean.insert(job);
        Assertions.assertEquals(1, insert);
    }
}
