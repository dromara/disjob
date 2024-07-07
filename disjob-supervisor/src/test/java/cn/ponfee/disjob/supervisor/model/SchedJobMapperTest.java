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

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.LazyLoader;
import cn.ponfee.disjob.common.date.JavaUtilDateFormat;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.supervisor.SpringBootTestBase;
import cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig;
import cn.ponfee.disjob.supervisor.dao.mapper.SchedJobMapper;
import cn.ponfee.disjob.supervisor.util.TriggerTimeUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

/**
 * @author Ponfee
 */
public class SchedJobMapperTest extends SpringBootTestBase<SchedJobMapper> {

    @Resource
    private IdGenerator idGenerator;

    @Resource
    private SchedJobMapper jobMapper;

    @Resource(name = SupervisorDataSourceConfig.SPRING_BEAN_NAME_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;

    @Resource(name = "disjobDataSource")
    private DataSource disjobDataSource;

    @Resource(name = "disjob-adminDataSource")
    private DataSource adminDataSource;

    @Test
    public void testInsert12() {
        System.out.println(disjobDataSource);
        System.out.println(adminDataSource);
        System.out.println(jdbcTemplate.queryForList("Select distinct job_id from sched_job limit 2"));
    }

    @Test
    public void testQuerySql() throws Exception {
        String jobParam = jobMapper.get(1003164910267351003L).getJobParam();
        ScriptParam scriptParam = Jsons.JSON5.readValue(jobParam, ScriptParam.class);
        System.out.println("----------------------");
        System.out.println(scriptParam.getScript());
        System.out.println("----------------------");
    }

    @Test
    public void testInsert1() {
        SchedJob job = new SchedJob();
        job.setJobId(idGenerator.generateId());
        job.setGroup("default");
        job.setJobName("test");
        job.setJobHandler("cn.ponfee.disjob.test.handler.NoopJobHandler");
        job.setJobState(JobState.DISABLE.value());
        job.setJobType(JobType.GENERAL.value());
        job.setJobParam("test param");
        job.setRetryType(RetryType.NONE.value());
        job.setRetryCount(0);
        job.setRetryInterval(0);
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerValue("0/10 * * * * ?");
        job.setExecuteTimeout(3600000);
        job.setCollidedStrategy(CollidedStrategy.CONCURRENT.value());
        job.setMisfireStrategy(MisfireStrategy.DISCARD.value());
        job.setRouteStrategy(RouteStrategy.ROUND_ROBIN.value());
        job.setRedeployStrategy(RedeployStrategy.RESUME.value());
        job.setRemark("test remark");
        job.setLastTriggerTime(null);
        job.setNextTriggerTime(TriggerTimeUtils.computeNextTriggerTime(job, new Date()));
        job.setUpdatedBy("0");
        job.setCreatedBy("0");
        job.setUpdatedAt(new Date());
        job.setCreatedAt(new Date());

        int insert = bean.insert(job);
        Assertions.assertEquals(1, insert);
    }

    @Test
    public void testInsert2() throws ParseException {
        SchedJob job = new SchedJob();
        job.setJobId(idGenerator.generateId());
        job.setGroup("default");
        job.setJobName(RandomStringUtils.randomAlphanumeric(5));
        job.setJobHandler("cn.ponfee.disjob.test.handler.NoopJobHandler");
        job.setJobState(JobState.DISABLE.value());

        job.setJobParam("");
        job.setJobType(JobType.GENERAL.value());

        job.setRetryType(RetryType.NONE.value());
        job.setRetryCount(0);
        job.setRetryInterval(0);
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.ONCE.value());

        String date = "2022-06-16 11:37:00";
        job.setTriggerValue(date);
        job.setNextTriggerTime(JavaUtilDateFormat.DEFAULT.parse(date).getTime());
        job.setExecuteTimeout(3600000);
        job.setMisfireStrategy(MisfireStrategy.LAST.value());
        job.setCollidedStrategy(CollidedStrategy.CONCURRENT.value());
        job.setRouteStrategy(RouteStrategy.ROUND_ROBIN.value());
        job.setRedeployStrategy(RedeployStrategy.RESUME.value());
        job.setRemark("test remark");
        job.setLastTriggerTime(null);
        job.setNextTriggerTime(TriggerTimeUtils.computeNextTriggerTime(job, new Date()));
        job.setUpdatedBy("0");
        job.setCreatedBy("0");
        job.setUpdatedAt(new Date());
        job.setCreatedAt(new Date());

        int insert = bean.insert(job);
        Assertions.assertEquals(1, insert);
    }

    @Test
    public void testInsert3() throws ParseException {
        SchedJob job = new SchedJob();
        job.setJobId(idGenerator.generateId());
        job.setGroup("default");
        job.setJobName(RandomStringUtils.randomAlphanumeric(5));
        job.setJobHandler("cn.ponfee.disjob.test.handler.NoopJobHandler");
        job.setJobState(JobState.DISABLE.value());

        job.setJobParam("test");
        job.setJobType(JobType.GENERAL.value());

        job.setRetryType(RetryType.NONE.value());
        job.setRetryCount(0);
        job.setRetryInterval(0);
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.ONCE.value());

        String date = "2022-06-17 18:02:00";
        job.setTriggerValue(date);
        job.setNextTriggerTime(JavaUtilDateFormat.DEFAULT.parse(date).getTime());
        job.setExecuteTimeout(3600000);
        job.setMisfireStrategy(MisfireStrategy.LAST.value());
        job.setCollidedStrategy(CollidedStrategy.CONCURRENT.value());
        job.setRouteStrategy(RouteStrategy.ROUND_ROBIN.value());
        job.setRedeployStrategy(RedeployStrategy.RESUME.value());
        job.setRemark("test remark");
        job.setLastTriggerTime(null);
        job.setNextTriggerTime(TriggerTimeUtils.computeNextTriggerTime(job, new Date()));
        job.setUpdatedBy("0");
        job.setCreatedBy("0");
        job.setUpdatedAt(new Date());
        job.setCreatedAt(new Date());

        int insert = bean.insert(job);
        Assertions.assertEquals(1, insert);
    }

    @Test
    public void testLazyLoader() {
        long job1Id = 1003164910267351000L;
        SchedJob job1 = LazyLoader.of(SchedJob.class, jobMapper::get, job1Id);
        Assertions.assertEquals(job1Id, job1.getJobId());

        SchedJob job2 = LazyLoader.of(SchedJob.class, jobMapper::get, 0L);
        Assertions.assertThrows(NullPointerException.class, job2::getJobId);
    }

    enum ScriptType {
        CMD,SHELL
    }

    @Getter
    @Setter
    static class ScriptParam implements Serializable {
        private static final long serialVersionUID = -7130726567342879284L;

        private ScriptType type;
        private String charset;
        private String script;
        private String[] envp;
    }

}
