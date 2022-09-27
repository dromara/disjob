package cn.ponfee.scheduler.supervisor.test.job.param;

import cn.ponfee.scheduler.common.base.model.Result;
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
    public void testSerial() {
        ExecuteParam param = new ExecuteParam(Operations.TRIGGER, 1, 2, 3, 4);
        Worker worker = new Worker("g", "i", "h", 8081);
        param.setWorker(worker);
        param.taskExecutor(new TaskExecutor() {
            @Override
            public Result execute(Checkpoint checkpoint) throws Exception {
                return null;
            }
        });
        String serial = param.toString();
        System.out.println(serial);
        Assertions.assertEquals(serial, "{\"jobId\":3,\"trackId\":2,\"operation\":\"TRIGGER\",\"taskId\":1,\"triggerTime\":4,\"worker\":{\"group\":\"g\",\"host\":\"h\",\"instanceId\":\"i\",\"port\":8081}}");
        Assertions.assertEquals(serial, JSON.parseObject(serial, ExecuteParam.class).toString());
    }

}
