package cn.ponfee.scheduler.dispatch.http;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.DiscoveryRestTemplate;

/**
 * Dispatch task based http
 *
 * @author Ponfee
 */
public class HttpTaskDispatcher extends TaskDispatcher {

    private static final String WORKER_RECEIVE_PATH = "worker/rpc/task/receive";

    private final DiscoveryRestTemplate<Worker> discoveryRestTemplate;

    public HttpTaskDispatcher(DiscoveryRestTemplate<Worker> discoveryRestTemplate,
                              TimingWheel<ExecuteParam> timingWheel) {
        super(discoveryRestTemplate.getDiscoveryServer(), timingWheel);
        this.discoveryRestTemplate = discoveryRestTemplate;
    }

    @Override
    protected boolean dispatch(ExecuteParam executeParam) throws JobException {
        try {
            Boolean res = discoveryRestTemplate.execute(executeParam.getWorker().getGroup(), WORKER_RECEIVE_PATH, Boolean.class, executeParam);
            return res != null && res;
        } catch (Exception e) {
            throw new JobException(JobCodeMsg.DISPATCH_TASK_FAILED, "Http dispatch task failed: " + executeParam, e);
        }
    }

}
