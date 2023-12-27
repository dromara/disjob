/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.util.Jsons;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Localized method parameter for spring web {@code org.springframework.stereotype.Controller} methods.
 * <p>Can defined multiple object arguments for {@code org.springframework.web.bind.annotation.RequestMapping} method.
 *
 * @author Ponfee
 */
public class LocalizedMethodArgumentResolver implements HandlerMethodArgumentResolver {

    //private final WeakHashMap<NativeWebRequest, Map<String, Object>> resolvedCache = new WeakHashMap<>();

    private static final Set<String> QUERY_PARAM_METHODS = RestTemplateUtils.QUERY_PARAM_METHODS.stream().map(HttpMethod::name).collect(Collectors.toSet());

    private static final String CACHE_ATTRIBUTE_KEY = "LOCALIZED_METHOD_ARGUMENTS";

    private static final Class<? extends Annotation> MARKED_ANNOTATION_TYPE = LocalizedMethodArguments.class;

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

    public static Object[] parseMethodParameters(Method method, HttpServletRequest request) throws IOException {
        if (QUERY_PARAM_METHODS.contains(request.getMethod())) {
            return LocalizedMethodArgumentUtils.parseQueryParams(method, request.getParameterMap());
        }

        try (ServletInputStream inputStream = request.getInputStream()) {
            String body = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if (StringUtils.isEmpty(body)) {
                return LocalizedMethodArgumentUtils.parseQueryParams(method, request.getParameterMap());
            } else {
                return Jsons.parseMethodArgs(body, method);
            }
        }
    }

    // --------------------------------------------------------------private methods

    private static boolean isAnnotationPresent(Method method, Class<? extends Annotation> annotationType) {
        return AnnotationUtils.findAnnotation(method, annotationType) != null;
    }

    private static boolean isAnnotationPresent(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return AnnotationUtils.findAnnotation(clazz, annotationType) != null;
    }

}
