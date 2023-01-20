/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.handle.impl;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.spring.RestTemplateUtils;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.JobHandler;
import lombok.Data;
import org.apache.http.client.config.RequestConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
        DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT
    );
    static {
        REST_TEMPLATE.setMessageConverters(Arrays.asList(
            new ByteArrayHttpMessageConverter(),
            new StringHttpMessageConverter(StandardCharsets.UTF_8),
            new ResourceHttpMessageConverter(),
            new SourceHttpMessageConverter<>(),
            new FormHttpMessageConverter(),
            RestTemplateUtils.buildJackson2HttpMessageConverter()
        ));
    }

    @Override
    public Result<String> execute(Checkpoint checkpoint) {
        HttpRequest req = Jsons.fromJson(task.getTaskParam(), HttpRequest.class);

        Assert.hasText(req.method, "Http method cannot be empty.");
        HttpMethod method = HttpMethod.valueOf(req.method.toUpperCase());
        if (RestTemplateUtils.QUERY_PARAMS.contains(method)) {
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
        } catch (Exception e) {
            log.error("Http job execute failed: " + task, e);
            return Result.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), Throwables.getRootCauseMessage(e));
        }
    }

    @Data
    public static class HttpRequest implements Serializable {
        private static final long serialVersionUID = 6173514568347976014L;

        private String method;
        private String url;
        private Map<String, Object> params;
        private Map<String, Object> headers;
        private String body;
        private Integer connectionTimeout; // unit milliseconds
        private Integer readTimeout;       // unit milliseconds
    }

    private static boolean equals(Integer source, int target) {
        return source == null || source == target;
    }

}
