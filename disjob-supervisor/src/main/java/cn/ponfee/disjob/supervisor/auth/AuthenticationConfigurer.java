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
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.application.value.DisjobGroup;
import cn.ponfee.disjob.supervisor.base.SupervisorConstants;
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
        private static final String ERR_MSG = "Authenticate failed.";

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            if (!(handler instanceof HandlerMethod)) {
                return true;
            }

            SupervisorAuthentication annotation = getAnnotation((HandlerMethod) handler);
            if (annotation == null || annotation.value() == SupervisorAuthentication.Subject.ANON) {
                return true;
            }

            String group = request.getHeader(JobConstants.AUTHENTICATE_HEADER_GROUP);
            if (StringUtils.isBlank(group)) {
                throw new AuthenticationException(ERR_MSG);
            }

            SupervisorAuthentication.Subject value = annotation.value();
            if (value == SupervisorAuthentication.Subject.WORKER) {
                authenticateWorker(request, group);
            } else if (value == SupervisorAuthentication.Subject.USER) {
                authenticateUser(request, group);
            } else {
                throw new UnsupportedOperationException("Unsupported supervisor authentication subject: " + annotation.value());
            }

            return true;
        }

        private static void authenticateWorker(HttpServletRequest request, String group) {
            String workerToken = SchedGroupService.mapGroup(group).getWorkerToken();
            if (StringUtils.isBlank(workerToken)) {
                return;
            }

            String token = request.getHeader(JobConstants.AUTHENTICATE_HEADER_TOKEN);
            if (!workerToken.equals(token)) {
                throw new AuthenticationException(ERR_MSG);
            }
        }

        private static void authenticateUser(HttpServletRequest request, String group) {
            String user = request.getHeader(JobConstants.AUTHENTICATE_HEADER_USER);
            DisjobGroup disjobGroup = SchedGroupService.mapGroup(group);
            if (!disjobGroup.isDeveloper(user)) {
                throw new AuthenticationException(ERR_MSG);
            }

            String userToken = SchedGroupService.mapGroup(group).getUserToken();
            if (StringUtils.isBlank(userToken)) {
                // user token must configured
                throw new AuthenticationException(ERR_MSG);
            }

            String token = request.getHeader(JobConstants.AUTHENTICATE_HEADER_TOKEN);
            if (!userToken.equals(token)) {
                throw new AuthenticationException(ERR_MSG);
            }

            request.setAttribute(SupervisorConstants.REQUEST_ATTRIBUTE_KEY_DISJOB_USER, disjobGroup);
        }

        private static SupervisorAuthentication getAnnotation(HandlerMethod hm) {
            SupervisorAuthentication a = hm.getMethodAnnotation(SupervisorAuthentication.class);
            return a != null ? a : hm.getBeanType().getAnnotation(SupervisorAuthentication.class);
        }
    }

}
