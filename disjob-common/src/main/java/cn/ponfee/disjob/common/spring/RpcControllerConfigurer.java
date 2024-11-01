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

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.Jsons;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * RpcController configurer
 *
 * @author Ponfee
 */
public class RpcControllerConfigurer implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new RpcControllerArgumentResolver());
    }

    /**
     * Configure rpc invocation for spring web {@code org.springframework.stereotype.Controller} methods.
     * <p>Can defined multiple object arguments for {@code org.springframework.web.bind.annotation.RequestMapping} method.
     */
    private static class RpcControllerArgumentResolver implements HandlerMethodArgumentResolver {
        static final Set<String> QUERY_PARAM_METHODS = Collects.convert(RestTemplateUtils.QUERY_PARAM_METHODS, HttpMethod::name);
        static final String CACHE_ATTRIBUTE_KEY = "$disjob$RpcController#method(args)";

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            if (parameter.getMethod() == null) {
                return false;
            }
            Class<?> declaringClass = parameter.getDeclaringClass();
            if (declaringClass.isAnnotationPresent(RpcController.class)) {
                return true;
            }
            Class<?> containingClass = parameter.getContainingClass();
            if (containingClass.equals(declaringClass)) {
                return false;
            }
            // jdk proxied: Proxy.isProxyClass(containingClass)
            return ClassUtils.findAnnotatedClass(declaringClass, containingClass, RpcController.class) != null;
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) throws IOException {
            Method method = Objects.requireNonNull(parameter.getMethod());
            HttpServletRequest request = Objects.requireNonNull(webRequest.getNativeRequest(HttpServletRequest.class));
            int parameterIndex = parameter.getParameterIndex();
            Object[] arguments;
            if (parameterIndex == 0) {
                arguments = parseMethodParameters(method, request);
                if (method.getParameterCount() > 1) {
                    request.setAttribute(CACHE_ATTRIBUTE_KEY, arguments);
                }
            } else {
                arguments = (Object[]) request.getAttribute(CACHE_ATTRIBUTE_KEY);
            }

            return Collects.get(arguments, parameterIndex);
        }

        private static Object[] parseMethodParameters(Method method, HttpServletRequest request) throws IOException {
            if (QUERY_PARAM_METHODS.contains(request.getMethod())) {
                return RpcControllerUtils.parseQueryParameters(method, request.getParameterMap());
            }

            try (ServletInputStream inputStream = request.getInputStream()) {
                String body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                if (StringUtils.isEmpty(body)) {
                    return RpcControllerUtils.parseQueryParameters(method, request.getParameterMap());
                } else {
                    return Jsons.parseMethodArgs(body, method);
                }
            }
        }
    }

}
