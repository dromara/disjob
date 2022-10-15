[![Blog](https://img.shields.io/badge/blog-@ponfee-informational.svg)](http://www.ponfee.cn)
[![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build status](https://img.shields.io/badge/build-passing-success.svg)](https://github.com/ponfee/distributed-scheduler/actions)

**`简体中文`** | [**`English`**](README.en.md)

# Distributed Scheduler

## Introduction

一个分布式的任务调度框架，除了具备常规的分布式任务调度功能外，还提供自定义子任务的拆分、可对执行中的长任务自由控制、DAG任务依赖、管理器与执行器分离部署等能力。

轻量级，简单易用，特别适合长任务的执行。有较好的伸缩性，扩展性，稳定性，历经生产检验。

## Architecture

- 架构图

![Architecture](doc/images/architecture.jpg)

- 代码结构

```Plain Text
distributed-scheduler
├── scheduler-common                                         # 工具包(Tools)
├── scheduler-core                                           # 任务调度相关的核心类（如数据模型、枚举类、抽象层接口等）
├── scheduler-dispatch                                       # 任务分发模块
│   ├── scheduler-dispatch-api                               # 任务分发的抽象接口层
│   ├── scheduler-dispatch-http                              # 任务分发的Http实现
│   └── scheduler-dispatch-redis                             # 任务分发的Redis实现
├── scheduler-registry                                       # Server(Supervisor & Worker)注册模块
│   ├── scheduler-registry-api                               # Server注册的抽象接口层
│   ├── scheduler-registry-consul                            # Server注册的Consul实现
│   └── scheduler-registry-redis                             # Server注册的Redis实现
├── scheduler-samples                                        # 使用范例模块
│   ├── scheduler-samples-common                             # 存放使用范例中用到的公共代码，包括使用到的一些公共配置文件等
│   ├── scheduler-samples-merged                             # Supervisor与Worker合并部署的范例（Spring boot应用）
│   └── scheduler-samples-separately                         # Supervisor与Worker分离部署的范例模块
│       ├── scheduler-samples-separately-supervisor          # Supervisor单独部署的范例（Spring boot应用）
│       ├── scheduler-samples-separately-worker-frameless    # Worker单独部署的范例（不依赖Web容器，直接main方法启动）
│       └── scheduler-samples-separately-worker-springboot   # Worker单独部署的范例（Spring boot应用）
├── scheduler-supervisor                                     # Supervisor代码（依赖于Spring Web环境，需要引导Spring扫描该包目录）
└── scheduler-worker                                         # Worker代码
```

## Features

- 分为管理器(Supervisor)和执行器(Worker)两种角色，Supervisor与Worker可分离部署
- Supervisor、Worker通过注册中心进行解耦，目前支持的注册中心有：redis、consul
- Supervisor以任务分发方式把任务给到Worker，目前支持的任务分发方式有：redis、http
- 支持多种任务的分发算法，包括：round-robin、random、consistent-hash、local-priority
- 支持任务分组(job-group)，任务会分发给指定组的Worker执行
- 自定义拆分任务，实现[**`JobHandler#split`**](scheduler-core/src/main/java/cn/ponfee/scheduler/core/handle/JobSplitter.java)即可把一个大任务拆分为多个小任务，任务分治
- 提供任务执行快照的自动保存(checkpoint)，让执行信息不丢失，保证因异常中断的任务能得到继续执行
- 提供执行中的任务控制能力，可随时暂停/取消正在执行中的任务，亦可恢复执行被暂停的任务
- 提供任务依赖执行的能力，多个任务构建好DAG依赖关系后，任务便按既定的依赖顺序依次执行
- 支持常规的分布式任务调度功能，包括但不限于以下：
  - 自定义的线程池执行任务，复用系统资源及实现任务控制[**`WorkerThreadPool`**](scheduler-worker/src/main/java/cn/ponfee/scheduler/worker/base/WorkerThreadPool.java)
  - 支持多种任务的触发方式[**`TriggerType`**](scheduler-core/src/main/java/cn/ponfee/scheduler/core/enums/TriggerType.java)
  - 多种任务的重试类型[**`RetryType`**](scheduler-core/src/main/java/cn/ponfee/scheduler/core/enums/RetryType.java)
  - 多种misfire的处理策略[**`MisfireStrategy`**](scheduler-core/src/main/java/cn/ponfee/scheduler/core/enums/MisfireStrategy.java)
  - 同一任务上一次还未执行完成，下一次已到触发时间的执行冲突处理策略[**`CollisionStrategy`**](scheduler-core/src/main/java/cn/ponfee/scheduler/core/enums/CollisionStrategy.java)

## Build

```bash
./mvnw clean package -DskipTests -Dcheckstyle.skip -U
```

## Quick Start

1. 运行仓库代码提供的SQL脚本，创建数据库表：[**`db-script/JOB_TABLES_DDL.sql`**](db-script/JOB_TABLES_DDL.sql)

2. 修改Mysql、Redis、Consul等配置文件：[**`scheduler-samples-common/src/main/resources/`**](scheduler-samples/scheduler-samples-common/src/main/resources/)
- 如果不使用Redis做为注册中心及任务分发，则无需配置并可排除Maven依赖：`spring-boot-starter-data-redis`
- 如果不使用Consul做为注册中心，则无需配置
- 不依赖Web容器的Worker应用的配置文件是在[**`worker-conf.yml`**](scheduler-samples/scheduler-samples-separately/scheduler-samples-separately-worker-frameless/src/main/resources/worker-conf.yml)

3. 编写自己的任务处理器[**`PrimeCountJobHandler`**](scheduler-samples/scheduler-samples-common/src/main/java/cn/ponfee/scheduler/samples/common/handler/PrimeCountJobHandler.java)，并继承[**`JobHandler`**](scheduler-core/src/main/java/cn/ponfee/scheduler/core/handle/JobHandler.java)

4. 启动[**`scheduler-samples/`**](scheduler-samples/)目录下的各应用，包括：
```Plain Text
 1）scheduler-samples-merged                        # Supervisor与Worker合并部署的Spring boot应用
 2）scheduler-samples-separately-supervisor         # Supervisor单独部署的Spring boot应用
 3）scheduler-samples-separately-worker-springboot  # Worker单独部署的Spring boot应用
 4）scheduler-samples-separately-worker-frameless   # Worker单独部署，不依赖Web容器，直接运行Main方法启动
```
***Notes***
- 已配置不同端口，可同时启动
- 可以在开发工具中运行启动类，也可直接运行构建好的jar包
- 注册中心或任务分发的类型选择是在Spring boot启动类中切换注解
  - EnableRedisServerRegistry启用Redis做为注册中心
  - EnableConsulServerRegistry启用Consul做为注册中心
  - EnableRedisTaskDispatching启用Redis做任务分发
  - EnableHttpTaskDispatching启用Http做任务分发
```java
@EnableConfigurationProperties({
    SupervisorProperties.class,
    HttpProperties.class,
    ConsulProperties.class
})
@EnableSupervisor
@EnableRedisServerRegistry  // EnableRedisServerRegistry、EnableConsulServerRegistry
@EnableRedisTaskDispatching // EnableRedisTaskDispatching、EnableHttpTaskDispatching
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    },
    scanBasePackages = {
        "cn.ponfee.scheduler.samples.common.configuration",
        "cn.ponfee.scheduler.samples.supervisor.configuration",
        "cn.ponfee.scheduler.supervisor",
    }
)
public class SupervisorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupervisorApplication.class, args);
    }
}
```

5. 执行以下curl命令添加任务(选择任一运行中的Supervisor应用替换“localhost:8080”)
- triggerConf修改为大于当前时间的日期值以便即将触发(如当前时间的下一分钟)
- jobHandler为刚编写的任务处理器类的全限定名（也支持直接贴源代码）
```bash
curl --location --request POST 'http://localhost:8080/api/job/add' \
--header 'Content-Type: application/json' \
--data-raw '{
    "jobGroup": "default",
    "jobName": "PrimeCountJobHandler",
    "jobHandler": "cn.ponfee.scheduler.samples.common.handler.PrimeCountJobHandler",
    "jobState": 1,
    "jobParam": "{\"m\":1,\"n\":6000000000,\"blockSize\":100000000,\"parallel\":7}",
    "triggerType": 2,
    "triggerConf": "2022-10-06 12:00:00"
}'
```

6. 查询库表验证任务是否添加成功，以及可查看任务的执行信息：
```sql
-- 刚CURL添加的任务会落入该表中
SELECT * FROM sched_job;

-- 查看任务的执行信息
SELECT * from sched_track;
SELECT * from sched_task;

-- 可执行以下SQL让该JOB再次触发执行
UPDATE sched_job SET job_state=1, misfire_strategy=3, last_trigger_time=NULL, next_trigger_time=1664944641000 WHERE job_name='PrimeCountJobHandler';

-- 也可执行以下CURL命令手动触发执行(选择一台运行中的Supervisor替换“localhost:8080”，jobId替换为待触发执行的job)
curl --location --request POST 'http://localhost:8080/api/job/manual_trigger?jobId=4236701614080' \
--header 'Content-Type: application/json'
```

## Contributing

个人能力及精力有限，期待及乐于接受好的意见及建议，如有发现bug、更优的实现方案、新特性等，可提交PR或新建[**`Issues`**](../../issues)一起探讨。

## Todo List

- [ ] 扩展注册中心：Zookeeper、Etcd、Nacos
- [x] Handler解耦：Handler代码只在Worker服务中，Worker提供处理器校验及拆分任务的接口[**`WorkerRemote`**](scheduler-worker/src/main/java/cn/ponfee/scheduler/worker/rpc/WorkerRemote.java)
- [ ] 任务管理后台Web UI、账户体系及权限控制、可视化监控BI
- [ ] 增加多种Checkpoint的支持：File System、Hadoop、RocksDB
- [ ] 本机多网卡时，指定网卡的host ip获取
