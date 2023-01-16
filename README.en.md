[![Blog](https://img.shields.io/badge/blog-@ponfee-informational.svg)](http://www.ponfee.cn)
[![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![JDK](https://img.shields.io/badge/jdk-8+-green.svg)](https://www.oracle.com/java/technologies/downloads/#java8)
[![Build status](https://github.com/ponfee/distributed-scheduler/workflows/build-with-maven/badge.svg)](https://github.com/ponfee/distributed-scheduler/actions)
[![Maven Central](https://img.shields.io/badge/maven--central-1.7-orange.svg?style=plastic&logo=apachemaven)](https://mvnrepository.com/search?q=cn.ponfee)

**`English`** | [简体中文](README.md)

# Distributed Scheduler

## Introduction

A distributed job scheduler framework, in addition to the conventional distributed task scheduling function, it also provides splitting of custom subtasks, customized control of long tasks in execution, DAG task dependency, supervisor and worker separate deployment, and so on.

Lightweight, easy to use, especially suitable for the execution of long tasks. Scalability, extensibility, and stability, and has been runed in production.

## Architecture

- architecture diagram

![Architecture](doc/images/architecture.jpg)

- code structure

```Plain Text
distributed-scheduler
├── scheduler-common                                         # Tools
├── scheduler-core                                           # Core classes code of task scheduling
├── scheduler-dispatch                                       # Task dispatch module
│   ├── scheduler-dispatch-api                               # Abstract interface layer for task dispatch
│   ├── scheduler-dispatch-http                              # Http implementation of task dispatch
│   └── scheduler-dispatch-redis                             # Redis implementation of task dispatch
├── scheduler-registry                                       # Server(supervisor & worker) registry module
│   ├── scheduler-registry-api                               # Server registry abstract interface layer
│   ├── scheduler-registry-consul                            # Server registry implementation based consul
│   ├── scheduler-registry-nacos                             # Server registry implementation based nacos
│   ├── scheduler-registry-redis                             # Server registry implementation based redis
│   ├── scheduler-registry-etcd                              # Server registry implementation based etcd
│   └── scheduler-registry-zookeeper                         # Server registry implementation based zookeeper
├── scheduler-samples                                        # Samples module
│   ├── scheduler-samples-common                             # Common configuration and codes of samples
│   ├── scheduler-samples-merged                             # Sample of merged deployment supervisor and worker(spring boot application)
│   └── scheduler-samples-separately                         # Sample of separated deployment supervisor and worker
│       ├── scheduler-samples-separately-supervisor          # Sample of only deployment supervisor(spring boot application)
│       ├── scheduler-samples-separately-worker-frameless    # Sample of only deployment worker(start by java main class)
│       └── scheduler-samples-separately-worker-springboot   # Sample of only deployment worker(spring boot application)
├── scheduler-supervisor                                     # Supervisor code(run in spring container environment)
└── scheduler-worker                                         # Worker code
```

## Features

- Defined two server roles: Supervisor and Worker, they can be deployed separately
- Supervisor and Worker are decoupled through the registration center. The currently supported registration centers are: Redis, Consul, Nacos, Zookeeper, Etcd
- Supervisor sends tasks to Workers in the way of task distribution. The currently supported task distribution methods are: Redis, Http
- Support task grouping (job-group), the task will be distributed to the specified group of Workers for execution
- Custom split tasks, override [JobHandler#split](scheduler-core/src/main/java/cn/ponfee/scheduler/core/handle/JobSplitter.java) to split a job to many tasks
- Provides automatic saving (checkpoint) of task execution snapshots, so that execution information is not lost, and tasks interrupted due to abnormalities can be continued to execute
- Provides the ability to control tasks during execution, and can suspend/cancel the tasks in progress at any time, and can also resume the execution of suspended tasks
- Provides the ability to execute tasks dependently. After multiple tasks build a DAG dependency relationship, the tasks will be executed sequentially according to the established dependency order

## [Download From Maven Central](https://mvnrepository.com/search?q=cn.ponfee)

> [Note](https://developer.aliyun.com/mvn/search): If it cannot download, please remove **aliyun maven central mirror** configuration from the `settings.xml` file.

```xml
<dependency>
  <groupId>cn.ponfee</groupId>
  <artifactId>scheduler-{xxx}</artifactId>
  <version>1.7</version>
</dependency>
```

## Build From Source

```bash
./mvnw clean package -DskipTests -Dcheckstyle.skip=true -U
```

## Quick Start

1. Run the SQL script provided by the warehouse code to create the database table: [db-script/JOB_TABLES_DDL.sql](db-script/JOB_TABLES_DDL.sql)(Also can direct run [embed mysql-server](scheduler-test/src/main/java/cn/ponfee/scheduler/test/db/EmbeddedMysqlServerMariaDB.java))

2. Modify configuration files such as Mysql, Redis, Consul, Nacos, Zookeeper, Etcd: [scheduler-samples-common/src/main/resources/](scheduler-samples/scheduler-samples-common/src/main/resources/)
- if use default localhost configuration([e.g consul localhost:8500](scheduler-registry/scheduler-registry-consul/src/main/java/cn/ponfee/scheduler/registry/consul/configuration/ConsulRegistryProperties.java)), you can not add the resource config file
- non-web application worker configuration: [worker-conf.yml](scheduler-samples/scheduler-samples-separately/scheduler-samples-separately-worker-frameless/src/main/resources/worker-conf.yml)

3. Create a job handler class [PrimeCountJobHandler](scheduler-samples/scheduler-samples-common/src/main/java/cn/ponfee/scheduler/samples/common/handler/PrimeCountJobHandler.java), and extends [JobHandler](scheduler-core/src/main/java/cn/ponfee/scheduler/core/handle/JobHandler.java)

4. Startup there applications [scheduler-samples/](scheduler-samples/): 

```Plain Text
 1）scheduler-samples-merged                        # Applicaion of merged deployment supervisor and worker
 2）scheduler-samples-separately-supervisor         # Spring boot application of only deployment supervisor
 3）scheduler-samples-separately-worker-springboot  # Spring boot application of only deployment worker
 4）scheduler-samples-separately-worker-frameless   # Non-web application of only deployment worker
```

- Different ports have been configured and can be started at the same time
- You can run the startup class in the development tool, or directly run the built jar package
- select registry center and dispatch mode [use by pom](scheduler-samples/scheduler-samples-common/pom.xml) import
- Embedded same servers can direct startup on local
    - [embedded redis server](scheduler-test/src/main/java/cn/ponfee/scheduler/test/redis/EmbeddedRedisServerKstyrc.java)
    - [embedded consul server](scheduler-registry/scheduler-registry-consul/src/test/java/cn/ponfee/scheduler/registry/consul/EmbeddedConsulServerPszymczyk.java)
    - [embedded nacos server](scheduler-registry/scheduler-registry-nacos/src/test/java/cn/ponfee/scheduler/registry/nacos/EmbeddedNacosServerTestcontainers.java)
    - [embedded etcd server](scheduler-registry/scheduler-registry-etcd/src/test/java/cn/ponfee/scheduler/registry/etcd/EmbeddedEtcdServerTestcontainers.java)
    - [embedded zookeeper server](scheduler-registry/scheduler-registry-zookeeper/src/test/java/cn/ponfee/scheduler/registry/zookeeper/EmbeddedZookeeperServer.java)

```java
@EnableSupervisor
@EnableWorker
public class SchedulerApplication extends AbstractSchedulerSamplesApplication {
  public static void main(String[] args) {
    SpringApplication.run(SchedulerApplication.class, args);
  }
}
```

5. Execute the following curl command to add tasks (select any running Supervisor application to replace `localhost:8081`)
- `triggerConf` modified to  next minute of the current time
- `jobHandler` is the fully qualified name of the newly written task processor class (also supports source code)

```bash
curl --location --request POST 'http://localhost:8081/api/job/add' \
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

6. Query the database table to verify whether the task is added successfully, and view the execution information of the task:

```sql
-- Query the sched_job data added by curl  
SELECT * FROM sched_job;

-- Query the job execution data
SELECT * from sched_track;
SELECT * from sched_task;

-- The following SQL can be executed to trigger the execution of the JOB again
UPDATE sched_job SET job_state=1, misfire_strategy=3, last_trigger_time=NULL, next_trigger_time=1664944641000 WHERE job_name='PrimeCountJobHandler';
```

- You can also execute the following CURL command to manually trigger execution (select a supervisor to replace `localhost:8081`)

```bash
curl --location --request POST 'http://localhost:8081/api/job/trigger?jobId=4236701614080' \
--header 'Content-Type: application/json'
```

## Contributing

If you find bugs, or better implementation solutions, or new features, etc. you can submit PR or create [Issues](../../issues).

## Todo List

- [x] JobHandler decoupling: The JobHandler code is deploy in the Worker application, provides http api to verification and split tasks [WorkerRemote](scheduler-worker/src/main/java/cn/ponfee/scheduler/worker/rpc/WorkerServiceProvider.java)
- [x] Extended registry: Zookeeper, Etcd, Nacos
- [ ] Task management background Web UI, account system and authority control, visual monitoring BI
- [ ] Add support for multiple checkpoints: File System, Hadoop, RocksDB
- [ ] When the machine has multiple network cards, the host ip of the specified network card is obtained
