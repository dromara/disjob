package cn.ponfee.scheduler.common.spring;

import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import com.alibaba.fastjson.JSON;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Localized method parameter for spring mvc {@code org.springframework.stereotype.Controller} methods.
 * <p>Can defined multiple object arguments for {@code org.springframework.web.bind.annotation.RequestMapping} method.
 *
 * @author Ponfee
 */
public class LocalizedMethodArgumentResolver implements HandlerMethodArgumentResolver {

    //private final WeakHashMap<NativeWebRequest, Map<String, Object>> resolvedCache = new WeakHashMap<>();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!(parameter.getExecutable() instanceof Method)) {
            return false;
        }

        Method method = parameter.getMethod();
        return method.getDeclaringClass().isAnnotationPresent(LocalizedMethodArguments.class)
            || method.isAnnotationPresent(LocalizedMethodArguments.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws IOException {
        Method method = parameter.getMethod();
        HttpServletRequest httpServletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        int parameterIndex = parameter.getParameterIndex();
        // method.toGenericString()
        String storeKey = "LOCALIZED_METHOD_ARGUMENTS:" + method.toString();
        Object[] arguments;
        if (parameterIndex == 0) {
            arguments = resolveMethodParameters(method, httpServletRequest);
            httpServletRequest.setAttribute(storeKey, arguments);
        } else {
            arguments = (Object[]) httpServletRequest.getAttribute(storeKey);
        }

        return Collects.get(arguments, parameterIndex);
    }

    private Object[] resolveMethodParameters(Method method, HttpServletRequest request) throws IOException {
        switch (request.getMethod()) {
            case "GET":
            case "DELETE":
                return resolveQueryString(method, request.getParameterMap());
            default:
                String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
                if (StringUtils.isEmpty(body)) {
                    return resolveQueryString(method, request.getParameterMap());
                } else {
                    return resolveRequestBody(method, body);
                }
        }
    }

    private Object[] resolveQueryString(Method method, Map<String, String[]> parameterMap) {
        Object[] arguments = new Object[method.getParameterCount()];
        for (int i = 0; i < method.getParameterCount(); i++) {
            String argName = "arg[" + i + "]";
            String[] array = parameterMap.get(argName);
            Assert.isTrue(
                array == null || array.length <= 1,
                "Argument cannot be multiple value, name: " + argName + ", value: " + JSON.toJSONString(array)
            );
            String argValue = Collects.get(array, 0);
            Type argType = method.getGenericParameterTypes()[i];
            if (argValue == null) {
                arguments[i] = (argType instanceof Class<?>) ? ObjectUtils.cast(null, (Class<?>) argType) : null;
            } else {
                arguments[i] = JSON.parseObject(argValue, argType);
            }
        }
        return arguments;
    }

    private Object[] resolveRequestBody(Method method, String body) {
        return JSON.parseArray(body, method.getGenericParameterTypes()).toArray();
    }

}
