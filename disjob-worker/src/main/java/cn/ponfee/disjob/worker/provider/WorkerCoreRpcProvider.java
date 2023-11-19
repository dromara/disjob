/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.provider;

import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.WorkerCoreRpcService;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.param.worker.AuthenticationParam;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Worker core rpc service provider.
 *
 * @author Ponfee
 */
public class WorkerCoreRpcProvider implements WorkerCoreRpcService, RpcController {

    private final String supervisorToken;

    public WorkerCoreRpcProvider(String supervisorToken) {
        this.supervisorToken = supervisorToken;
    }

    @Override
    public void verify(JobHandlerParam param) throws JobException {
        authenticate(param);
        JobHandlerUtils.verify(param);
    }

    @Override
    public List<SplitTask> split(JobHandlerParam param) throws JobException {
        authenticate(param);
        return JobHandlerUtils.split(param);
    }

    private void authenticate(AuthenticationParam param) {
        if (StringUtils.isBlank(supervisorToken)) {
            return;
        }

        if (!supervisorToken.equals(param.getSupervisorToken())) {
            throw new AuthenticationException("Authentication failed.");
        }
    }

}
