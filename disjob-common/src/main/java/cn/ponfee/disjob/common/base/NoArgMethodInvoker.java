/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.util.ClassUtils;
import org.apache.commons.lang3.StringUtils;

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
    public NoArgMethodInvoker(String... methodNames) {
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
