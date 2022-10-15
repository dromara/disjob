package cn.ponfee.scheduler.common.spring;

import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
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
import java.util.List;
import java.util.Map;

/**
 * Localized method parameter for spring web {@code org.springframework.stereotype.Controller} methods.
 * <p>Can defined multiple object arguments for {@code org.springframework.web.bind.annotation.RequestMapping} method.
 *
 * @author Ponfee
 */
public class LocalizedMethodArgumentResolver implements HandlerMethodArgumentResolver {

    //private final WeakHashMap<NativeWebRequest, Map<String, Object>> resolvedCache = new WeakHashMap<>();

    private static final List<String> QUERY_PARAMS = ImmutableList.of(
        "GET", "DELETE", "HEAD", "OPTIONS"
    );

    private static final String STORE_KEY_PREFIX = "LOCALIZED_METHOD_ARGUMENTS:";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!(parameter.getExecutable() instanceof Method)) {
            return false;
        }

        return AnnotationUtils.findAnnotation(parameter.getMethod(), LocalizedMethodArguments.class) != null
            || AnnotationUtils.findAnnotation(parameter.getDeclaringClass(), LocalizedMethodArguments.class) != null;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws IOException {
        Method method = parameter.getMethod();
        HttpServletRequest httpServletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        int parameterIndex = parameter.getParameterIndex();
        Object[] arguments;
        if (parameterIndex == 0) {
            arguments = resolveMethodParameters(method, httpServletRequest);
            if (method.getParameterCount() > 1) {
                // method.toGenericString()
                httpServletRequest.setAttribute(STORE_KEY_PREFIX + method, arguments);
            }
        } else {
            arguments = (Object[]) httpServletRequest.getAttribute(STORE_KEY_PREFIX + method);
        }

        return Collects.get(arguments, parameterIndex);
    }

    private Object[] resolveMethodParameters(Method method, HttpServletRequest request) throws IOException {
        if (QUERY_PARAMS.contains(request.getMethod())) {
            return resolveQueryString(method, request.getParameterMap());
        } else {
            String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
            if (StringUtils.isEmpty(body)) {
                return resolveQueryString(method, request.getParameterMap());
            } else {
                return resolveRequestBody(method, body);
            }
        }
    }

    private Object[] resolveQueryString(Method method, Map<String, String[]> parameterMap) {
        int parameterCount = method.getParameterCount();
        Object[] arguments = new Object[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            String argName = "arg[" + i + "]";
            String[] array = parameterMap.get(argName);
            Assert.isTrue(
                array == null || array.length <= 1,
                "Argument cannot be multiple value, name: " + argName + ", value: " + Jsons.toJson(array)
            );
            String argValue = Collects.get(array, 0);
            Type argType = method.getGenericParameterTypes()[i];
            if (argValue == null) {
                // if basic type then set default value
                arguments[i] = (argType instanceof Class<?>) ? ObjectUtils.cast(null, (Class<?>) argType) : null;
            } else {
                arguments[i] = Jsons.fromJson(argValue, argType);
            }
        }
        return arguments;
    }

    private Object[] resolveRequestBody(Method method, String body) {
        return JSON.parseArray(body, method.getGenericParameterTypes()).toArray();

        /*
        JsonNode jsonNode = Jsons.NORMAL.objectMapper().readTree(body);
        Assert.isTrue(jsonNode.isArray(), "Request parameter must be json array.");
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Assert.isTrue(arrayNode.size() == genericParameterTypes.length, "Request parameter size inconsistent.");
        int length = genericParameterTypes.length;
        Object[] arguments = new Object[length];
        for (int i = 0; i < length; i++) {
            arguments[i] = Jsons.NORMAL.parse(arrayNode.get(i).toString(), genericParameterTypes[i]);
        }
        return arguments;
        */
    }

}
