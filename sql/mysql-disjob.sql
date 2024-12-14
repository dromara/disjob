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
  `id`                    BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `job_id`                BIGINT         UNSIGNED  NOT NULL                               COMMENT '全局唯一ID',
  `group`                 VARCHAR(60)              NOT NULL                               COMMENT '分组名称(可以理解为一个应用的appid，此job只会分派给所属组的Worker执行)',
  `job_name`              VARCHAR(60)              NOT NULL                               COMMENT 'Job名称',
  `job_type`              TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT 'Job类型：1-常规；2-工作流(DAG)；',
  `job_state`             TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT 'Job状态：0-禁用；1-启用；',
  `job_executor`          TEXT                     NOT NULL                               COMMENT 'Job执行器(支持：执行器类的全限定名、Spring bean name、DAG表达式、执行器源码等)',
  `job_param`             TEXT                               DEFAULT NULL                 COMMENT 'Job参数',
  `retry_type`            TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '调度失败重试类型：0-不重试；1-只重试失败的Task；2-重试所有的Task；',
  `retry_count`           TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '调度失败可重试的最大次数',
  `retry_interval`        INT            UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '调度失败重试间隔(毫秒)，阶梯递增(square of sched_instance.retried_count)',
  `trigger_type`          TINYINT        UNSIGNED  NOT NULL                               COMMENT '触发器类型：1-Cron表达式；2-指定时间；3-指定周期；4-指定间隔；5-固定频率；6-固定延时；7-任务依赖；',
  `trigger_value`         VARCHAR(255)             NOT NULL                               COMMENT '触发器值(对应trigger_type)：1-Cron表达式；2-时间格式(2000-01-01 00:00:00)；3-{"period":"MONTHLY","start":"2000-01-01 00:00:00","step":1}；4-指定间隔秒数；5-固定频率秒数；6-固定延时秒数；7-父任务job_id(多个逗号分隔)；',
  `start_time`            DATETIME(3)                        DEFAULT NULL                 COMMENT 'Job有效起始时间(为空不限制)',
  `end_time`              DATETIME(3)                        DEFAULT NULL                 COMMENT 'Job有效终止时间(为空不限制)',
  `execute_timeout`       INT            UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '执行超时时间(毫秒)，若大于0则执行超时会中断任务',
  `collided_strategy`     TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '冲突策略(如果上一次调度未完成，下一次调度执行策略)：1-并发执行；2-顺序执行；3-覆盖上次任务（取消上次任务，执行本次任务）；4-丢弃本次任务；',
  `misfire_strategy`      TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '过期策略：1-立即触发执行一次；2-跳过所有被错过的；3-执行所有被错过的；',
  `route_strategy`        TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '任务分派给哪一个worker的路由策略：1-轮询；2-随机；3-简单的哈希；4-一致性哈希；5-本地优先；6-广播；',
  `shutdown_strategy`     TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT 'Worker关机的执行策略(如重新发布服务时)：1-恢复执行；2-暂停执行；3-取消执行；',
  `last_trigger_time`     BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT '最近一次的触发时间(毫秒时间戳)',
  `next_trigger_time`     BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT '下一次的触发时间(毫秒时间戳)',
  `next_scan_time`        DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '下一次的扫描时间',
  `scan_failed_count`     TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '连续失败的扫描次数，连续失败次数达到阈值后自动禁用(set job_state=0)',
  `remark`                VARCHAR(255)                       DEFAULT NULL                 COMMENT '备注',
  `version`               INT            UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '行记录版本号',
  `is_deleted`            BIGINT         UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '是否已删除：0-否；{id}-是(用id来解决因软删引起的唯一索引冲突问题)；',
  `updated_by`            VARCHAR(60)                        DEFAULT NULL                 COMMENT '更新人',
  `created_by`            VARCHAR(60)                        DEFAULT NULL                 COMMENT '创建人',
  `updated_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jobid` (`job_id`),
  UNIQUE KEY `uk_group_jobname_isdeleted` (`group`, `job_name`, `is_deleted`),
  KEY `ix_jobstate_nexttriggertime` (`job_state`, `next_trigger_time`) COMMENT '用于扫表',
  KEY `ix_updatedat` (`updated_at`),
  KEY `ix_createdat` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='作业配置表';

