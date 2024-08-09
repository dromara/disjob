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

package cn.ponfee.disjob.supervisor.auth;

import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.auth.SupervisorAuthentication.Subject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Supervisor authentication configurer
 *
 * @author Ponfee
 */
@Configuration
public class AuthenticationConfigurer implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthenticationInterceptor()).order(Ordered.HIGHEST_PRECEDENCE);
    }

    private static class AuthenticationInterceptor implements HandlerInterceptor {
        private static final String ERR_MSG = "Authenticate failed.";

        @SuppressWarnings({"NullableProblems"})
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            if (!(handler instanceof HandlerMethod)) {
                return true;
            }

            SupervisorAuthentication annotation = SpringUtils.getAnnotation(SupervisorAuthentication.class, (HandlerMethod) handler);
            if (annotation == null || annotation.value() == Subject.ANON) {
                return true;
            }

            String group = requestGroup();
            if (StringUtils.isBlank(group)) {
                throw new AuthenticationException(ERR_MSG);
            }

            Subject value = annotation.value();
            if (value == Subject.WORKER) {
                authenticateWorker(group);
            } else if (value == Subject.USER) {
                authenticateUser(group);
            } else {
                throw new UnsupportedOperationException("Unsupported supervisor authentication subject: " + value);
            }

            return true;
        }

        private static void authenticateWorker(String group) {
            if (!SchedGroupService.verifyWorkerAuthenticationToken(requestToken(), group)) {
                throw new AuthenticationException(ERR_MSG);
            }
        }

        private static void authenticateUser(String group) {
            if (!SchedGroupService.isDeveloper(group, requestUser())) {
                throw new AuthenticationException(ERR_MSG);
            }

            if (!SchedGroupService.verifyUserAuthenticationToken(requestToken(), group)) {
                throw new AuthenticationException(ERR_MSG);
            }
        }
    }

    public static String requestUser() {
        return getRequest().getHeader(JobConstants.AUTHENTICATE_HEADER_USER);
    }

    public static String requestGroup() {
        return getRequest().getHeader(JobConstants.AUTHENTICATE_HEADER_GROUP);
    }

    private static String requestToken() {
        return getRequest().getHeader(JobConstants.AUTHENTICATE_HEADER_TOKEN);
    }

    @SuppressWarnings({"null", "ConstantConditions"})
    private static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

}
