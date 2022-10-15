package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.core.handle.SplitTask;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * Worker provides api, for the supervisor communication.
 *
 * @author Ponfee
 */
public interface WorkerService {

    String PREFIX_PATH = "worker/rpc/";

    @PostMapping(PREFIX_PATH + "verify")
    Result<Boolean> verify(String jobHandler, String jobParam);

    @PostMapping(PREFIX_PATH + "split")
    Result<List<SplitTask>> split(String jobHandler, String jobParam);

}
