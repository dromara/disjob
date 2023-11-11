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
import cn.ponfee.disjob.core.param.JobHandlerParam;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Worker core rpc service, provides for supervisor communication.
 *
 * @author Ponfee
 */
@Hidden
@RequestMapping(WorkerCoreRpcService.PREFIX_PATH)
public interface WorkerCoreRpcService {

    String PREFIX_PATH = "worker/core/rpc/";

    @PostMapping("job/verify")
    void verify(JobHandlerParam param) throws JobException;

    @PostMapping("job/split")
    List<SplitTask> split(JobHandlerParam param) throws JobException;

}
