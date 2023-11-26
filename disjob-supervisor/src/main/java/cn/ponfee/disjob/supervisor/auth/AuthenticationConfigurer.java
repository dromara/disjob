/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.auth;

import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import cn.ponfee.disjob.supervisor.service.SchedGroupManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
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
public class AuthenticationConfigurer implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthenticationInterceptor()).order(Ordered.HIGHEST_PRECEDENCE);
    }

    private static class AuthenticationInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            if (!(handler instanceof HandlerMethod)) {
                return true;
            }

            Class<?> beanType = ((HandlerMethod) handler).getBeanType();
            SupervisorAuthentication annotation = beanType.getAnnotation(SupervisorAuthentication.class);
            if (annotation == null) {
                return true;
            }

            String group = request.getHeader(JobConstants.AUTHENTICATE_HEADER_GROUP);
            if (StringUtils.isBlank(group)) {
                throw new AuthenticationException("Authenticate failed.");
            }

            switch (annotation.value()) {
                case WORKER:
                    authenticateWorker(request, group);
                    break;
                case USER:
                    authenticateUser(request, group);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported supervisor authentication subject: " + annotation.value());
            }

            return true;
        }

        private static void authenticateWorker(HttpServletRequest request, String group) {
            String workerToken = SchedGroupManager.get(group).getWorkerToken();
            if (StringUtils.isBlank(workerToken)) {
                return;
            }

            String token = request.getHeader(JobConstants.AUTHENTICATE_HEADER_TOKEN);
            if (!workerToken.equals(token)) {
                throw new AuthenticationException("Authenticate failed.");
            }
        }

        private static void authenticateUser(HttpServletRequest request, String group) {
            String userToken = SchedGroupManager.get(group).getUserToken();
            if (StringUtils.isBlank(userToken)) {
                return;
            }

            String token = request.getHeader(JobConstants.AUTHENTICATE_HEADER_TOKEN);
            if (!userToken.equals(token)) {
                throw new AuthenticationException("Authenticate failed.");
            }
        }
    }

}
