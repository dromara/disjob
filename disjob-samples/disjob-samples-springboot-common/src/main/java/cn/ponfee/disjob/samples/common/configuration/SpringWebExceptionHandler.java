/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.common.configuration;

import cn.ponfee.disjob.common.exception.BaseException;
import cn.ponfee.disjob.common.exception.BaseRuntimeException;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Global spring web mvc exception handler
 *
 * @author Ponfee
 */
@ControllerAdvice(/*assignableTypes = cn.ponfee.disjob.common.spring.RpcController.class*/)
public class SpringWebExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SpringWebExceptionHandler.class);

    private static final String APPLICATION_JSON_VALUE_UTF8 = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";
    private static final String TEXT_PLAIN_VALUE_UTF8 = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8";

    private static final List<Class<? extends Exception>> BAD_REQUEST_EXCEPTIONS = ImmutableList.of(
        IllegalArgumentException.class,
        IllegalStateException.class,
        UnsupportedOperationException.class,
        BaseException.class,
        BaseRuntimeException.class
    );

    @ExceptionHandler(Throwable.class)
    public void execute(HandlerMethod handlerMethod, HttpServletResponse response, Throwable t) throws IOException {
        boolean isBadRequest = BAD_REQUEST_EXCEPTIONS.stream().anyMatch(e -> e.isInstance(t));
        String errorMsg = Throwables.getRootCauseMessage(t);
        if (!isBadRequest || StringUtils.isEmpty(errorMsg)) {
            LOG.error("Handle server exception", t);
        } else {
            LOG.error("Handle biz exception: {}", errorMsg);
        }

        PrintWriter out = response.getWriter();
        if (isMethodReturnResultType(handlerMethod)) {
            response.setContentType(APPLICATION_JSON_VALUE_UTF8);
            out.write(Result.failure(JobCodeMsg.SERVER_ERROR.getCode(), errorMsg).toString());
        } else {
            response.setContentType(TEXT_PLAIN_VALUE_UTF8);
            response.setStatus(obtainHttpStatus(t, isBadRequest).value());
            out.write(errorMsg);
        }
        out.flush();
    }

    private HttpStatus obtainHttpStatus(Throwable t, boolean isBadRequest) {
        if (t instanceof AuthenticationException) {
            return HttpStatus.UNAUTHORIZED;
        }
        return isBadRequest ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static boolean isMethodReturnResultType(HandlerMethod handlerMethod) {
        if (handlerMethod == null ||
            handlerMethod.getBeanType() == BasicErrorController.class) {
            return false;
        }
        return Result.class.isAssignableFrom(handlerMethod.getMethod().getReturnType());
    }

}
