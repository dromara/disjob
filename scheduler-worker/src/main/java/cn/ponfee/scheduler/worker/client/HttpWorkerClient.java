package cn.ponfee.scheduler.worker.client;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.registry.Discovery;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Worker client based http request communication to supervisor.
 *
 * @author Ponfee
 */
public class HttpWorkerClient implements WorkerClient {

    private final static Logger LOG = LoggerFactory.getLogger(HttpWorkerClient.class);
    private final static int MAX_RETRY_TIMES = 3;

    private static final ParameterizedTypeReference<Result<SchedJob>> RESULT_SCHED_JOB = new ParameterizedTypeReference<Result<SchedJob>>() {};
    private static final ParameterizedTypeReference<Result<SchedTask>> RESULT_SCHED_TASK = new ParameterizedTypeReference<Result<SchedTask>>() {};
    private static final ParameterizedTypeReference<Result<Void>> RESULT_VOID = new ParameterizedTypeReference<Result<Void>>() {};
    private static final ParameterizedTypeReference<Result<Boolean>> RESULT_BOOLEAN = new ParameterizedTypeReference<Result<Boolean>>() {};

    private static final String PATH_PREFIX = "rpc/";

    private final Discovery<Supervisor> discoverySupervisor;
    private final RestTemplate restTemplate;

    public HttpWorkerClient(Discovery<Supervisor> discoverySupervisor) {
        MappingJackson2HttpMessageConverter httpMessageConverter = new MappingJackson2HttpMessageConverter();
        httpMessageConverter.setObjectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL));
        httpMessageConverter.setSupportedMediaTypes(Collects.concat(httpMessageConverter.getSupportedMediaTypes(), MediaType.TEXT_PLAIN));

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setMessageConverters(Arrays.asList(
            new ByteArrayHttpMessageConverter(),
            new StringHttpMessageConverter(StandardCharsets.UTF_8),
            new ResourceHttpMessageConverter(),
            new SourceHttpMessageConverter<>(),
            new FormHttpMessageConverter(),
            httpMessageConverter
        ));

        this.discoverySupervisor = discoverySupervisor;
        this.restTemplate = restTemplate;
    }

    public HttpWorkerClient(Discovery<Supervisor> discoverySupervisor,
                            RestTemplate restTemplate) {
        this.discoverySupervisor = discoverySupervisor;
        this.restTemplate = restTemplate;
    }

    @Override
    public SchedJob getJob(long jobId) throws Exception {
        return execute(PATH_PREFIX + "job/get", RESULT_SCHED_JOB, jobId);
    }

    @Override
    public SchedTask getTask(long taskId) throws Exception {
        return execute(PATH_PREFIX + "task/get", RESULT_SCHED_TASK, taskId);
    }

    @Override
    public boolean startTask(ExecuteParam param) throws Exception {
        return execute(PATH_PREFIX + "task/start", RESULT_BOOLEAN, param);
    }

    @Override
    public boolean checkpoint(long taskId, String executeSnapshot) throws Exception {
        return execute(PATH_PREFIX + "task/checkpoint", RESULT_BOOLEAN, taskId, executeSnapshot);
    }

    @Override
    public boolean updateTaskErrorMsg(long taskId, String errorMsg) throws Exception {
        return execute(PATH_PREFIX + "task_error_msg/update", RESULT_BOOLEAN, taskId, errorMsg);
    }

    @Override
    public boolean pauseTrack(long trackId) throws Exception {
        return execute(PATH_PREFIX + "track/pause", RESULT_BOOLEAN, trackId);
    }

    @Override
    public boolean cancelTrack(long trackId, Operations operations) throws Exception {
        return execute(PATH_PREFIX + "track/cancel", RESULT_BOOLEAN, trackId, operations);
    }

    @Override
    public boolean pauseExecutingTask(ExecuteParam param, String errorMsg) throws Exception {
        return execute(PATH_PREFIX + "executing_task/pause", RESULT_BOOLEAN, param, errorMsg);
    }

    @Override
    public boolean cancelExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) throws Exception {
        return execute(PATH_PREFIX + "executing_task/cancel", RESULT_BOOLEAN, param, toState, errorMsg);
    }

    @Override
    public boolean terminateExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) throws Exception {
        return execute(PATH_PREFIX + "executing_task/terminate", RESULT_BOOLEAN, param, toState, errorMsg);
    }

    private <T> T execute(String path, ParameterizedTypeReference<Result<T>> returnType, Object... arguments) throws Exception {
        List<Supervisor> supervisors = discoverySupervisor.getServers(null);
        if (CollectionUtils.isEmpty(supervisors)) {
            throw new IllegalStateException("Http worker client, Not found available supervisor.");
        }

        int supervisorNumber = supervisors.size();
        int start = ThreadLocalRandom.current().nextInt(supervisorNumber);
        for (int i = 0, n = Math.min(supervisorNumber, MAX_RETRY_TIMES) + 1; i < n; i++) {
            Supervisor supervisor = supervisors.get((start + i) % supervisorNumber);
            String url = String.format("http://%s:%d/%s", supervisor.getHost(), supervisor.getPort(), path);
            try {
                Result<T> result = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(arguments), returnType).getBody();
                if (result.isSuccess()) {
                    return result.getData();
                } else {
                    throw new IllegalStateException("Invoked supervisor failed, url: " + url + ", req: " + JSON.toJSONString(arguments) + ", res: " + result);
                }
            } catch (ResourceAccessException | RestClientResponseException e) {
                // round-robin retry
                LOG.error("Invoked supervisor error, url: " + url + ", req: " + JSON.toJSONString(arguments), e);
                Thread.sleep(300 * IntMath.pow(i + 1, 2));
            }
        }

        throw new IllegalStateException("Not found available supervisor: " + path + ", " + JSON.toJSONString(arguments));
    }

}
