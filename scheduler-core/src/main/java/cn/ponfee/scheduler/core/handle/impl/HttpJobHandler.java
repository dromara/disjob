/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.handle.impl;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The job handler for execute http request.
 *
 * @author Ponfee
 */
public class HttpJobHandler extends JobHandler<String> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpJobHandler.class);

    @Override
    public Result<String> execute(Checkpoint checkpoint) {
        throw new RuntimeException("unimplemented.");
    }
}
