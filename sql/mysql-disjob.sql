-- ----------------------------
-- DROP USER
-- ----------------------------
-- 语法：DROP USER 'username'@'host_name'，“%”表示删除所有主机中的该用户，默认为“%”
DROP USER IF EXISTS 'disjob'@'%';
FLUSH PRIVILEGES;
SET NAMES utf8mb4;


-- ----------------------------
-- CREATE DATABASE
-- ----------------------------
-- SET global validate_password_policy=LOW;
-- SET sql_mode="NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION";
-- DROP DATABASE IF EXISTS disjob;
CREATE DATABASE IF NOT EXISTS disjob DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
USE disjob;


-- ----------------------------
-- USER PRIVILEGES
-- ----------------------------
-- GRANT ALL PRIVILEGES ON disjob.* TO 'disjob'@'%' IDENTIFIED BY 'disjob';
CREATE USER 'disjob'@'%' IDENTIFIED BY 'disjob$123456';
GRANT ALL PRIVILEGES ON disjob.* TO 'disjob'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;


-- ----------------------------
-- CREATE TABLE
-- ----------------------------
CREATE TABLE `sched_job` (
  `id`                  BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `job_id`              BIGINT         UNSIGNED  NOT NULL                               COMMENT '全局唯一ID',
  `job_group`           VARCHAR(60)              NOT NULL                               COMMENT 'Job分组(用于分派给同组下的Worker执行)',
  `job_name`            VARCHAR(60)              NOT NULL                               COMMENT 'Job名称',
  `job_type`            TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT 'Job类型：1-普通(Normal)；2-工作流(DAG)；',
  `job_state`           TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT 'Job状态：0-禁用；1-启用；',
  `job_handler`         TEXT                     NOT NULL                               COMMENT 'Job处理器(支持：处理器类的全限定名、Spring bean name、DAG表达式、处理器源码等)',
  `job_param`           TEXT                               DEFAULT NULL                 COMMENT 'Job参数',
  `retry_type`          TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '调度失败重试类型：0-不重试；1-只重试失败的Task；2-重试所有的Task；',
  `retry_count`         TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '调度失败可重试的最大次数',
  `retry_interval`      INT            UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '调度失败重试间隔(毫秒)，阶梯递增(square of sched_instance.retried_count)',
  `start_time`          DATETIME(3)                        DEFAULT NULL                 COMMENT 'Job起始时间(为空不限制)',
  `end_time`            DATETIME(3)                        DEFAULT NULL                 COMMENT 'Job结束时间(为空不限制)',
  `trigger_type`        TINYINT        UNSIGNED  NOT NULL                               COMMENT '触发器类型：1-Cron表达式；2-指定时间；3-固定周期；4-任务依赖；',
  `trigger_value`       VARCHAR(255)             NOT NULL                               COMMENT '触发器配置(对应trigger_type)：1-Cron表达式；2-时间格式；3-{"period":"DAILY","start":"2018-12-06 00:00:00","step":1}；4-父任务job_id(多个逗号分隔)；',
  `execute_timeout`     INT            UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '执行超时时间(毫秒)，若大于0则执行超时会中断任务',
  `collided_strategy`   TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '冲突策略(如果上一次调度未完成，下一次调度执行策略)：1-并行执行；2-串行执行；3-覆盖上次任务（取消上次任务，执行本次任务）；4-丢弃本次任务；',
  `misfire_strategy`    TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '过期策略：1-触发最近一次；2-丢弃；3-触发所有；',
  `route_strategy`      TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '任务分派给哪一个worker的路由策略：1-轮询；2-随机；3-简单的哈希；4-一致性哈希；5-本地优先；6-广播；',
  `last_trigger_time`   BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT '最近一次的触发时间(毫秒时间戳)',
  `next_trigger_time`   BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT '下一次的触发时间(毫秒时间戳)',
  `next_scan_time`      DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '下一次的扫描时间',
  `failed_scan_count`   TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '连续失败的扫描次数，连续失败次数达到阈值后自动禁用(set job_state=0)',
  `remark`              VARCHAR(255)                       DEFAULT NULL                 COMMENT '备注',
  `version`             INT            UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '行记录版本号',
  `is_deleted`          TINYINT        UNSIGNED            DEFAULT '0'                  COMMENT '是否已删除：0-否；NULL-是(用NULL来解决因软删引起的唯一索引冲突问题)；',
  `updated_by`          VARCHAR(60)                        DEFAULT NULL                 COMMENT '更新人',
  `created_by`          VARCHAR(60)                        DEFAULT NULL                 COMMENT '创建人',
  `updated_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jobid` (`job_id`),
  UNIQUE KEY `uk_jobgroup_jobname_isdeleted` (`job_group`, `job_name`, `is_deleted`),
  KEY `ix_jobstate_nexttriggertime` (`job_state`, `next_trigger_time`) COMMENT '用于扫表',
  KEY `ix_createdat` (`created_at`),
  KEY `ix_updatedat` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='调度配置表';

CREATE TABLE `sched_depend` (
  `id`                  BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `parent_job_id`       BIGINT         UNSIGNED  NOT NULL                               COMMENT '父job_id',
  `child_job_id`        BIGINT         UNSIGNED  NOT NULL                               COMMENT '子job_id',
  `sequence`            INT            UNSIGNED  NOT NULL                               COMMENT '序号(从1开始)',
  `updated_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parentjobid_childjobid` (`parent_job_id`, `child_job_id`),
  UNIQUE KEY `uk_childjobid_sequence` (`child_job_id`, `sequence`),
  KEY `ix_createdat` (`created_at`),
  KEY `ix_updatedat` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='调度依赖表';

CREATE TABLE `sched_instance` (
  `id`                  BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `instance_id`         BIGINT         UNSIGNED  NOT NULL                               COMMENT '全局唯一ID',
  `rnstance_id`         BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT 'root instance_id(Retry、Depend、Workflow)',
  `pnstance_id`         BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT 'parent instance_id(Retry、Depend、Workflow)',
  `wnstance_id`         BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT 'job_type为Workflow生成的lead instance_id',
  `job_id`              BIGINT         UNSIGNED  NOT NULL                               COMMENT 'sched_job.job_id',
  `trigger_time`        BIGINT         UNSIGNED  NOT NULL                               COMMENT '触发时间(毫秒时间戳)',
  `run_type`            TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '运行类型：1-SCHEDULE；2-DEPEND；3-RETRY；4-MANUAL(手动触发)；',
  `run_state`           TINYINT        UNSIGNED  NOT NULL                               COMMENT '运行状态：10-待运行；20-运行中；30-已暂停；40-已完成；50-已取消；',
  `run_start_time`      DATETIME(3)                        DEFAULT NULL                 COMMENT '运行开始时间',
  `run_end_time`        DATETIME(3)                        DEFAULT NULL                 COMMENT '运行结束时间',
  `run_duration`        BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT '运行时长(毫秒)',
  `retried_count`       TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '已重试的次数(the maximum value is sched_job.retry_count)',
  `attach`              VARCHAR(1024)                      DEFAULT NULL                 COMMENT '附加信息',
  `version`             INT            UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '行记录版本号',
  `updated_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_instanceid` (`instance_id`),
  UNIQUE KEY `uk_jobid_triggertime_runtype` (`job_id`, `trigger_time`, `run_type`),
  KEY `ix_runstate_triggertime` (`run_state`, `trigger_time`) COMMENT '用于扫表',
  KEY `ix_triggertime` (`trigger_time`),
  KEY `ix_rnstanceid` (`rnstance_id`),
  KEY `ix_pnstanceid` (`pnstance_id`),
  KEY `ix_wnstanceid` (`wnstance_id`),
  KEY `ix_createdat` (`created_at`),
  KEY `ix_updatedat` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='调度实例表';

CREATE TABLE `sched_task` (
  `id`                  BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `task_id`             BIGINT         UNSIGNED  NOT NULL                               COMMENT '全局唯一ID',
  `instance_id`         BIGINT         UNSIGNED  NOT NULL                               COMMENT 'sched_instance.instance_id',
  `task_no`             INT            UNSIGNED  NOT NULL                               COMMENT '当前任务序号(从1开始)',
  `task_count`          INT            UNSIGNED  NOT NULL                               COMMENT '任务总数量',
  `task_param`          TEXT                               DEFAULT NULL                 COMMENT 'job_handler执行task的参数(参考sched_job.job_param)',
  `execute_start_time`  DATETIME(3)                        DEFAULT NULL                 COMMENT '执行开始时间',
  `execute_end_time`    DATETIME(3)                        DEFAULT NULL                 COMMENT '执行结束时间',
  `execute_duration`    BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT '执行时长(毫秒)',
  `execute_state`       TINYINT        UNSIGNED  NOT NULL                               COMMENT '执行状态：10-等待执行；20-正在执行；30-暂停执行；40-执行完成；50-实例化异常；51-校验失败；52-初始化异常；53-执行失败；54-执行异常；55-执行超时；56-执行冲突(sched_job.collided_strategy=3)；57-手动取消；58-广播未执行；',
  `execute_snapshot`    TEXT                               DEFAULT NULL                 COMMENT '保存的执行快照数据',
  `worker`              VARCHAR(255)                       DEFAULT NULL                 COMMENT '工作进程(JVM进程，GROUP:WORKER-ID:HOST:PORT)',
  `error_msg`           VARCHAR(1024)                      DEFAULT NULL                 COMMENT '执行错误信息',
  `version`             INT            UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '行记录版本号',
  `updated_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_taskid` (`task_id`),
  UNIQUE KEY `uk_instanceid_taskno` (`instance_id`, `task_no`),
  KEY `ix_createdat` (`created_at`),
  KEY `ix_updatedat` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='调度任务表';

CREATE TABLE `sched_workflow` (
  `id`                  BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `wnstance_id`         BIGINT         UNSIGNED  NOT NULL                               COMMENT 'sched_instance.wnstance_id',
  `cur_node`            VARCHAR(255)             NOT NULL                               COMMENT '当前任务节点(section:ordinal:name)',
  `pre_node`            VARCHAR(255)             NOT NULL                               COMMENT '前置任务节点(section:ordinal:name)',
  `sequence`            INT            UNSIGNED  NOT NULL                               COMMENT '序号(从1开始)',
  `run_state`           TINYINT        UNSIGNED  NOT NULL                               COMMENT '运行状态：10-待运行；20-运行中；30-已暂停；40-已完成；50-已取消；',
  `instance_id`         BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT '当前执行的sched_instance.instance_id(失败重试时会更新为重试的instance_id)',
  `updated_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wnstanceid_curnode_prenode` (`wnstance_id`, `cur_node`, `pre_node`),
  UNIQUE KEY `uk_wnstanceid_sequence` (`wnstance_id`, `sequence`),
  KEY `ix_createdat` (`created_at`),
  KEY `ix_updatedat` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='调度工作流表';

CREATE TABLE `sched_group` (
  `id`                  BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `group`               VARCHAR(60)              NOT NULL                               COMMENT '分组名(同sched_job.job_group)',
  `token`               VARCHAR(255)                       DEFAULT NULL                 COMMENT '密钥令牌，用于认证',
  `alarm_subscribers`   VARCHAR(512)                       DEFAULT NULL                 COMMENT '告警订阅人员列表',
  `updated_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group` (`group`),
  KEY `ix_createdat` (`created_at`),
  KEY `ix_updatedat` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='分组表';

CREATE TABLE `sched_user` (
  `id`                  BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `username`            VARCHAR(60)              NOT NULL                               COMMENT '用户名',
  `password`            VARCHAR(255)             NOT NULL                               COMMENT '密码',
  `groups`              VARCHAR(2048)            NOT NULL                               COMMENT '分组名(多个逗号分隔)',
  `updated_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`          DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `ix_createdat` (`created_at`),
  KEY `ix_updatedat` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户表';


-- ----------------------------
-- INITIALIZE TEST SAMPLES JOB
-- ----------------------------
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351000, 'default', 'noop-job',      'cn.ponfee.disjob.test.handler.NoopJobHandler',                  1, 1, 1, '',                                                                  1, '0/40 * * * * ?',                          unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351001, 'default', 'http-job',      'cn.ponfee.disjob.core.handle.impl.HttpJobHandler',              1, 1, 1, '{"method":"GET", "url":"https://www.baidu.com"}',                   1, '0/50 * * * * ?',                          unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351002, 'default', 'command-job',   'cn.ponfee.disjob.core.handle.impl.CommandJobHandler',           0, 1, 1, '{"cmdarray":["/bin/sh","-c","echo $(date +%Y/%m/%d)"]}',            1, '0/40 * * * * ?',                          unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351003, 'default', 'script-job',    'cn.ponfee.disjob.core.handle.impl.ScriptJobHandler',            0, 1, 1, '{"type":"SHELL", "script":"#!/bin/sh \necho hello-shell!"}',        1, '0/50 * * * * ?',                          unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351004, 'default', 'prime-count',   'cn.ponfee.disjob.test.handler.PrimeCountJobHandler',            1, 1, 1, '{\"m\":1,\"n\":1000000000,\"blockSize\":50000000,\"parallel\":4}',  2, '2022-10-06 22:53:00',                     unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351005, 'default', 'broadcast-job', 'cn.ponfee.disjob.test.handler.TestBroadcastJobHandler',         1, 1, 6, 'broadcast-job-param',                                               2, '2023-03-18 21:30:00',                     unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351006, 'default', 'workflow-job',  'AJobHandler,BJobHandler->CJobHandler->DJobHandler,EJobHandler', 1, 2, 1, '',                                                                  2, '2023-03-18 21:30:00',                     unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351007, 'default', 'depend-job',    'cn.ponfee.disjob.test.handler.PrimeCountJobHandler',            1, 1, 1, '{\"m\":1,\"n\":500000000,\"blockSize\":20000000,\"parallel\":3}',   4, '1003164910267351000,1003164910267351001', null                 );

INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351008, 'default', 'workflow-json-graph', '[{"source":"1:1:AJobHandler","target":"1:1:CJobHandler"},{"source":"1:1:AJobHandler","target":"1:1:DJobHandler"},{"source":"1:1:BJobHandler","target":"1:1:DJobHandler"},{"source":"1:1:BJobHandler","target":"1:1:EJobHandler"}]', 1, 2, '', 2, '2023-03-18 21:30:00', unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351009, 'default', 'prime-count-dag', 'cn.ponfee.disjob.test.handler.PrimeCountJobHandler -> cn.ponfee.disjob.test.handler.PrimeAccumulateJobHandler', 1, 2, '{\"m\":1,\"n\":2000000000,\"blockSize\":100000000,\"parallel\":3}', 2, '2023-09-02 18:00:00', unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_type`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351010, 'default', 'groovy-job', 'cn.ponfee.disjob.core.handle.impl.GroovyJobHandler', 1, 1, 'import java.util.*; savepoint.save(executingTask.getTaskId(),UUID.randomUUID().toString()); return "execute at: " + new Date() + jobHandler.toString()', 1, '0/50 * * * * ?', unix_timestamp()*1000);

-- depend job config
INSERT INTO `sched_depend` (`child_job_id`, `parent_job_id`, `sequence`) VALUES (1003164910267351007, 1003164910267351000, 1);
INSERT INTO `sched_depend` (`child_job_id`, `parent_job_id`, `sequence`) VALUES (1003164910267351007, 1003164910267351001, 2);


COMMIT;
