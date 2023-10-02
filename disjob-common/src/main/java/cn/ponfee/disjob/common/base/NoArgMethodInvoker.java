/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.util.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;

/**
 * Specifies multiple non-arg method names, find the first and invoke it
 *
 * @author Ponfee
 */
public final class NoArgMethodInvoker {

    private final String[] methodNames;

    /**
     * Constructs instance
     *
     * @param methodNames the no-arg method list
     */
    public NoArgMethodInvoker(@Nonnull String... methodNames) {
        if (methodNames == null || methodNames.length == 0) {
            throw new IllegalArgumentException("Must be specified least once no-arg method name.");
        }
        this.methodNames = methodNames;
    }

    /**
     * Invoke no-arg method
     *
     * @param target the target object
     */
    public void invoke(Object target) {
        if (target == null) {
            return;
        }
        if (target instanceof Class<?>) {
            throw new IllegalArgumentException("Invalid target object " + target);
        }

        Arrays.stream(methodNames)
            .filter(StringUtils::isNotBlank)
            .map(name -> ClassUtils.getMethod(target, name))
            .filter(Objects::nonNull)
            .findAny()
            .ifPresent(method -> ClassUtils.invoke(target, method));
    }

}
