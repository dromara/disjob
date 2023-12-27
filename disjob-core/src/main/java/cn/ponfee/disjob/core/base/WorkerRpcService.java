/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.param.worker.GetMetricsParam;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
import cn.ponfee.disjob.core.param.worker.ModifyMaximumPoolSizeParam;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Worker rpc service, provides for supervisor communication.
 *
 * @author Ponfee
 */
@Hidden
@RequestMapping(WorkerRpcService.PREFIX_PATH)
public interface WorkerRpcService {

    String PREFIX_PATH = "worker/rpc/";

    @PostMapping("job/verify")
    void verify(JobHandlerParam param) throws JobException;

    @PostMapping("job/split")
    List<SplitTask> split(JobHandlerParam param) throws JobException;

    @GetMapping("metrics")
    WorkerMetrics metrics(GetMetricsParam param);

    @PostMapping("maximum_pool_size/modify")
    void modifyMaximumPoolSize(ModifyMaximumPoolSizeParam param);

}
