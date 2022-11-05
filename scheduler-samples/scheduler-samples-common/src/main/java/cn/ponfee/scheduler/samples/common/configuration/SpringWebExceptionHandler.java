package cn.ponfee.scheduler.samples.common.configuration;

import cn.ponfee.scheduler.common.base.exception.BaseCheckedException;
import cn.ponfee.scheduler.common.base.exception.BaseUncheckedException;
import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

/**
 * Global spring web mvc exception handler
 *
 * @author Ponfee
 */
@ControllerAdvice // @RestControllerAdvice(annotations = BindControllerAdvice.class)
public class SpringWebExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SpringWebExceptionHandler.class);
    private static final String APPLICATION_JSON_UTF8_VALUE = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";

    private static final List<Class<? extends Exception>> BAD_REQUEST_EXCEPTIONS = ImmutableList.of(
        IllegalArgumentException.class,
        IllegalStateException.class,
        BaseCheckedException.class,
        BaseUncheckedException.class
    );

    @ExceptionHandler(Exception.class)
    public void execute(HttpServletResponse response, Exception e) throws Exception {
        LOG.error("Global exception aspect advice", e);

        Result<String> result = Result.failure(JobCodeMsg.SERVER_ERROR, Throwables.getRootCauseStackTrace(e));
        HttpStatus status = BAD_REQUEST_EXCEPTIONS.stream().anyMatch(t -> t.isInstance(e))
                          ? HttpStatus.BAD_REQUEST
                          : HttpStatus.INTERNAL_SERVER_ERROR;
        response.setStatus(status.value());
        response.setContentType(APPLICATION_JSON_UTF8_VALUE);
        PrintWriter out = response.getWriter();
        out.write(Jsons.toJson(result));
        out.flush();
    }

}
