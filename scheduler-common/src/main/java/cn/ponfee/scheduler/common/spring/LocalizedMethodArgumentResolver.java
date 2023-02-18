/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.spring;

import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Localized method parameter for spring web {@code org.springframework.stereotype.Controller} methods.
 * <p>Can defined multiple object arguments for {@code org.springframework.web.bind.annotation.RequestMapping} method.
 *
 * @author Ponfee
 */
public class LocalizedMethodArgumentResolver implements HandlerMethodArgumentResolver {

    //private final WeakHashMap<NativeWebRequest, Map<String, Object>> resolvedCache = new WeakHashMap<>();
    private static final Set<String> QUERY_PARAMS = ImmutableSet.of(GET.name(), DELETE.name(), HEAD.name(), OPTIONS.name());
    private static final String CACHE_ATTRIBUTE_KEY = "LOCALIZED_METHOD_ARGUMENTS";
    private static final Class<? extends Annotation> MARKED_ANNOTATION_TYPE = LocalizedMethodArguments.class;

    private final ObjectMapper objectMapper;

    public LocalizedMethodArgumentResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!(parameter.getExecutable() instanceof Method)) {
            return false;
        }

        return isAnnotationPresent(parameter.getMethod(), MARKED_ANNOTATION_TYPE)
            || isAnnotationPresent(parameter.getDeclaringClass(), MARKED_ANNOTATION_TYPE);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws IOException {
        Method method = Objects.requireNonNull(parameter.getMethod());
        HttpServletRequest httpServletRequest = Objects.requireNonNull(webRequest.getNativeRequest(HttpServletRequest.class));
        int parameterIndex = parameter.getParameterIndex();
        Object[] arguments;
        if (parameterIndex == 0) {
            arguments = parseMethodParameters(method, httpServletRequest);
            if (method.getParameterCount() > 1) {
                // CACHE_KEY_PREFIX + method.toString()
                httpServletRequest.setAttribute(CACHE_ATTRIBUTE_KEY, arguments);
            }
        } else {
            arguments = (Object[]) httpServletRequest.getAttribute(CACHE_ATTRIBUTE_KEY);
        }

        return Collects.get(arguments, parameterIndex);
    }

    private Object[] parseMethodParameters(Method method, HttpServletRequest request) throws IOException {
        if (QUERY_PARAMS.contains(request.getMethod())) {
            return parseQueryString(method, request.getParameterMap());
        } else {
            try (ServletInputStream inputStream = request.getInputStream()) {
                String body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                if (StringUtils.isEmpty(body)) {
                    return parseQueryString(method, request.getParameterMap());
                } else {
                    return parseRequestBody(method, body);
                }
            }
        }
    }

    private Object[] parseQueryString(Method method, Map<String, String[]> parameterMap) {
        int parameterCount = method.getParameterCount();
        Object[] arguments = new Object[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            String argName = "args[" + i + "]";
            String[] array = parameterMap.get(argName);
            Assert.isTrue(
                array == null || array.length <= 1,
                () -> "Argument cannot be multiple value, name: " + argName + ", value: " + Jsons.toJson(array)
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

    private Object[] parseRequestBody(Method method, String body) throws IOException {
        // 不推荐使用，因为需要额外依赖fastjson
        //return com.alibaba.fastjson.JSON.parseArray(body, method.getGenericParameterTypes()).toArray();

        Type[] genericArgumentTypes = method.getGenericParameterTypes();
        int argumentCount = genericArgumentTypes.length;
        if (/*method.getParameterCount()*/argumentCount == 0) {
            return null;
        }

        JsonNode rootNode = objectMapper.readTree(body);
        if (rootNode.isArray()) {
            ArrayNode requestParameters = (ArrayNode) rootNode;

            // 方法只有一个参数，但请求参数长度大于1
            // ["a", "b"]     -> method(Object[] arg) -> arg=["a", "b"]
            // [["a"], ["b"]] -> method(Object[] arg) -> arg=[["a"], ["b"]]
            if (argumentCount == 1 && requestParameters.size() > 1) {
                return new Object[]{parse(rootNode, genericArgumentTypes[0])};
            }

            // 其它情况，在调用方将参数(requestParameters)用数组包一层：new Object[]{ arg-1, arg-2, ..., arg-n }
            // [["a", "b"]]   -> method(Object[] arg)                 -> arg =["a", "b"]
            // [["a"], ["b"]] -> method(Object[] arg1, Object[] arg2) -> arg1=["a"], arg2=["b"]
            // ["a", "b"]     -> method(Object[] arg1, Object[] arg2) -> arg1=["a"], arg2=["b"]  # ACCEPT_SINGLE_VALUE_AS_ARRAY作用：将字符串“a”转为数组arg1[]
            Assert.isTrue(
                argumentCount == requestParameters.size(),
                () -> "Method arguments size: " + argumentCount + ", but actual size: " + requestParameters.size()
            );

            Object[] methodArguments = new Object[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                methodArguments[i] = parse(requestParameters.get(i), genericArgumentTypes[i]);
            }
            return methodArguments;
        } else {
            Assert.isTrue(argumentCount == 1, "Single object request parameter not support multiple arguments method.");
            return new Object[]{parse(rootNode, genericArgumentTypes[0])};
        }
    }

    private Object parse(JsonNode jsonNode, Type type) throws IOException {
        return objectMapper
            .readerFor(objectMapper.getTypeFactory().constructType(type))
            .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .readValue(objectMapper.treeAsTokens(jsonNode));
    }

    private static boolean isAnnotationPresent(Method method, Class<? extends Annotation> annotationType) {
        return AnnotationUtils.findAnnotation(method, annotationType) != null;
    }

    private static boolean isAnnotationPresent(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return AnnotationUtils.findAnnotation(clazz, annotationType) != null;
    }

}
