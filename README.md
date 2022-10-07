[![Blog](https://img.shields.io/badge/blog-@ponfee-informational.svg)](http://www.ponfee.cn)
[![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Build status](https://img.shields.io/badge/build-passing-success.svg)]()
[![Tests status](https://img.shields.io/badge/tests-passing-success.svg)]()

**`简体中文`** | [**`English`**](README.en.md)

# Distributed Scheduler

## Introduction

一个分布式的任务调度框架，除了具备常规的分布式任务调度功能外，还提供自定义子任务的拆分、可对执行中的长任务自由控制、DAG任务依赖、管理器与执行器分离部署等能力。

轻量级，简单易用，特别适合长任务的执行。有较好的伸缩性，扩展性，稳定性，历经生产检验。

## Architecture

- 架构图

![Architecture](doc/images/architecture.jpg)

- 工程代码结构

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
- 支持任务分组(job-group)，任务由指定组的Worker才能执行
- 自定义拆分任务，实现[**`JobHandler#split`**](scheduler-core/src/main/java/cn/ponfee/scheduler/core/handle/JobSplitter.java)即可把大任务拆分为多个小任务，任务分治
- 提供任务执行快照的自动保存(checkpoint)，让执行信息不丢失，保证因异常中断的任务能得到继续执行
- 提供执行中的任务控制能力，可随时暂停/取消正在执行中的任务，亦可恢复执行被暂停的任务
- 提供任务依赖执行的能力，多个任务构建DAG依赖关系后，任务便按既定的依赖顺序依次执行
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
 - 如果不使用Redis做为注册中心及任务分发无需配置，并可排除Maven依赖：`spring-boot-starter-data-redis`
 - 如果不使用Consul做为注册中心无需配置
 - 无框架依赖的Worker应用的配置文件是在[**`worker-conf.yml`**](scheduler-samples/scheduler-samples-separately/scheduler-samples-separately-worker-frameless/src/main/resources/worker-conf.yml)

3. 启动[**`scheduler-samples/`**](scheduler-samples/)目录下的各应用，包括：
 - 已配置不同端口，可同时启动
 - 可以在开发工具中运行启动类，也可直接运行构建好的jar包
```Plain Text
 1）scheduler-samples-merged:                       Supervisor与Worker合并部署的Spring boot应用
 2）scheduler-samples-separately-supervisor:        Supervisor单独部署的Spring boot应用
 3）scheduler-samples-separately-worker-springboot: Worker单独部署的Spring boot应用
 4）scheduler-samples-separately-worker-frameless:  Worker单独部署，不依赖Web容器，直接运行Main方法启动
```
注册中心或任务分发的类型选择是在Spring boot启动类中切换注解([**`无框架依赖的Worker应用`**](scheduler-samples/scheduler-samples-separately/scheduler-samples-separately-worker-frameless/src/main/java/cn/ponfee/scheduler/worker/samples/Main.java)是在Main代码中切换)
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
@EnableRedisServerRegistry // EnableRedisServerRegistry、EnableConsulServerRegistry
@EnableRedisTaskDispatching // EnableRedisTaskDispatching、EnableHttpTaskDispatching
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    },
    scanBasePackages = {
        "cn.ponfee.scheduler.supervisor",
        "cn.ponfee.scheduler.samples.configuration"
    }
)
public class SupervisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupervisorApplication.class, args);
    }

}
```

4. 编写自己的任务处理器[**`PrimeCountJobHandler`**](scheduler-samples/scheduler-samples-common/src/main/java/cn/ponfee/scheduler/samples/PrimeCountJobHandler.java)，并继承[**`JobHandler`**](scheduler-core/src/main/java/cn/ponfee/scheduler/core/handle/JobHandler.java)类(范例中提供的是一个素数统计的处理器)
```java
/**
 * 统计任意0<m<=n的[m, n]的素数个数
 *
 * @author Ponfee
 */
public class PrimeCountJobHandler extends JobHandler<Void> {

    private static final long DEFAULT_BLOCK_SIZE = 100_000_000L; // 默认以每块1亿分批统计

    /**
     * 拆分任务，自由控制任务的拆分数量
     *
     * @param job the schedule job
     * @return task list
     * @throws JobException if split occur error
     */
    @Override
    public List<SplitTask> split(SchedJob job) throws JobException {
        JobParam jobParam = Jsons.fromJson(job.getJobParam(), JobParam.class);
        long m = jobParam.getM();
        long n = jobParam.getN();
        long blockSize = Optional.ofNullable(jobParam.getBlockSize()).orElse(DEFAULT_BLOCK_SIZE);
        Assert.isTrue(m > 0, "Number M must be greater than zero.");
        Assert.isTrue(n >= m, "Number N cannot less than M.");
        Assert.isTrue(blockSize > 0, "Block size must be greater than zero.");
        Assert.isTrue(jobParam.getParallel() > 0, "Parallel must be greater than zero.");

        int parallel = n == m ? 1 : (int) Math.min(((n - m) + blockSize - 1) / blockSize, jobParam.getParallel());
        List<SplitTask> result = new ArrayList<>(parallel);
        for (int i = 0; i < parallel; i++) {
            TaskParam taskParam = new TaskParam();
            taskParam.setStart(m + blockSize * i);
            taskParam.setBlockSize(blockSize);
            taskParam.setStep(blockSize * parallel);
            taskParam.setN(n);
            result.add(new SplitTask(Jsons.toJson(taskParam)));
        }
        return result;
    }

    /**
     * 执行任务的逻辑实现
     *
     * @param checkpoint the checkpoint
     * @return execute result
     * @throws Exception if execute occur error
     */
    @Override
    public Result<Void> execute(Checkpoint checkpoint) throws Exception {
        TaskParam taskParam = Jsons.fromJson(task().getTaskParam(), TaskParam.class);
        long start = taskParam.getStart();
        long blockSize = taskParam.getBlockSize();
        long step = taskParam.getStep();
        long n = taskParam.getN();
        Assert.isTrue(start > 0, "Start must be greater than zero.");
        Assert.isTrue(blockSize > 0, "Block size must be greater than zero.");
        Assert.isTrue(step > 0, "Step must be greater than zero.");
        Assert.isTrue(n > 0, "N must be greater than zero.");

        ExecuteSnapshot execution;
        if (StringUtils.isEmpty(task().getExecuteSnapshot())) {
            execution = new ExecuteSnapshot(start);
        } else {
            execution = Jsons.fromJson(task().getExecuteSnapshot(), ExecuteSnapshot.class);
            if (execution.getNext() == null || execution.isFinished()) {
                Assert.isTrue(execution.isFinished() && execution.getNext() == null, "Invalid execute snapshot data.");
                return Result.SUCCESS;
            }
        }

        long blockStep = blockSize - 1, next = execution.getNext();
        long lastTime = System.currentTimeMillis(), currTime;
        while (next <= n) {
            long count = Prime.MillerRabin.countPrimes(next, Math.min(next + blockStep, n));
            execution.increment(count);

            next += step;
            if (next > n) {
                execution.setNext(null);
                execution.setFinished(true);
            } else {
                execution.setNext(next);
            }

            currTime = System.currentTimeMillis();
            if (execution.isFinished() || (currTime - lastTime) > 5000) {
                checkpoint.checkpoint(task().getTaskId(), Jsons.toJson(execution));
            }
            lastTime = currTime;
        }
        return Result.SUCCESS;
    }

    @Data
    public static class JobParam implements Serializable {
        private long m;
        private long n;
        private Long blockSize; // 分块统计，每块的大小
        private int parallel;   // 并行度：子任务数量
    }

    @Data
    public static class TaskParam implements Serializable {
        private long start;
        private long blockSize;
        private long step;
        private long n;
    }

    @Data
    @NoArgsConstructor
    public static class ExecuteSnapshot implements Serializable {
        private Long next;
        private long count;
        private boolean finished;

        public ExecuteSnapshot(long start) {
            this.next = start;
            this.count = 0;
            this.finished = false;
        }

        public void increment(long delta) {
            this.count += delta;
        }
    }

}
```

5. 选择任一运行中的Supervisor应用替换“localhost:8080”，执行以下curl命令添加任务
 - triggerConf参数需要改成大于当前时间的日期值，请合理设置一个不久便能触发执行的时间(如当前时间的下一分钟)
 - jobHandler为刚编写的任务处理器类的全限定名（也可直接贴源代码）
```bash
curl --location --request POST 'http://localhost:8080/api/job/add' \
--header 'Content-Type: application/json' \
--data-raw '{
    "jobGroup": "default",
    "jobName": "PrimeCountJobHandler",
    "jobHandler": "cn.ponfee.scheduler.samples.PrimeCountJobHandler",
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

-- 也可执行以下CURL命令手动触发执行(选择一台运行中的Supervisor替换“localhost:8080”，jobId为替换为待触发执行的job)
curl --location --request POST 'http://localhost:8080/api/job/manual_trigger?jobId=4236701614080' \
--header 'Content-Type: application/json'
```

## Contributing

由于个人能力有限，本项目肯定存在未发现的缺陷，也一定有更优的实现方案。期待及乐于接受好的意见及建议，如有发现BUG或有你认为的新特性，可提交PR或新建[**`Issues`**](../../issues)一起探讨。

## Todo List

- 扩展注册中心：Zookeeper、Etcd、Nacos
- 任务管理后台Web UI及可视化监控BI
- 增加多种Checkpoint的支持：File System、Hadoop、RocksDB
- 本机多网卡时，指定网卡的host ip获取
