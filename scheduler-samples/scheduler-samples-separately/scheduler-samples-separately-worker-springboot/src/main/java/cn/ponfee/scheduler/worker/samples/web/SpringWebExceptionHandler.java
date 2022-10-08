package cn.ponfee.scheduler.worker.samples.web;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * Global Spring web mvc Exception handler
 *
 * @author Ponfee
 */
@ControllerAdvice
public class SpringWebExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SpringWebExceptionHandler.class);
    private static final String APPLICATION_JSON_UTF8_VALUE = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";

    @ExceptionHandler(Exception.class)
    public void execute(HttpServletResponse response, Exception e) throws Exception {
        response.setContentType(APPLICATION_JSON_UTF8_VALUE);
        PrintWriter out = response.getWriter();
        Result<String> result = Result.failure(JobCodeMsg.SERVER_ERROR, Throwables.getRootCauseStackTrace(e));
        LOG.error(e.getMessage(), e);
        out.write(Jsons.toJson(result));
        out.flush();
    }

}
