package com.ruoyi.web.exception;

import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.base.JobConstants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.html.EscapeUtil;
import com.ruoyi.common.utils.security.PermissionUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 全局异常处理器
 *
 * @author ruoyi
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 权限校验异常（ajax请求返回json，redirect请求跳转页面）
     */
    @ExceptionHandler(AuthorizationException.class)
    public Object handleAuthorizationException(AuthorizationException e, HttpServletRequest request) {
        log.error("请求地址'{}'，权限校验失败'{}'", request.getRequestURI(), e.getMessage());
        if (ServletUtils.isAjaxRequest(request)) {
            return AjaxResult.error(PermissionUtils.getMsg(e.getMessage()));
        } else {
            return new ModelAndView("error/unauth");
        }
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public Object handleServiceException(ServiceException e, HttpServletRequest request) {
        log.error("业务异常", e);
        if (ServletUtils.isAjaxRequest(request)) {
            return AjaxResult.error(e.getMessage());
        } else {
            return new ModelAndView("error/service", "errorMessage", e.getMessage());
        }
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request) {
        log.error("请求路径中缺少必需的路径变量：path={}，error={}", request.getRequestURI(), e.getMessage());
        return AjaxResult.error(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                                HttpServletRequest request) {
        log.error("请求参数类型不匹配：path={}，error={}", request.getRequestURI(), e.getMessage());
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value)) {
            value = EscapeUtil.clean(value);
        }
        return AjaxResult.error(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), e.getRequiredType(), value));
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e) {
        return AjaxResult.error(e.getAllErrors().get(0).getDefaultMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Throwable.class)
    public void handleException(HandlerMethod handlerMethod,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Throwable t) throws IOException {
        log.error("系统异常", t);

        String errorMsg = Throwables.getRootCauseMessage(t);
        response.setCharacterEncoding(JobConstants.UTF_8);
        PrintWriter out = response.getWriter();
        if (isResponseJson(handlerMethod, request)) {
            response.setContentType(JobConstants.APPLICATION_JSON_UTF8);
            Object result;
            if (Result.class.isAssignableFrom(handlerMethod.getMethod().getReturnType())) {
                result = Result.failure(JobCodeMsg.SERVER_ERROR.getCode(), errorMsg);
            } else {
                result = AjaxResult.error(errorMsg);
            }
            errorMsg = Jsons.toJson(result);
        } else {
            response.setContentType(JobConstants.TEXT_PLAIN_UTF8);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        out.write(errorMsg);
        out.flush();
    }

    // -----------------------------------------------------------------------------private methods

    private static boolean isResponseJson(HandlerMethod handlerMethod, HttpServletRequest request) {
        if (handlerMethod == null ||
            handlerMethod.getBeanType() == BasicErrorController.class ||
            AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RpcController.class) != null) {
            return false;
        }

        return ServletUtils.isAjaxRequest(request);
    }

}
