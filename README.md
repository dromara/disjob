[![Blog](https://img.shields.io/badge/blog-@Ponfee-informational.svg?logo=Pelican)](http://www.ponfee.cn)
[![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![JDK](https://img.shields.io/badge/jdk-8+-green.svg)](https://www.oracle.com/java/technologies/downloads/#java8)
[![Build status](https://github.com/ponfee/disjob/workflows/build-with-maven/badge.svg)](https://github.com/ponfee/disjob/actions)
[![Maven Central](https://img.shields.io/badge/maven--central-2.0.1-orange.svg?style=plastic&logo=apachemaven)](https://central.sonatype.com/namespace/cn.ponfee)

**`简体中文`** | [English](README.en.md)

# Disjob

## Introduction

一个分布式的任务调度框架，除了具备常规的分布式任务调度功能外，还提供：暂停/取消运行中的任务、恢复执行被暂停的任务、任务拆分、失败重试、广播任务、任务依赖、工作流任务(DAG)、管理器与执行器分离部署、Web管理后台等能力。

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
│   ├── disjob-samples-merged-springboot                  # Supervisor与Worker合并部署的范例（Spring boot应用）
│   ├── disjob-samples-supervisor-springboot              # Supervisor单独部署的范例（Spring boot应用）
│   ├── disjob-samples-worker-frameless                   # Worker单独部署的范例（非Spring-boot应用，直接main方法启动）
│   └── disjob-samples-worker-springboot                  # Worker单独部署的范例（Spring boot应用）
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
- 提供Web管理后台，通过界面进行作业配置，任务监控等

## [Download From Maven Central](https://central.sonatype.com/namespace/cn.ponfee)

```xml
<dependency>
  <groupId>cn.ponfee</groupId>
  <artifactId>disjob-{xxx}</artifactId>
  <version>2.0.1</version>
</dependency>
```

## Build From Source

```bash
./mvnw clean install -DskipTests -Dcheckstyle.skip=true -U
```

## Quick Start

0. 管理后台演示地址：http://ponfee.cn:8000/ ，用户名/密码：`disjob/disjob123`

1. IDE分别导入项目(分为三个独立的项目，共用一个`git`仓库)
  - [主项目—disjob](pom.xml)
  - [样例项目—disjob-samples](disjob-samples/pom.xml)
  - [管理后台—disjob-admin](disjob-admin/pom.xml)

2. 运行仓库提供的两个SQL脚本：[mysql-disjob.sql](mysql-disjob.sql)、[mysql-disjob_admin.sql](disjob-admin/mysql-disjob_admin.sql)创建两个数据库表(也可直接运行[内置Mysql](disjob-test/src/main/java/cn/ponfee/disjob/test/db/EmbeddedMysqlServerMariaDB.java)，启动时会自动初始化两个SQL脚本)
  - [MacOS报“Library not loaded”错误信息参考](disjob-test/src/main/DB/MariaDB/MariaDB.md)
  - 内置的本地mysql server无需用户名密码即可使用

3. 在[pom文件](disjob-samples/disjob-samples-common/pom.xml)中选择注册中心及任务分发的具体实现(默认redis注册中心、http任务分发)
  - 在pom中更改maven依赖即可：disjob-registry-{xxx}、disjob-dispatch-{xxx}
  - 项目已内置一些本地启动的server：
    - [内置redis-server](disjob-test/src/main/java/cn/ponfee/disjob/test/redis/EmbeddedRedisServerKstyrc.java)
    - [内置consul-server](disjob-registry/disjob-registry-consul/src/test/java/cn/ponfee/disjob/registry/consul/EmbeddedConsulServerPszymczyk.java)
    - [内置nacos-server](disjob-registry/disjob-registry-nacos/src/test/java/cn/ponfee/disjob/registry/nacos/EmbeddedNacosServerTestcontainers.java)（依赖本地docker环境）
    - [内置etcd-server](disjob-registry/disjob-registry-etcd/src/test/java/cn/ponfee/disjob/registry/etcd/EmbeddedEtcdServerTestcontainers.java)（依赖本地docker环境）
    - [内置zookeeper-server](disjob-registry/disjob-registry-zookeeper/src/test/java/cn/ponfee/disjob/registry/zookeeper/EmbeddedZookeeperServer.java)
    - [内置Mysql & Redis的合体](disjob-samples/disjob-samples-common/src/test/java/cn/ponfee/disjob/samples/MysqlAndRedisServerStarter.java)（推荐使用该类来一次性启动本地mysql及redis）

4. 修改配置文件
  - 数据库配置：[Mysql](disjob-samples/conf-supervisor/application-mysql.yml)
  - 注册中心配置：只需配置选择的注册中心即可，如[Consul注册中心](disjob-samples/disjob-samples-common/src/main/resources/application-consul.yml)、[Redis注册中心](disjob-samples/disjob-samples-common/src/main/resources/application-redis.yml)
  - 任务分发配置：[Redis分发](disjob-samples/disjob-samples-common/src/main/resources/application-redis.yml)、若选择Http分发方式可无需配置
  - 其它可按需配置(不配置则会使用默认值)：[supervisor](disjob-samples/conf-supervisor/)、[worker](disjob-samples/conf-worker/)、[web](disjob-samples/disjob-samples-common/src/main/resources)
  - 非Spring-boot的Worker应用配置文件：[worker-conf.yml](disjob-samples/disjob-samples-worker-frameless/src/main/resources/worker-conf.yml)

5. 启动[samples项目](disjob-samples)下的各应用，包括
  - [Supervisor与Worker合并部署的Spring boot应用](disjob-samples/disjob-samples-merged-springboot/src/main/java/cn/ponfee/disjob/samples/merged/MergedApplication.java)
  - [Supervisor单独部署的Spring boot应用](disjob-samples/disjob-samples-supervisor-springboot/src/main/java/cn/ponfee/disjob/samples/supervisor/SupervisorApplication.java)
  - [Worker单独部署的Spring boot应用](disjob-samples/disjob-samples-worker-springboot/src/main/java/cn/ponfee/disjob/samples/worker/WorkerApplication.java)
  - [Worker单独部署的非Spring-boot应用，直接运行Main方法](disjob-samples/disjob-samples-worker-frameless/src/main/java/cn/ponfee/disjob/samples/worker/WorkerFramelessMain.java)
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

6. 修改管理后台disjob-admin的配置
  - [pom.xml](disjob-admin/ruoyi-disjob/pom.xml)中更改maven依赖即可：disjob-registry-{xxx}、disjob-dispatch-{xxx}，默认是redis注册、http分发
  - [disjob mysql](disjob-admin/ruoyi-disjob/src/main/resources/application-disjob-mysql.yml)配置
  - [redis](disjob-admin/ruoyi-disjob/src/main/resources/application-disjob-redis.yml)配置
  - [disjob_admin mysql](disjob-admin/ruoyi-admin/src/main/resources/application-druid.yml)配置（使用的是druid数据源）
  - [可加@EnableWorker启用Worker角色](disjob-admin/ruoyi-disjob/src/main/java/cn/ponfee/disjob/admin/DisjobAdminConfiguration.java)（disjob-admin必须启用Supervisor角色）

7. 启动disjob-admin
  - [启动java类](disjob-admin/ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java)
  - 启动成功后浏览器访问 http://127.0.0.1:80/ 进入后台管理系统（用户名密码：admin/admin123）
  - 登录后在左侧菜单栏找到`调度管理`菜单，即可使用后台管理功能
    - 调度配置：查看、新增、修改等
    - 调度实例：具体时间点的运行实例，一个实例有多个task。鼠标向下滚动可看到第二个分页，第一个分页查询root实例并支持下钻，第二个分页查询所有实例

> **💡提示：若使用内置的mysql、redis，以上所有配置都无需修改即可启动各应用**

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
