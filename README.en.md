[![Blog](https://img.shields.io/badge/blog-@ponfee-informational.svg)](http://www.ponfee.cn)
[![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![JDK](https://img.shields.io/badge/jdk-8+-green.svg)](https://www.oracle.com/java/technologies/downloads/#java8)
[![Build status](https://github.com/ponfee/disjob/workflows/build-with-maven/badge.svg)](https://github.com/ponfee/disjob/actions)
[![Maven Central](https://img.shields.io/badge/maven--central-2.0.0-orange.svg?style=plastic&logo=apachemaven)](https://central.sonatype.com/namespace/cn.ponfee)

**`English`** | [简体中文](README.md)

# Disjob

## Introduction

A distributed job scheduling framework, in addition to the conventional distributed task scheduling function, it also provides splitting subtasks, control of tasks in execution, task dependency, broadcast execution, workflow task(DAG), supervisor and worker separate deployment, and so on.

Lightweight, easy to use, especially suitable for the execution of long tasks. Scalability, extensibility, and stability, and has been run in production.

## Architecture

- architecture diagram

![Architecture](docs/images/architecture.jpg)

- code structure

```Plain Text
disjob                                                    # Main project
├── disjob-admin                                          # Disjob backend admin system based Ruoyi framework
├── disjob-bom                                            # Maven project bom module
├── disjob-common                                         # Tools
├── disjob-core                                           # Core classes code of task scheduling
├── disjob-dispatch                                       # Task dispatch module
│   ├── disjob-dispatch-api                               # Abstract interface layer for task dispatch
│   ├── disjob-dispatch-http                              # Http implementation of task dispatch
│   └── disjob-dispatch-redis                             # Redis implementation of task dispatch
├── disjob-id                                             # Distributed ID generator
├── disjob-registry                                       # Server(supervisor & worker) registry module
│   ├── disjob-registry-api                               # Server registry abstract interface layer
│   ├── disjob-registry-consul                            # Server registry implementation based consul
│   ├── disjob-registry-etcd                              # Server registry implementation based etcd
│   ├── disjob-registry-nacos                             # Server registry implementation based nacos
│   ├── disjob-registry-redis                             # Server registry implementation based redis
│   └── disjob-registry-zookeeper                         # Server registry implementation based zookeeper
├── disjob-reports                                        # aggregate code coverage report
├── disjob-samples                                        # Samples project
│   ├── disjob-samples-common                             # Common configuration and codes of samples
│   ├── disjob-samples-merged                             # Sample of merged deployment supervisor and worker(spring boot application)
│   └── disjob-samples-separately                         # Sample of separated deployment supervisor and worker
│       ├── disjob-samples-separately-supervisor          # Sample of only deployment supervisor(spring boot application)
│       ├── disjob-samples-separately-worker-frameless    # Sample of only deployment worker(start by java main class)
│       └── disjob-samples-separately-worker-springboot   # Sample of only deployment worker(spring boot application)
├── disjob-supervisor                                     # Supervisor code(run in spring container environment)
├── disjob-test                                           # use for testing
└── disjob-worker                                         # Worker code
```

## Features

- Defined two server roles: Supervisor and Worker, they can be deployed separately
- Supervisor and Worker are decoupled through the registration center. The currently supported registration centers are: Redis, Consul, Nacos, Zookeeper, Etcd
- Supervisor sends tasks to Workers in the way of task distribution. The currently supported task distribution methods are: Redis, Http
- Support task grouping (job-group), the task will be distributed to the specified group of Workers for execution
- Custom split tasks, override [JobHandler#split](disjob-core/src/main/java/cn/ponfee/disjob/core/handle/JobSplitter.java) to split a job to many tasks
- Provides automatic saving (checkpoint) of task execution snapshots, so that execution information is not lost, and tasks interrupted due to abnormalities can be continued to execute
- Provides the ability to control tasks during execution, and can suspend/cancel the tasks in progress at any time, and can also resume the execution of suspended tasks
- Provides the ability to execute tasks dependently. After multiple tasks build dependency relationship, the tasks will be executed sequentially according to the established dependency order
- Supported DAG workflow task，set jobHandler to a dag expression, e.g. A->B,C,(D->E)->D,F->G

## [Download From Maven Central](https://central.sonatype.com/namespace/cn.ponfee)

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

1. Imports project to IDE (Contains two projects, shared the git repository)
  - [main project](pom.xml)
  - [samples project](disjob-samples/pom.xml)

2. Run the SQL script file[mysql-schema.sql](mysql-disjob.sql) to create database table(Also can direct run [embed mysql-server](disjob-test/src/main/java/cn/ponfee/disjob/test/db/EmbeddedMysqlServerMariaDB.java), auto init sql script on startup)
- [MacBook M1 error "Library not loaded" ref](disjob-test/src/main/DB/MariaDB/MariaDB.md)

3. Modify configuration files such as [Mysql](disjob-samples/conf-supervisor/application-mysql.yml), [Redis](disjob-samples/disjob-samples-common/src/main/resources/application-redis.yml), [Consul](disjob-samples/disjob-samples-common/src/main/resources/application-consul.yml) and so on.
- if use default localhost configuration([e.g consul localhost:8500](disjob-registry/disjob-registry-consul/src/main/java/cn/ponfee/disjob/registry/consul/configuration/ConsulRegistryProperties.java)), you can not add the resource config file(same as Nacos/Zookeeper/Etcd)
- non spring-boot application of worker configuration: [worker-conf.yml](disjob-samples/disjob-samples-separately/disjob-samples-separately-worker-frameless/src/main/resources/worker-conf.yml)

4. Create a job handler class [PrimeCountJobHandler](disjob-samples/disjob-samples-common/src/main/java/cn/ponfee/disjob/samples/common/handler/PrimeCountJobHandler.java), and extends [JobHandler](disjob-core/src/main/java/cn/ponfee/disjob/core/handle/JobHandler.java)

5. Startup these [sample applications](disjob-samples)
  - [Spring boot application of merged Supervisor&Worker](disjob-samples/disjob-samples-merged/src/main/java/cn/ponfee/disjob/samples/merged/MergedApplication.java)
  - [Spring boot application of Supervisor](disjob-samples/disjob-samples-separately/disjob-samples-separately-supervisor/src/main/java/cn/ponfee/disjob/samples/supervisor/SupervisorApplication.java)
  - [Spring boot application of Worker](disjob-samples/disjob-samples-separately/disjob-samples-separately-worker-springboot/src/main/java/cn/ponfee/disjob/samples/worker/WorkerApplication.java)
  - [Frameless application of Worker, direct run main](disjob-samples/disjob-samples-separately/disjob-samples-separately-worker-frameless/src/main/java/cn/ponfee/disjob/samples/worker/Main.java)
  - Notes:
    - Different ports have been configured and can be started at the same time
    - You can run the startup class in the development tool, or directly run the built jar package
    - select registry center and dispatch mode [use by pom](disjob-samples/disjob-samples-common/pom.xml) import
  - Embedded same servers can direct startup on local: 
    - [embedded redis-server](disjob-test/src/main/java/cn/ponfee/disjob/test/redis/EmbeddedRedisServerKstyrc.java)
    - [embedded consul-server](disjob-registry/disjob-registry-consul/src/test/java/cn/ponfee/disjob/registry/consul/EmbeddedConsulServerPszymczyk.java)
    - [embedded nacos-server](disjob-registry/disjob-registry-nacos/src/test/java/cn/ponfee/disjob/registry/nacos/EmbeddedNacosServerTestcontainers.java)(depends on local docker)
    - [embedded etcd-server](disjob-registry/disjob-registry-etcd/src/test/java/cn/ponfee/disjob/registry/etcd/EmbeddedEtcdServerTestcontainers.java)(depends on local docker)
    - [embedded zookeeper-server](disjob-registry/disjob-registry-zookeeper/src/test/java/cn/ponfee/disjob/registry/zookeeper/EmbeddedZookeeperServer.java)
    - [embedded Mysql & Redis](disjob-samples/disjob-samples-common/src/test/java/cn/ponfee/disjob/samples/MysqlAndRedisServerStarter.java)

```java
@EnableSupervisor
@EnableWorker
public class MergedApplication extends AbstractSamplesApplication {
  public static void main(String[] args) {
    SpringApplication.run(MergedApplication.class, args);
  }
}
```

6. Execute the following curl command to add tasks (select any running Supervisor application to replace `localhost:8081`)
- `triggerValue` modified to  next minute of the current time
- `jobHandler` supported: the fully qualified class name, spring bean name, DAG Expression, source code

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

7. Query the database table to verify whether the task is added successfully, and view the execution information of the task

```sql
-- Query the sched_job data added by curl  
SELECT * FROM sched_job;

-- Query the job execution data
SELECT * from sched_instance;
SELECT * from sched_task;

-- The following SQL can be executed to trigger the execution of the Job again
UPDATE sched_job SET job_state=1, last_trigger_time=NULL, next_trigger_time=(unix_timestamp()*1000+2000) WHERE job_name='prime-counter';
```

- You can also execute the following CURL command to manually trigger execution (select a supervisor to replace `localhost:8081`)

```bash
curl --location --request POST 'http://localhost:8081/api/job/trigger?jobId=1003164910267351004' \
--header 'Content-Type: application/json'
```

## Contributing

If you find bugs, or better implementation solutions, or new features, etc. you can submit PR or create [Issues](../../issues).

## Todo List

- [x] Extended registry: Zookeeper, Etcd, Nacos
- [x] Workflow task(Workflow DAG)
- [x] Task management background system, account and authority
- [ ] Build a project document web site
- [ ] Monitor real-time executing logs of tasks online
- [ ] alarm subscribe: Email, SMS, Voice, Lark, Ding Talk, WeChat
- [ ] visual monitoring BI(Dashboard)
- [ ] Add support for multiple checkpoints: File System, Hadoop, RocksDB
