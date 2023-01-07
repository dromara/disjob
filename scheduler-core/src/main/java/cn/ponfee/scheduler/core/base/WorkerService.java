/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.core.exception.JobException;
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
    boolean verify(String jobHandler, String jobParam);

    @PostMapping(PREFIX_PATH + "split")
    List<SplitTask> split(String jobHandler, String jobParam) throws JobException;

}
