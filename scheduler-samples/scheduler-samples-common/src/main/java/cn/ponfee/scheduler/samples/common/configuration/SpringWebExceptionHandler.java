/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.common.configuration;

import cn.ponfee.scheduler.common.exception.BaseCheckedException;
import cn.ponfee.scheduler.common.exception.BaseUncheckedException;
import cn.ponfee.scheduler.common.exception.Throwables;
import cn.ponfee.scheduler.common.model.Result;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
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
import java.io.PrintWriter;
import java.util.List;

/**
 * Global spring web mvc exception handler
 *
 * @author Ponfee
 */
@ControllerAdvice(/*assignableTypes = cn.ponfee.scheduler.common.spring.RpcController.class*/)
public class SpringWebExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SpringWebExceptionHandler.class);

    private static final String APPLICATION_JSON_VALUE_UTF8 = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";
    private static final String TEXT_PLAIN_VALUE_UTF8 = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8";

    private static final List<Class<? extends Exception>> BAD_REQUEST_EXCEPTIONS = ImmutableList.of(
        IllegalArgumentException.class,
        IllegalStateException.class,
        UnsupportedOperationException.class,
        BaseCheckedException.class,
        BaseUncheckedException.class
    );

    @ExceptionHandler(Exception.class)
    public void execute(HandlerMethod handlerMethod, HttpServletResponse response, Exception e) throws Exception {
        boolean isBadRequest = BAD_REQUEST_EXCEPTIONS.stream().anyMatch(t -> t.isInstance(e));
        String errorMsg = Throwables.getRootCauseMessage(e);
        if (!isBadRequest || StringUtils.isEmpty(errorMsg)) {
            LOG.error("Handle server exception", e);
        } else {
            LOG.error("Handle biz exception: " + errorMsg);
        }

        PrintWriter out = response.getWriter();
        if (!isMethodReturnResultType(handlerMethod)) {
            response.setContentType(TEXT_PLAIN_VALUE_UTF8);
            response.setStatus((isBadRequest ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR).value());
            out.write(errorMsg);
        } else {
            response.setContentType(APPLICATION_JSON_VALUE_UTF8);
            out.write(Result.failure(JobCodeMsg.SERVER_ERROR.getCode(), errorMsg).toJson());
        }
        out.flush();
    }

    private static boolean isMethodReturnResultType(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return false;
        }
        if (handlerMethod.getBeanType() == BasicErrorController.class) {
            return false;
        }
        return Result.class.isAssignableFrom(handlerMethod.getMethod().getReturnType());
    }

}
