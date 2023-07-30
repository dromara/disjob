[![Blog](https://img.shields.io/badge/blog-@Ponfee-informational.svg?logo=Pelican)](http://www.ponfee.cn)
[![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![JDK](https://img.shields.io/badge/jdk-8+-green.svg)](https://www.oracle.com/java/technologies/downloads/#java8)
[![Build status](https://github.com/ponfee/disjob/workflows/build-with-maven/badge.svg)](https://github.com/ponfee/disjob/actions)
[![Maven Central](https://img.shields.io/badge/maven--central-2.0.0-orange.svg?style=plastic&logo=apachemaven)](https://central.sonatype.com/namespace/cn.ponfee)

**`简体中文`** | [English](README.en.md)

# Disjob

## Introduction

一个分布式的任务调度框架，除了具备常规的分布式任务调度功能外，还提供：任务拆分、暂停/取消运行中的任务、恢复执行被暂停的任务、失败重试、广播任务、任务依赖、工作流任务(DAG)、管理器与执行器分离部署等能力。

轻量级，简单易用，特别适合长任务的执行。有较好的伸缩性、扩展性、稳定性，历经生产检验。

## Architecture

- 架构图

![Architecture](docs/images/architecture.jpg)

- 工程结构

```Plain Text
disjob                                                    # 主项目
├── disjob-admin                                          # 基于Ruoyi框架二次开发的Disjob后台管理系统
├── disjob-bom                                            # Maven project bom module
├── disjob-common                                         # 工具包
├── disjob-core                                           # 任务调度相关的核心类（如数据模型、枚举类、抽象层接口等）
├── disjob-dispatch                                       # 任务分发模块
│   ├── disjob-dispatch-api                               # 任务分发的抽象接口层
│   ├── disjob-dispatch-http                              # 任务分发的Http实现
│   └── disjob-dispatch-redis                             # 任务分发的Redis实现
├── disjob-id                                             # 分布式ID生成
├── disjob-registry                                       # Server(Supervisor & Worker)注册模块
│   ├── disjob-registry-api                               # Server注册的抽象接口层
│   ├── disjob-registry-consul                            # Server注册的Consul实现
│   ├── disjob-registry-etcd                              # Server注册的Etcd实现
│   ├── disjob-registry-nacos                             # Server注册的Nacos实现
│   ├── disjob-registry-redis                             # Server注册的Redis实现
│   └── disjob-registry-zookeeper                         # Server注册的Zookeeper实现
├── disjob-reports                                        # 聚合各个模块的测试覆盖率报告
├── disjob-samples                                        # Samples项目
│   ├── disjob-samples-common                             # 存放使用范例中用到的公共代码，包括使用到的一些公共配置文件等
│   ├── disjob-samples-merged                             # Supervisor与Worker合并部署的范例（Spring boot应用）
│   └── disjob-samples-separately                         # Supervisor与Worker分离部署的范例模块
│       ├── disjob-samples-separately-supervisor          # Supervisor单独部署的范例（Spring boot应用）
│       ├── disjob-samples-separately-worker-frameless    # Worker单独部署的范例（非Spring-boot应用，直接main方法启动）
│       └── disjob-samples-separately-worker-springboot   # Worker单独部署的范例（Spring boot应用）
├── disjob-supervisor                                     # Supervisor代码（Spring-boot应用）
├── disjob-test                                           # 用于辅助测试
└── disjob-worker                                         # Worker代码
```

## Features

- 分为管理器(Supervisor)和执行器(Worker)两种角色，Supervisor与Worker可分离部署
- Supervisor与Worker通过注册中心相互发现，支持的注册中心有：Redis、Consul、Nacos、Zookeeper、Etcd
- Supervisor负责生成任务，把任务分发给Worker执行，支持的任务分发方式有：Redis、Http
- 需要指定Job的分组(job-group)，Job的任务只会分发给指定组的Worker执行
- 提供拆分任务的能力，重写拆分方法[JobHandler#split](disjob-core/src/main/java/cn/ponfee/disjob/core/handle/JobSplitter.java)即可拆分为多个任务，实现任务分治及并行执行
- 支持暂停和取消运行中的任务，已暂停的任务可恢复继续执行，执行失败的任务支持重试
- 支持任务保存(checkpoint)其执行状态，让手动或异常暂停的任务能从上一次的执行状态中恢复继续执行
- 支持广播任务，广播任务会分发给job-group下的所有worker执行
- 支持Job间的依赖，多个Job配置好依赖关系后便会按既定的依赖顺序依次执行
- 支持DAG工作流，可把jobHandler配置为复杂的DAG表达式，如：A->B,C,(D->E)->D,F->G

## [Download From Maven Central](https://central.sonatype.com/namespace/cn.ponfee)

> **注意**: 最近 [aliyun](https://developer.aliyun.com/mvn/search) 那边的镜像仓受Maven中央仓库网络限制，部分依赖可能会从中央仓库同步文件失败，如果依赖查找不到(即无法下载)请在`settings.xml`文件中删除aliyun mirror的配置(不建议使用aliyun maven mirror)

```xml
<dependency>
  <groupId>cn.ponfee</groupId>
  <artifactId>disjob-{xxx}</artifactId>
  <version>2.0.0</version>
</dependency>
```

## Build From Source

```bash
./mvnw clean install -DskipTests -Dcheckstyle.skip=true -U
```

## Quick Start

1. IDE分别导入项目(分为两个独立的项目，共用一个`git`仓库)
  - [主项目](pom.xml)
  - [samples项目](disjob-samples/pom.xml)

2. 运行仓库代码提供的SQL脚本[mysql-schema.sql](mysql-schema.sql)创建数据库表(也可直接运行[内置Mysql](disjob-test/src/main/java/cn/ponfee/disjob/test/db/EmbeddedMysqlServerMariaDB.java)，启动时会自动初始化SQL脚本)
  - [MacOS报“Library not loaded”错误信息参考](disjob-test/src/main/DB/MariaDB/MariaDB.md)

3. 在[pom文件](disjob-samples/disjob-samples-common/pom.xml)中选择注册中心及任务分发的具体实现(默认redis注册中心、http任务分发)
  - 在pom中更改maven依赖即可：disjob-registry-{xxx}、disjob-dispatch-{xxx}
  - 项目已内置一些本地启动的server：
    - [内置redis-server](disjob-test/src/main/java/cn/ponfee/disjob/test/redis/EmbeddedRedisServerKstyrc.java)
    - [内置consul-server](disjob-registry/disjob-registry-consul/src/test/java/cn/ponfee/disjob/registry/consul/EmbeddedConsulServerPszymczyk.java)
    - [内置nacos-server](disjob-registry/disjob-registry-nacos/src/test/java/cn/ponfee/disjob/registry/nacos/EmbeddedNacosServerTestcontainers.java)（依赖本地docker环境）
    - [内置etcd-server](disjob-registry/disjob-registry-etcd/src/test/java/cn/ponfee/disjob/registry/etcd/EmbeddedEtcdServerTestcontainers.java)（依赖本地docker环境）
    - [内置zookeeper-server](disjob-registry/disjob-registry-zookeeper/src/test/java/cn/ponfee/disjob/registry/zookeeper/EmbeddedZookeeperServer.java)
    - [内置Mysql & Redis的合体](disjob-samples/disjob-samples-common/src/test/java/cn/ponfee/disjob/samples/MysqlAndRedisServerStarter.java)

4. 修改配置文件
  - 数据库配置：[Mysql](disjob-samples/conf-supervisor/application-mysql.yml)
  - 注册中心配置：只需配置选择的注册中心即可，如[Consul注册中心](disjob-samples/disjob-samples-common/src/main/resources/application-consul.yml)、[Redis注册中心](disjob-samples/disjob-samples-common/src/main/resources/application-redis.yml)
  - 任务分发配置：[Redis分发](disjob-samples/disjob-samples-common/src/main/resources/application-redis.yml)、若选择Http分发方式可无需配置
  - 其它可按需配置(不配置则会使用默认值)：[supervisor](disjob-samples/conf-supervisor/)、[worker](disjob-samples/conf-worker/)、[web](disjob-samples/disjob-samples-common/src/main/resources)
  - 非Spring-boot的Worker应用配置文件：[worker-conf.yml](disjob-samples/disjob-samples-separately/disjob-samples-separately-worker-frameless/src/main/resources/worker-conf.yml)

5. 编写自己的任务处理器[PrimeCountJobHandler](disjob-samples/disjob-samples-common/src/main/java/cn/ponfee/disjob/samples/common/handler/PrimeCountJobHandler.java)，并继承[JobHandler](disjob-core/src/main/java/cn/ponfee/disjob/core/handle/JobHandler.java)

6. 启动[samples项目](disjob-samples)下的各应用，包括
  - [Supervisor与Worker合并部署的Spring boot应用](disjob-samples/disjob-samples-merged/src/main/java/cn/ponfee/disjob/samples/merged/MergedApplication.java)
  - [Supervisor单独部署的Spring boot应用](disjob-samples/disjob-samples-separately/disjob-samples-separately-supervisor/src/main/java/cn/ponfee/disjob/samples/supervisor/SupervisorApplication.java)
  - [Worker单独部署的Spring boot应用](disjob-samples/disjob-samples-separately/disjob-samples-separately-worker-springboot/src/main/java/cn/ponfee/disjob/samples/worker/WorkerApplication.java)
  - [Worker单独部署的非Spring-boot应用，直接运行Main方法](disjob-samples/disjob-samples-separately/disjob-samples-separately-worker-frameless/src/main/java/cn/ponfee/disjob/samples/worker/Main.java)
  - 说明：
    - 已配置不同端口，可同时启动(多个Server组成分布式集群调度环境)
    - 可以在开发工具中运行启动类，也可直接运行构建好的jar包
```java
@EnableSupervisor
@EnableWorker
public class MergedApplication extends AbstractSamplesApplication {
  public static void main(String[] args) {
    SpringApplication.run(MergedApplication.class, args);
  }
}
```

7. 执行以下curl命令添加任务(任选一台运行中的Supervisor应用替换`localhost:8081`)
  - `triggerValue`修改为大于当前时间的日期值以便即将触发(如当前时间点的下一分钟)
  - `jobHandler`支持：类的全限定名、Spring bean name、DAG表达式、源码

```bash
curl --location --request POST 'http://localhost:8081/api/job/add' \
--header 'Content-Type: application/json' \
--data-raw '{
    "jobGroup": "default",
    "jobName": "prime-counter",
    "jobHandler": "cn.ponfee.disjob.test.handler.PrimeCountJobHandler",
    "jobState": 1,
    "jobParam": "{\"m\":1,\"n\":6000000000,\"blockSize\":100000000,\"parallel\":7}",
    "triggerType": 2,
    "triggerValue": "2022-10-06 12:00:00"
}'
```

8. 查询库表验证任务是否添加成功，以及可查看任务的执行信息

```sql
-- 刚CURL添加的任务会落入该表中
SELECT * FROM sched_job;

-- 查看任务的执行信息
SELECT * from sched_instance;
SELECT * from sched_task;

-- 可执行以下SQL让该Job再次触发执行
UPDATE sched_job SET job_state=1, last_trigger_time=NULL, next_trigger_time=(unix_timestamp()*1000+2000) WHERE job_name='prime-counter';
```

- 也可执行以下CURL命令手动触发执行一次(任选一台运行中的Supervisor替换`localhost:8081`，jobId替换为待触发执行的job)

```bash
curl --location --request POST 'http://localhost:8081/api/job/trigger?jobId=1003164910267351004' \
--header 'Content-Type: application/json'
```

## Contributing

如有发现bug、更优的实现方案、新特性等，可提交PR或新建[Issues](../../issues)。

## Todo List

- [x] 扩展注册中心：Zookeeper、Etcd、Nacos
- [x] 工作流任务(Workflow DAG)
- [x] 任务管理后台、账户体系及权限控制
- [ ] 搭建一个关于项目使用说明的文档站点
- [ ] 在线查看任务实时运行日志
- [ ] 告警订阅：邮件、短信、电话、飞书、钉钉、微信
- [ ] 可视化监控BI(Dashboard)
- [ ] 增加多种Checkpoint的支持：File System、Hadoop、RocksDB