CREATE TABLE `sched_depend` (
  `id`                    BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `parent_job_id`         BIGINT         UNSIGNED  NOT NULL                               COMMENT '父job_id',
  `child_job_id`          BIGINT         UNSIGNED  NOT NULL                               COMMENT '子job_id',
  `created_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parentjobid_childjobid` (`parent_job_id`, `child_job_id`),
  KEY `ix_childjobid` (`child_job_id`),
  KEY `ix_createdat` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='作业依赖表';

CREATE TABLE `sched_instance` (
  `id`                    BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `instance_id`           BIGINT         UNSIGNED  NOT NULL                               COMMENT '全局唯一ID',
  `rnstance_id`           BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT 'root instance_id(Retry、Depend、Workflow)',
  `pnstance_id`           BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT 'parent instance_id(Retry、Depend、Workflow)',
  `wnstance_id`           BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT 'job_type为Workflow生成的lead instance_id',
  `job_id`                BIGINT         UNSIGNED  NOT NULL                               COMMENT 'sched_job.job_id',
  `trigger_time`          BIGINT         UNSIGNED  NOT NULL                               COMMENT '触发时间(毫秒时间戳)',
  `run_type`              TINYINT        UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '运行类型：1-SCHEDULE；2-DEPEND；3-RETRY；4-MANUAL(手动触发)；',
  `unique_flag`           BIGINT         UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '唯一标识(保证trigger_time唯一)：0-SCHEDULE/MANUAL；{instance_id}-其它场景；',
  `is_retrying`           TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '是否重试中：0-否；1-是；',
  `run_state`             TINYINT        UNSIGNED  NOT NULL                               COMMENT '运行状态：10-待运行；20-运行中；30-已暂停；40-已完成；50-已取消；',
  `run_start_time`        DATETIME(3)                        DEFAULT NULL                 COMMENT '运行开始时间',
  `run_end_time`          DATETIME(3)                        DEFAULT NULL                 COMMENT '运行结束时间',
  `retried_count`         TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '已重试的次数(the maximum value is sched_job.retry_count)',
  `next_scan_time`        DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '下一次的扫描时间',
  `workflow_cur_node`     VARCHAR(255)                       DEFAULT NULL                 COMMENT '工作流任务的当前节点(sched_workflow.cur_node，非工作流任务时为NULL)',
  `version`               INT            UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '行记录版本号',
  `updated_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_instanceid` (`instance_id`),
  UNIQUE KEY `uk_jobid_triggertime_runtype_uniqueflag` (`job_id`, `trigger_time`, `run_type`, `unique_flag`),
  KEY `ix_runstate_triggertime` (`run_state`, `trigger_time`) COMMENT '用于扫表',
  KEY `ix_pnstanceid` (`pnstance_id`),
  KEY `ix_wnstanceid` (`wnstance_id`),
  KEY `ix_updatedat` (`updated_at`),
  KEY `ix_createdat` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='调度实例表';

CREATE TABLE `sched_task` (
  `id`                    BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `task_id`               BIGINT         UNSIGNED  NOT NULL                               COMMENT '全局唯一ID',
  `instance_id`           BIGINT         UNSIGNED  NOT NULL                               COMMENT 'sched_instance.instance_id',
  `task_no`               INT            UNSIGNED  NOT NULL                               COMMENT '当前任务序号(从1开始)',
  `task_count`            INT            UNSIGNED  NOT NULL                               COMMENT '任务总数量',
  `task_param`            TEXT                               DEFAULT NULL                 COMMENT 'job_executor执行task的参数(参考sched_job.job_param)',
  `execute_start_time`    DATETIME(3)                        DEFAULT NULL                 COMMENT '执行开始时间',
  `execute_end_time`      DATETIME(3)                        DEFAULT NULL                 COMMENT '执行结束时间',
  `execute_state`         TINYINT        UNSIGNED  NOT NULL                               COMMENT '执行状态：10-等待执行；20-正在执行；30-暂停执行；40-执行完成；50-派发失败；51-初始化异常；52-执行失败；53-执行异常；54-执行超时；55-执行冲突；56-广播终止；57-执行终止；58-关机取消；59-手动取消；',
  `execute_snapshot`      TEXT                               DEFAULT NULL                 COMMENT '保存的执行快照数据',
  `worker`                VARCHAR(255)                       DEFAULT NULL                 COMMENT '工作进程(JVM进程，GROUP:WORKER-ID:HOST:PORT)',
  `start_request_id`      VARCHAR(32)                        DEFAULT NULL                 COMMENT 'Start task时的请求ID，用于start请求超时重试幂等',
  `dispatch_failed_count` TINYINT        UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '任务派发失败的次数(失败次数达到阈值后需要终止)',
  `error_msg`             VARCHAR(2048)                      DEFAULT NULL                 COMMENT '执行错误信息',
  `updated_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_taskid` (`task_id`),
  UNIQUE KEY `uk_instanceid_taskno` (`instance_id`, `task_no`),
  KEY `ix_updatedat` (`updated_at`),
  KEY `ix_createdat` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='调度任务表';

CREATE TABLE `sched_workflow` (
  `id`                    BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `wnstance_id`           BIGINT         UNSIGNED  NOT NULL                               COMMENT 'sched_instance.wnstance_id',
  `pre_node`              VARCHAR(255)             NOT NULL                               COMMENT '前置任务节点(section:ordinal:name)',
  `cur_node`              VARCHAR(255)             NOT NULL                               COMMENT '当前任务节点(section:ordinal:name)',
  `run_state`             TINYINT        UNSIGNED  NOT NULL                               COMMENT '运行状态：10-待运行；20-运行中；30-已暂停；40-已完成；50-已取消；',
  `instance_id`           BIGINT         UNSIGNED            DEFAULT NULL                 COMMENT '当前执行的sched_instance.instance_id(失败重试时会更新为重试的instance_id)',
  `updated_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wnstanceid_prenode_curnode` (`wnstance_id`, `pre_node`, `cur_node`),
  KEY `ix_updatedat` (`updated_at`),
  KEY `ix_createdat` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='调度工作流表';

CREATE TABLE `sched_group` (
  `id`                    BIGINT         UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT '自增主键ID',
  `group`                 VARCHAR(60)              NOT NULL                               COMMENT '分组名称(同sched_job.group)',
  `own_user`              VARCHAR(60)              NOT NULL                               COMMENT '负责人',
  `supervisor_token`      VARCHAR(60)              NOT NULL  DEFAULT ''                   COMMENT 'Supervisor访问Worker的密钥令牌',
  `worker_token`          VARCHAR(60)              NOT NULL  DEFAULT ''                   COMMENT 'Worker访问Supervisor的密钥令牌',
  `user_token`            VARCHAR(60)              NOT NULL  DEFAULT ''                   COMMENT 'User访问Supervisor Openapi接口的密钥令牌(`未部署Admin` 或 `提供类似开放平台` 时使用)',
  `dev_users`             VARCHAR(512)                       DEFAULT NULL                 COMMENT '开发人员(多个逗号分隔)',
  `alarm_users`           VARCHAR(512)                       DEFAULT NULL                 COMMENT '告警接收人员(多个逗号分隔)',
  `worker_context_path`   VARCHAR(100)             NOT NULL  DEFAULT '/'                  COMMENT '该组下的Worker服务的context-path',
  `web_hook`              VARCHAR(255)                       DEFAULT NULL                 COMMENT '告警web hook地址',
  `version`               INT            UNSIGNED  NOT NULL  DEFAULT '1'                  COMMENT '行记录版本号',
  `is_deleted`            BIGINT         UNSIGNED  NOT NULL  DEFAULT '0'                  COMMENT '是否已删除：0-否；{id}-是(用id来解决因软删引起的唯一索引冲突问题)；',
  `updated_by`            VARCHAR(60)                        DEFAULT NULL                 COMMENT '更新人',
  `created_by`            VARCHAR(60)                        DEFAULT NULL                 COMMENT '创建人',
  `updated_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间' ON UPDATE CURRENT_TIMESTAMP(3),
  `created_at`            DATETIME(3)              NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_isdeleted` (`group`, `is_deleted`),
  KEY `ix_updatedat` (`updated_at`),
  KEY `ix_createdat` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='分组表';


CREATE TABLE IF NOT EXISTS `sched_lock` (
  `id`                    BIGINT       UNSIGNED    NOT NULL  AUTO_INCREMENT               COMMENT 'auto increment primary key id',
  `name`                  VARCHAR(60)              NOT NULL                               COMMENT 'lock name',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Distributed lock based database';

CREATE TABLE IF NOT EXISTS `sched_snowflake` (
  `id`                    BIGINT        UNSIGNED   NOT NULL  AUTO_INCREMENT               COMMENT 'auto increment primary key id',
  `biz_tag`               VARCHAR(60)              NOT NULL                               COMMENT 'biz tag',
  `server_tag`            VARCHAR(128)             NOT NULL                               COMMENT 'server tag, for example ip:port',
  `worker_id`             INT           UNSIGNED   NOT NULL                               COMMENT 'snowflake worker-id',
  `heartbeat_time`        BIGINT        UNSIGNED   NOT NULL                               COMMENT 'last heartbeat time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biztag_servertag` (`biz_tag`, `server_tag`),
  UNIQUE KEY `uk_biztag_workerid` (`biz_tag`, `worker_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Allocate snowflake worker-id';

CREATE TABLE IF NOT EXISTS `sched_registry` (
  `id`                    BIGINT        UNSIGNED   NOT NULL  AUTO_INCREMENT               COMMENT 'auto increment primary key id',
  `namespace`             VARCHAR(60)              NOT NULL                               COMMENT 'registry namespace',
  `role`                  VARCHAR(30)              NOT NULL                               COMMENT 'role(worker, supervisor)',
  `server`                VARCHAR(255)             NOT NULL                               COMMENT 'server serialization',
  `heartbeat_time`        BIGINT        UNSIGNED   NOT NULL                               COMMENT 'last heartbeat time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_namespace_role_server` (`namespace`, `role`, `server`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Disjob registry based database';


-- ----------------------------
-- INITIALIZE SAMPLE TEST DATA
-- ----------------------------
INSERT INTO `sched_group` (`group`, `own_user`, `supervisor_token`, `worker_token`, `user_token`, `dev_users`) VALUES
  ('app-test', 'disjob', '20bb8b7f1cb94dc894b45546a7c2982f', '358678bfe34648f68b607036a27c6854', '1878f0158782423f9306e7d4c70c999c', 'admin,alice'),
  ('app-demo', 'admin', '', '', '', 'disjob')
;

INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351000, 'app-test', 'noop-job',      'cn.ponfee.disjob.test.executor.NoopJobExecutor',                     1, 1, 1, '',                                                               1, '0/40 * * * * ?',                          unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351001, 'app-test', 'http-job',      'cn.ponfee.disjob.worker.executor.impl.HttpJobExecutor',              1, 1, 1, '{"method":"GET", "url":"https://www.baidu.com"}',                1, '0/50 * * * * ?',                          unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351002, 'app-test', 'command-job',   'cn.ponfee.disjob.worker.executor.impl.CommandJobExecutor',           0, 1, 1, '{"cmdarray":["/bin/sh","-c","echo $(date +%Y/%m/%d)"]}',         1, '0/40 * * * * ?',                          unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351003, 'app-test', 'script-job',    'cn.ponfee.disjob.worker.executor.impl.ScriptJobExecutor',            0, 1, 1, '{"type":"SHELL", "script":"#!/bin/sh \necho hello-shell!"}',     1, '0/50 * * * * ?',                          unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351004, 'app-test', 'prime-count',   'cn.ponfee.disjob.test.executor.PrimeCountJobExecutor',               1, 1, 1, '{\"m\":1,\"n\":100000000,\"blockSize\":3000000,\"parallel\":2}', 2, '2022-10-06 22:53:00',                     unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351005, 'app-test', 'broadcast-job', 'cn.ponfee.disjob.test.executor.TestBroadcastJobExecutor',            1, 1, 6, 'broadcast-job-param',                                            2, '2023-03-18 21:30:00',                     unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351006, 'app-test', 'workflow-job',  'AJobExecutor,BJobExecutor->CJobExecutor->DJobExecutor,EJobExecutor', 1, 2, 1, '',                                                               2, '2023-03-18 21:30:00',                     unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351007, 'app-test', 'depend-job',    'cn.ponfee.disjob.test.executor.PrimeCountJobExecutor',               1, 1, 1, '{\"m\":1,\"n\":50000000,\"blockSize\":2000000,\"parallel\":2}',  7, '1003164910267351000,1003164910267351006', null                 );
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351020, 'app-test', 'fixed-rate',    'cn.ponfee.disjob.test.executor.NoopJobExecutor',                     1, 1, 1, 'fixed-rate demo',                                                5, '20',                                      unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351021, 'app-test', 'fixed-delay',   'cn.ponfee.disjob.test.executor.NoopJobExecutor',                     1, 1, 5, 'fixed-delay demo',                                               6, '20',                                      unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351008, 'app-test', 'workflow-json-graph', '["AJobExecutor -> CJobExecutor","AJobExecutor -> DJobExecutor","BJobExecutor -> DJobExecutor","BJobExecutor -> EJobExecutor"]', 1, 2, 2, '', 2, '2023-03-18 21:30:00', unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351009, 'app-test', 'prime-count-dag', 'cn.ponfee.disjob.test.executor.PrimeCountJobExecutor -> cn.ponfee.disjob.test.executor.PrimeAccumulateJobExecutor', 1, 2, 1, '{\"m\":1,\"n\":500000000,\"blockSize\":3000000,\"parallel\":3}', 2, '2023-09-02 18:00:00', unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `group`, `job_name`, `job_executor`, `job_state`, `job_type`, `route_strategy`, `job_param`, `trigger_type`, `trigger_value`, `next_trigger_time`) VALUES (1003164910267351010, 'app-test', 'groovy-job', 'cn.ponfee.disjob.worker.executor.impl.GroovyJobExecutor', 1, 1, 4, 'import java.util.*; savepoint.save(new Date().toString() + ": " + UUID.randomUUID().toString()); return "taskId=" + executionTask.getTaskId() + ", executeAt=" + new Date() + ", jobExecutor=" + jobExecutor.toString();', 1, '0/50 * * * * ?', unix_timestamp()*1000);

INSERT INTO `sched_depend` (`child_job_id`, `parent_job_id`) VALUES (1003164910267351007, 1003164910267351000);
INSERT INTO `sched_depend` (`child_job_id`, `parent_job_id`) VALUES (1003164910267351007, 1003164910267351006);



COMMIT;
