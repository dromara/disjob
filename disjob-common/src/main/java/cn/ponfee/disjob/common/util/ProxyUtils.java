/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Proxy utility class.
 *
 * @author Ponfee
 */
public class ProxyUtils {

    public static <T> T create(Class<T> interfaceType, InvocationHandler invocationHandler) {
        Class<?>[] interfaces = {interfaceType};
        return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), interfaces, invocationHandler);
    }

}
