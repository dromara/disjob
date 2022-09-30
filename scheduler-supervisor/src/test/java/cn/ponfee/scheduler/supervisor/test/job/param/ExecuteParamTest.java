package cn.ponfee.scheduler.supervisor.test.job.param;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.TaskExecutor;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author Ponfee
 */
public class ExecuteParamTest {

    @Test
    public void testFastjson() {
        ExecuteParam param = new ExecuteParam(Operations.TRIGGER, 1, 2, 3, 4);
        Worker worker = new Worker("g", "i", "h", 8081);
        param.setWorker(worker);
        param.taskExecutor(new TaskExecutor() {
            @Override
            public Result execute(Checkpoint checkpoint) {
                return null;
            }
        });
        String json = param.toString();
        System.out.println(json);
        Assertions.assertEquals(json, "{\"operation\":\"TRIGGER\",\"taskId\":1,\"trackId\":2,\"jobId\":3,\"triggerTime\":4,\"worker\":{\"host\":\"h\",\"port\":8081,\"group\":\"g\",\"instanceId\":\"i\"}}");
        Assertions.assertEquals(json, JSON.parseObject(json, ExecuteParam.class).toString());
    }

    @Test
    public void testJackson() {
        ExecuteParam param = new ExecuteParam(Operations.TRIGGER, 1, 2, 3, 4);
        Worker worker = new Worker("g", "i", "h", 8081);
        param.setWorker(worker);
        param.taskExecutor(new TaskExecutor() {
            @Override
            public Result execute(Checkpoint checkpoint) {
                return null;
            }
        });
        String json = param.toString();
        System.out.println(json);
        Assertions.assertEquals(json, "{\"operation\":\"TRIGGER\",\"taskId\":1,\"trackId\":2,\"jobId\":3,\"triggerTime\":4,\"worker\":{\"host\":\"h\",\"port\":8081,\"group\":\"g\",\"instanceId\":\"i\"}}");
        Assertions.assertEquals(json, Jsons.fromJson(json, ExecuteParam.class).toString());
    }

}
