package cn.ponfee.scheduler.supervisor.test.job.dao;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.core.enums.*;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.supervisor.dao.mapper.SchedJobMapper;
import cn.ponfee.scheduler.supervisor.test.SpringBootTestBase;
import cn.ponfee.scheduler.supervisor.util.JobUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author Ponfee
 */
public class SchedJobMapperTest extends SpringBootTestBase<SchedJobMapper> {

    @Resource
    private IdGenerator idGenerator;

    @Test
    public void testInsert1() {
        SchedJob job = new SchedJob();
        job.setJobId(idGenerator.generateId());
        job.setJobGroup("default");
        job.setJobName("test");
        job.setJobHandler("cn.ponfee.scheduler.core.handle.impl.NoopJobHandler");
        job.setJobState(JobState.STOPPED.value());
        job.setJobParam("test param");
        job.setRetryType(RetryType.NONE.value());
        job.setRetryCount(0);
        job.setRetryInterval(0);
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerConf("0/10 * * * * ?");
        job.setExecuteTimeout(3600000);
        job.setCollisionStrategy(CollisionStrategy.CONCURRENT.value());
        job.setMisfireStrategy(MisfireStrategy.DISCARD.value());
        job.setRouteStrategy(RouteStrategy.ROUND_ROBIN.value());
        job.setWeightScore(1);
        job.setRemark("test remark");
        job.setLastTriggerTime(null);
        job.setNextTriggerTime(JobUtils.computeNextTriggerTime(job));
        job.setAlarmSubscribers("");
        job.setUpdatedBy("0");
        job.setCreatedBy("0");
        job.setUpdatedAt(new Date());
        job.setCreatedAt(new Date());

        int insert = bean().insert(job);
        Assertions.assertEquals(1, insert);
    }

    @Test
    public void testInsert2() {
        SchedJob job = new SchedJob();
        job.setJobId(idGenerator.generateId());
        job.setJobGroup("default");
        job.setJobName(RandomStringUtils.randomAlphanumeric(5));
        job.setJobHandler("cn.ponfee.scheduler.core.handle.impl.NoopJobHandler");
        job.setJobState(JobState.STOPPED.value());


        job.setJobParam("");

        job.setRetryType(RetryType.NONE.value());
        job.setRetryCount(0);
        job.setRetryInterval(0);
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.ONCE.value());

        String date = "2022-06-16 11:37:00";
        job.setTriggerConf(date);
        job.setNextTriggerTime(Dates.toDate(date).getTime());
        job.setExecuteTimeout(3600000);
        job.setMisfireStrategy(MisfireStrategy.LAST.value());
        job.setCollisionStrategy(CollisionStrategy.CONCURRENT.value());
        job.setRouteStrategy(RouteStrategy.ROUND_ROBIN.value());
        job.setWeightScore(1);
        job.setRemark("test remark");
        job.setLastTriggerTime(null);
        job.setNextTriggerTime(JobUtils.computeNextTriggerTime(job));
        job.setAlarmSubscribers("");
        job.setUpdatedBy("0");
        job.setCreatedBy("0");
        job.setUpdatedAt(new Date());
        job.setCreatedAt(new Date());

        int insert = bean().insert(job);
        Assertions.assertEquals(1, insert);
    }

    @Test
    public void testInsert3() {
        SchedJob job = new SchedJob();
        job.setJobId(idGenerator.generateId());
        job.setJobGroup("default");
        job.setJobName(RandomStringUtils.randomAlphanumeric(5));
        job.setJobHandler("cn.ponfee.scheduler.core.handle.impl.NoopJobHandler");
        job.setJobState(JobState.STOPPED.value());

        job.setJobParam("est");

        job.setRetryType(RetryType.NONE.value());
        job.setRetryCount(0);
        job.setRetryInterval(0);
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.ONCE.value());

        String date = "2022-06-17 18:02:00";
        job.setTriggerConf(date);
        job.setNextTriggerTime(Dates.toDate(date).getTime());
        job.setExecuteTimeout(3600000);
        job.setMisfireStrategy(MisfireStrategy.LAST.value());
        job.setCollisionStrategy(CollisionStrategy.CONCURRENT.value());
        job.setRouteStrategy(RouteStrategy.ROUND_ROBIN.value());
        job.setWeightScore(1);
        job.setRemark("test remark");
        job.setLastTriggerTime(null);
        job.setNextTriggerTime(JobUtils.computeNextTriggerTime(job));
        job.setAlarmSubscribers("");
        job.setUpdatedBy("0");
        job.setCreatedBy("0");
        job.setUpdatedAt(new Date());
        job.setCreatedAt(new Date());

        int insert = bean().insert(job);
        Assertions.assertEquals(1, insert);
    }
}
