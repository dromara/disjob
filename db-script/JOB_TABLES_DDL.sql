/*
 distributed scheduler database
*/

-- ----------------------------
-- CREATE DATABASE
-- ----------------------------
-- SET global validate_password_policy=LOW;
-- SET sql_mode="NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION";
-- DROP DATABASE IF EXISTS distributed_scheduler;
CREATE DATABASE IF NOT EXISTS distributed_scheduler DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE distributed_scheduler;


-- ----------------------------
-- USER PRIVILEGES
-- ----------------------------
-- GRANT ALL PRIVILEGES ON distributed_scheduler.* TO 'distributed_scheduler'@'%' IDENTIFIED BY 'distributed_scheduler';
CREATE USER 'distributed_scheduler'@'%' IDENTIFIED BY 'distributed_scheduler';
GRANT ALL PRIVILEGES ON distributed_scheduler.* TO 'distributed_scheduler'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;


-- ----------------------------
-- CREATE TABLE
-- ----------------------------
CREATE TABLE `sched_job` (
    `id`                   bigint(20)    unsigned  NOT NULL AUTO_INCREMENT                                        COMMENT '自增主键ID',
    `job_id`               bigint(20)    unsigned  NOT NULL                                                       COMMENT '全局唯一ID',
    `job_group`            varchar(30)             NOT NULL                                                       COMMENT 'Job分组(用于分配给同组下的Worker执行)',
    `job_name`             varchar(60)             NOT NULL                                                       COMMENT 'Job名称',
    `job_handler`          text                    NOT NULL                                                       COMMENT 'Job处理器(实现处理器接口的全限定类名或源代码)',
    `job_state`            tinyint(1)    unsigned  NOT NULL DEFAULT '0'                                           COMMENT 'Job状态：0-已禁用；1-已启用；',
    `job_param`            text                    DEFAULT NULL                                                   COMMENT 'Job参数',
    `retry_type`           tinyint(3)    unsigned  NOT NULL DEFAULT '0'                                           COMMENT '调度失败重试类型：0-不重试；1-重试所有的Task；2-只重试失败的Task；',
    `retry_count`          tinyint(3)    unsigned  NOT NULL DEFAULT '0'                                           COMMENT '调度失败可重试的最大次数',
    `retry_interval`       int(11)       unsigned  NOT NULL DEFAULT '0'                                           COMMENT '调度失败重试间隔(毫秒)，阶梯递增(square of sched_track.retried_count)',
    `start_time`           datetime                DEFAULT NULL                                                   COMMENT 'Job起始时间(为空不限制)',
    `end_time`             datetime                DEFAULT NULL                                                   COMMENT 'Job结束时间(为空不限制)',
    `trigger_type`         tinyint(3)    unsigned  NOT NULL                                                       COMMENT '触发器类型：1-Crontab方式；2-指定时间执行一次；3-周期性执行；4-任务依赖；',
    `trigger_conf`         varchar(255)            NOT NULL                                                       COMMENT '触发器配置(对应trigger_type)：1-Crontab表达式；2-时间格式；3-{"period":"DAILY","start":"2018-12-06 00:00:00","step":1}；4-父任务job_id(多个逗号分隔)；',
    `execute_timeout`      int(11)       unsigned  NOT NULL DEFAULT '0'                                           COMMENT '执行超时时间(毫秒)，若大于0则执行超时会中断任务',
    `collision_strategy`   tinyint(3)    unsigned  NOT NULL DEFAULT '1'                                           COMMENT '冲突策略(如果上一次调度未完成，下一次调度执行策略)：1-并行；2-串行；3-覆盖(先取消上一次的执行)；4-丢弃；',
    `misfire_strategy`     tinyint(3)    unsigned  NOT NULL DEFAULT '1'                                           COMMENT '过期策略：1-触发最近一次；2-丢弃；3-触发所有；',
    `route_strategy`       tinyint(3)    unsigned  NOT NULL DEFAULT '1'                                           COMMENT '任务分配给哪一个worker的路由策略：1-轮询；2-随机；3-简单的哈希；4-一致性哈希；5-本地优先；',
    `weight_score`         tinyint(3)    unsigned  NOT NULL DEFAULT '1'                                           COMMENT 'Job的权重分数，用于分配各Job的调度资源(分数越高表示需要占用的资源越多)',
    `last_trigger_time`    bigint(20)    unsigned  DEFAULT NULL                                                   COMMENT '最近一次的触发时间(毫秒时间戳)',
    `next_trigger_time`    bigint(20)    unsigned  DEFAULT NULL                                                   COMMENT '下一次的触发时间(毫秒时间戳)',
    `next_scan_time`       datetime(3)             NOT NULL DEFAULT CURRENT_TIMESTAMP(3)                          COMMENT '下一次的扫描时间',
    `alarm_subscribers`    varchar(512)            DEFAULT NULL                                                   COMMENT '告警订阅人员列表',
    `remark`               varchar(255)            DEFAULT NULL                                                   COMMENT '备注',
    `version`              int(11)       unsigned  NOT NULL DEFAULT '1'                                           COMMENT '行记录版本号',
    `is_deleted`           tinyint(1)    unsigned  DEFAULT '0'                                                    COMMENT '是否已删除：0-否；NULL-是(用NULL来解决因软删引起的唯一索引冲突问题)；',
    `updated_by`           varchar(60)             DEFAULT NULL                                                   COMMENT '更新人',
    `created_by`           varchar(60)             DEFAULT NULL                                                   COMMENT '创建人',
    `updated_at`           datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_at`           datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP                             COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_id` (`job_id`),
    KEY `ix_job_group_job_name_is_deleted` (`job_group`, `job_name`, `is_deleted`),
    KEY `ix_job_state_next_trigger_time` (`job_state`, `next_trigger_time`) COMMENT '用于扫表',
    KEY `ix_created_at` (`created_at`),
    KEY `ix_updated_at` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='任务配置表';

CREATE TABLE `sched_track` (
    `id`                   bigint(20)    unsigned  NOT NULL AUTO_INCREMENT                                        COMMENT '自增主键ID',
    `track_id`             bigint(20)    unsigned  NOT NULL                                                       COMMENT '全局唯一ID',
    `parent_track_id`      bigint(20)    unsigned  DEFAULT NULL                                                   COMMENT 'run_type IN (DEPEND, RETRY)时的父ID',
    `job_id`               bigint(20)    unsigned  NOT NULL                                                       COMMENT 'sched_job.job_id',
    `trigger_time`         bigint(20)    unsigned  NOT NULL                                                       COMMENT '触发时间(毫秒时间戳)',
    `run_type`             tinyint(3)    unsigned  NOT NULL DEFAULT '1'                                           COMMENT '运行类型：1-SCHEDULE；2-DEPEND；3-RETRY；4-MANUAL；',
    `run_state`            tinyint(3)    unsigned  NOT NULL                                                       COMMENT '运行状态：10-待运行；20-运行中；30-已暂停；40-已完成；50-已取消；',
    `run_start_time`       datetime(3)             DEFAULT NULL                                                   COMMENT '运行开始时间',
    `run_end_time`         datetime(3)             DEFAULT NULL                                                   COMMENT '运行结束时间',
    `run_duration`         bigint(20)    unsigned  DEFAULT NULL                                                   COMMENT '运行时长(毫秒)',
    `retried_count`        tinyint(3)    unsigned  NOT NULL DEFAULT '0'                                           COMMENT '已重试的次数(the maximum value is sched_job.retry_count)',
    `version`              int(11)       unsigned  NOT NULL DEFAULT '1'                                           COMMENT '行记录版本号',
    `updated_at`           datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_at`           datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP                             COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_track_id` (`track_id`),
    UNIQUE KEY `uk_job_id_trigger_time_run_type` (`job_id`, `trigger_time`, `run_type`),
    KEY `ix_parent_track_id` (`parent_track_id`),
    KEY `ix_run_state_trigger_time` (`run_state`, `trigger_time`) COMMENT '用于扫表',
    KEY `ix_created_at` (`created_at`),
    KEY `ix_updated_at` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='记录任务触发执行及生命周期的追踪';

CREATE TABLE `sched_task` (
    `id`                   bigint(20)    unsigned  NOT NULL AUTO_INCREMENT                                        COMMENT '自增主键ID',
    `task_id`              bigint(20)              NOT NULL                                                       COMMENT '全局唯一ID',
    `track_id`             bigint(20)    unsigned  NOT NULL                                                       COMMENT 'sched_track.track_id',
    `task_param`           text                    DEFAULT NULL                                                   COMMENT 'job_handler执行task的参数(参考sched_job.job_param)',
    `execute_start_time`   datetime(3)             DEFAULT NULL                                                   COMMENT '执行开始时间',
    `execute_end_time`     datetime(3)             DEFAULT NULL                                                   COMMENT '执行结束时间',
    `execute_duration`     bigint(20)    unsigned  DEFAULT NULL                                                   COMMENT '执行时长(毫秒)',
    `execute_state`        tinyint(3)    unsigned  NOT NULL                                                       COMMENT '执行状态：10-等待执行；20-正在执行；30-暂停执行；40-正常完成；50-实例化失败取消；51-校验失败取消；52-初始化异常取消；53-执行失败取消；54-执行异常取消；55-执行超时取消；56-数据不一致取消；57-执行冲突取消(sched_job.collision_strategy=3)；58-手动取消；',
    `execute_snapshot`     text                    DEFAULT NULL                                                   COMMENT '保存的执行快照数据',
    `worker`               varchar(255)            DEFAULT NULL                                                   COMMENT '工作进程(JVM进程，GROUP:WORKER-ID:HOST:PORT)',
    `error_msg`            varchar(4096)           DEFAULT NULL                                                   COMMENT '执行错误信息',
    `version`              int(11)       unsigned  NOT NULL DEFAULT '1'                                           COMMENT '行记录版本号',
    `updated_at`           datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_at`           datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP                             COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `ix_track_id` (`track_id`),
    KEY `ix_created_at` (`created_at`),
    KEY `ix_updated_at` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='具体执行的任务数据';

CREATE TABLE `sched_lock` (
    `id`                   bigint(20)    unsigned  NOT NULL AUTO_INCREMENT                                        COMMENT '自增主键ID',
    `name`                 varchar(50)             NOT NULL                                                       COMMENT '锁名称',
    `updated_at`           datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_at`           datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP                             COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `ix_created_at` (`created_at`),
    KEY `ix_updated_at` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用于扫表时多个机器独占锁';

CREATE TABLE `sched_depend` (
    `id`                   bigint(20)    unsigned  NOT NULL AUTO_INCREMENT                                        COMMENT '自增主键ID',
    `parent_job_id`        bigint(20)    unsigned  NOT NULL                                                       COMMENT '父job_id',
    `child_job_id`         bigint(20)    unsigned  NOT NULL                                                       COMMENT '子job_id',
    `updated_at`           datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_at`           datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP                             COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_parent_job_id_child_job_id` (`parent_job_id`, `child_job_id`),
    KEY `ix_child_job_id` (`child_job_id`),
    KEY `ix_created_at` (`created_at`),
    KEY `ix_updated_at` (`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖表';


-- ----------------------------
-- INITIALIZE DATA
-- ----------------------------
INSERT INTO sched_lock(`name`) VALUES ('scan_triggering_job');
INSERT INTO sched_lock(`name`) VALUES ('scan_waiting_track');
INSERT INTO sched_lock(`name`) VALUES ('scan_running_track');

-- ----------------------------
-- INITIALIZE TEST SAMPLES JOB
-- ----------------------------
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_param`, `trigger_type`, `trigger_conf`, `misfire_strategy`, `next_trigger_time`) VALUES (3988904755200, 'default', 'noop-job',    'cn.ponfee.scheduler.core.handle.impl.NoopJobHandler',             1, '',                                                                  1, '0/30 * * * * ?',      2, unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_param`, `trigger_type`, `trigger_conf`, `misfire_strategy`, `next_trigger_time`) VALUES (3988904755300, 'default', 'http-job',    'cn.ponfee.scheduler.core.handle.impl.HttpJobHandler',             1, '{"method":"GET", "url":"https://www.baidu.com"}',                   1, '0/15 * * * * ?',      2, unix_timestamp()*1000);
INSERT INTO `sched_job` (`job_id`, `job_group`, `job_name`, `job_handler`, `job_state`, `job_param`, `trigger_type`, `trigger_conf`, `misfire_strategy`, `next_trigger_time`) VALUES (4236701614080, 'default', 'prime-count', 'cn.ponfee.scheduler.samples.common.handler.PrimeCountJobHandler', 1, '{\"m\":1,\"n\":6000000000,\"blockSize\":100000000,\"parallel\":7}', 2, '2022-10-06 22:53:00', 3, unix_timestamp()*1000);
