/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
