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
import cn.ponfee.disjob.common.util.ObjectUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Localized method argument utils
 *
 * @author Ponfee
 */
public final class LocalizedMethodArgumentUtils {

    public static Object[] parseQueryParams(Method method, Map<String, String[]> parameterMap) {
        int parameterCount = method.getParameterCount();
        Object[] arguments = new Object[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            String argName = getQueryParamName(i);
            String[] array = parameterMap.get(argName);
            Assert.isTrue(array == null || array.length <= 1, () -> "Param cannot be multiple value: " + argName + "=" + Jsons.toJson(array));
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

    public static MultiValueMap<String, String> buildQueryParams(Object... arguments) {
        if (ArrayUtils.isEmpty(arguments)) {
            return null;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(arguments.length << 1);
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] != null) {
                params.add(getQueryParamName(i), Jsons.toJson(arguments[i]));
            }
        }
        return params;
    }

    public static String getQueryParamName(int argIndex) {
        return "args[" + argIndex + "]";
    }

}
