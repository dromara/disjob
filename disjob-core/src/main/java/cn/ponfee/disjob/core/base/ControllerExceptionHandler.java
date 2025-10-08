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

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.exception.BaseException;
import cn.ponfee.disjob.common.exception.BaseRuntimeException;
import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import com.google.common.collect.ImmutableList;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Controller exception handler
 * <p>annotations与basePackages同时使用：只要匹配其一就会被处理
 *
 * @author Ponfee
 */
@ConditionalOnMissingClass("cn.ponfee.disjob.core.base.ControllerExceptionHandler") // 禁止以扫描包的方式创建Bean
@RestControllerAdvice(basePackages = {"cn.ponfee.disjob.supervisor", "cn.ponfee.disjob.worker"})
class ControllerExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    private static final List<Class<? extends Exception>> BIZ_EXCEPTIONS = ImmutableList.of(
        IllegalArgumentException.class,
        IllegalStateException.class,
        UnsupportedOperationException.class,
        BaseException.class,
        BaseRuntimeException.class
    );

    @ExceptionHandler(Throwable.class)
    public void execute(HandlerMethod handlerMethod, HttpServletResponse response, Throwable t) throws IOException {
        String errorMsg = ExceptionUtils.getRootCauseMessage(t);
        if (BIZ_EXCEPTIONS.stream().anyMatch(e -> e.isInstance(t))) {
            LOG.error("Handle biz exception {}: {}", t.getClass().getName(), errorMsg);
        } else {
            LOG.error("Handle server exception", t);
        }

        response.setCharacterEncoding(JobConstants.UTF_8);
        PrintWriter out = response.getWriter();
        if (isMethodReturnResultType(handlerMethod)) {
            response.setContentType(JobConstants.APPLICATION_JSON_UTF8);
            errorMsg = Result.failure(JobCodeMsg.SERVER_ERROR.getCode(), errorMsg).toJson();
        } else {
            response.setContentType(JobConstants.TEXT_PLAIN_UTF8);
            response.setStatus(obtainHttpStatus(t).value());
        }
        out.write(errorMsg);
        out.flush();
    }

    // ------------------------------------------------------------------private methods

    private HttpStatus obtainHttpStatus(Throwable t) {
        if (t instanceof AuthenticationException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (t instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static boolean isMethodReturnResultType(HandlerMethod handlerMethod) {
        if (handlerMethod == null ||
            handlerMethod.getBeanType() == BasicErrorController.class) {
            return false;
        }
        return Result.class.isAssignableFrom(handlerMethod.getMethod().getReturnType());
    }

}
