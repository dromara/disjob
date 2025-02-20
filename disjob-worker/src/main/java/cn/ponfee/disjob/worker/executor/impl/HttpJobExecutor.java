/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.worker.executor.impl;

import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.worker.executor.ExecutionResult;
import cn.ponfee.disjob.worker.executor.ExecutionTask;
import cn.ponfee.disjob.worker.executor.JobExecutor;
import cn.ponfee.disjob.worker.executor.Savepoint;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;

/**
 * The job executor for execute http request.
 * <p>
 *
 * <pre>job_param example: {@code
 *  {
 *    "method":"GET",
 *    "url":"https://www.baidu.com"
 *  }
 * }</pre>
 *
 * @author Ponfee
 */
public class HttpJobExecutor extends JobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(HttpJobExecutor.class);

    private static final int DEFAULT_CONNECT_TIMEOUT = 2000;
    private static final int DEFAULT_READ_TIMEOUT = 5000;

    private static final RestTemplate REST_TEMPLATE = RestTemplateUtils.create(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, null);

    @Override
    public ExecutionResult execute(ExecutionTask task, Savepoint savepoint) {
        HttpJobRequest req = Jsons.fromJson(task.getTaskParam(), HttpJobRequest.class);

        Assert.hasText(req.method, "Http method cannot be empty.");
        HttpMethod method = HttpMethod.valueOf(req.method.toUpperCase());
        if (RestTemplateUtils.QUERY_PARAM_METHODS.contains(method)) {
            Assert.isNull(req.body, () -> "Http method '" + req.method + "' not supported request body.");
        }
        Assert.hasText(req.url, "Http url cannot be empty.");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(req.url);
        MultiValueMap<String, String> paramsMap = RestTemplateUtils.convertToMultiValueMap(req.params);
        if (paramsMap != null) {
            builder.queryParams(paramsMap);
        }
        URI uri = builder.build().encode().toUri();

        MultiValueMap<String, String> headersMap = RestTemplateUtils.convertToMultiValueMap(req.headers);
        HttpEntity<?> requestEntity = (req.body == null && headersMap == null) ? null : new HttpEntity<>(req.body, headersMap);

        Class<String> responseType = String.class;
        RequestCallback requestCallback = REST_TEMPLATE.httpEntityCallback(requestEntity, responseType);
        ResponseExtractor<ResponseEntity<String>> responseExtractor = REST_TEMPLATE.responseEntityExtractor(responseType);

        RequestConfig requestConfig = null;
        if (!(equals(req.connectionTimeout, DEFAULT_CONNECT_TIMEOUT) && equals(req.readTimeout, DEFAULT_READ_TIMEOUT))) {
            RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
            if (req.connectionTimeout != null) {
                requestConfigBuilder.setConnectTimeout(req.connectionTimeout);
            }
            if (req.readTimeout != null) {
                requestConfigBuilder.setSocketTimeout(req.readTimeout);
            }
            requestConfig = requestConfigBuilder.build();
        }
        try {
            ResponseEntity<String> responseEntity = RestTemplateUtils.invoke(
                requestConfig, REST_TEMPLATE, uri, method, requestCallback, responseExtractor);
            if (responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful()) {
                return ExecutionResult.success(responseEntity.getBody());
            } else {
                HttpStatus status = null;
                String body = null;
                if (responseEntity != null) {
                    status = responseEntity.getStatusCode();
                    body = responseEntity.getBody();
                }
                return ExecutionResult.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), status + ": " + body);
            }
        } catch (Throwable t) {
            LOG.error("Http request error: " + task, t);
            return ExecutionResult.failure(JobCodeMsg.JOB_EXECUTE_ERROR.getCode(), Throwables.getRootCauseMessage(t));
        }
    }

    @Setter
    @Getter
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
