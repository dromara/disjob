/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.http;

import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Http task receiver service
 *
 * @author Ponfee
 */
@Hidden
@RequestMapping(WorkerRpcService.PREFIX_PATH)
interface HttpTaskReceiverService {

    /**
     * Receive task http method
     *
     * @param param the task
     * @return {@code true} if received successfully
     */
    @PostMapping("/task/receive")
    boolean receive(ExecuteTaskParam param);

}
