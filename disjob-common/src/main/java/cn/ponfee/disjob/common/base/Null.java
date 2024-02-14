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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * The {@code Null} class is representing unable instance object
 *
 * @author Ponfee
 */
public final class Null implements java.io.Serializable {
    private static final long serialVersionUID = -2631792665226478680L;

    public static final Constructor<Null> BROKEN_CONSTRUCTOR;
    public static final Method BROKEN_METHOD;

    static {
        try {
            BROKEN_CONSTRUCTOR = Null.class.getDeclaredConstructor();
            BROKEN_METHOD = Null.class.getDeclaredMethod("broken");
        } catch (Exception e) {
            // cannot happen
            throw new SecurityException(e);
        }
    }

    private Null() {
        throw new Error("Null cannot create instance.");
    }

    private void broken() {
        throw new Error("Forbid invoke this method.");
    }

    private Object readResolve() {
        return null;
    }
}
