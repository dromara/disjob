/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle.impl;

import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.handle.Checkpoint;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.model.SchedTask;
import lombok.Data;
import org.apache.http.client.config.RequestConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The job handler for execute http request.
 *
 * @author Ponfee
 */
public class HttpJobHandler extends JobHandler<String> {

    private static final int DEFAULT_CONNECT_TIMEOUT = 2000;
    private static final int DEFAULT_READ_TIMEOUT = 5000;

    private final static RestTemplate REST_TEMPLATE = RestTemplateUtils.buildRestTemplate(
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_READ_TIMEOUT,
        StandardCharsets.UTF_8,
        RestTemplateUtils.buildJackson2HttpMessageConverter(null)
    );

    @Override
    public Result<String> execute(Checkpoint checkpoint) {
        final SchedTask task = task();
        HttpJobRequest req = Jsons.fromJson(task.getTaskParam(), HttpJobRequest.class);

        Assert.hasText(req.method, "Http method cannot be empty.");
        HttpMethod method = HttpMethod.valueOf(req.method.toUpperCase());
        if (RestTemplateUtils.QUERY_PARAM_METHODS.contains(method)) {
            Assert.isNull(req.body, () -> "Http method '" + req.method + "' not supported request body.");
        }
        Assert.hasText(req.url, "Http url cannot be empty.");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(req.url);
        MultiValueMap<String, String> paramsMap = RestTemplateUtils.toMultiValueMap(req.params);
        if (paramsMap != null) {
            builder.queryParams(paramsMap);
        }
        URI uri = builder.build().encode().toUri();

        MultiValueMap<String, String> headersMap = RestTemplateUtils.toMultiValueMap(req.headers);
        HttpEntity<?> httpEntity = (req.body == null && headersMap == null) ? null : new HttpEntity<>(req.body, headersMap);

        Class<String> responseType = String.class;
        RequestCallback requestCallback = REST_TEMPLATE.httpEntityCallback(httpEntity, responseType);
        ResponseExtractor<ResponseEntity<String>> responseExtractor = REST_TEMPLATE.responseEntityExtractor(responseType);

        try {
            ResponseEntity<String> res;
            if (equals(req.connectionTimeout, DEFAULT_CONNECT_TIMEOUT) && equals(req.readTimeout, DEFAULT_READ_TIMEOUT)) {
                res = REST_TEMPLATE.execute(uri, method, requestCallback, responseExtractor);
            } else {
                RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
                if (req.connectionTimeout != null) {
                    requestConfigBuilder.setConnectTimeout(req.connectionTimeout);
                }
                if (req.readTimeout != null) {
                    requestConfigBuilder.setSocketTimeout(req.readTimeout);
                }
                RestTemplateUtils.HttpContextHolder.bind(requestConfigBuilder.build());
                try {
                    res = REST_TEMPLATE.execute(uri, method, requestCallback, responseExtractor);
                } finally {
                    RestTemplateUtils.HttpContextHolder.unbind();
                }
            }

            if (res.getStatusCode().is2xxSuccessful()) {
                return Result.success(res.getBody());
            } else {
                return Result.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), "Code: " + res.getStatusCode() + ", response: " + res.getBody());
            }
        } catch (Throwable t) {
            log.error("Http request failed: " + task, t);
            return Result.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), Throwables.getRootCauseMessage(t));
        }
    }

    @Data
    public static class HttpJobRequest implements Serializable {
        private static final long serialVersionUID = 6173514568347976014L;

        private String method;
        private String url;
        private Map<String, Object> params;
        private Map<String, Object> headers;
        private String body;
        private Integer connectionTimeout; // milliseconds unit
        private Integer readTimeout;       // milliseconds unit
    }

    private static boolean equals(Integer source, int target) {
        return source == null || source == target;
    }

}
