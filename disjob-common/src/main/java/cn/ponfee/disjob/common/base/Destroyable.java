/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

/**
 * Destroy resources
 *
 * @author Ponfee
 */
@FunctionalInterface
public interface Destroyable {

    NoArgMethodInvoker DEFAULT = new NoArgMethodInvoker("destroy", "close", "release");

    /**
     * Destroy resources
     */
    void destroy();

    /**
     * Destroy target resources
     *
     * @param target the target object
     */
    static void destroy(Object target) throws Exception {
        if (target == null) {
            return;
        }

        if (target instanceof AutoCloseable) {
            ((AutoCloseable) target).close();
        } else if (target instanceof Destroyable) {
            Destroyable destroyable = (Destroyable) target;
            if (!destroyable.isDestroyed()) {
                ((Destroyable) target).destroy();
            }
        } else {
            DEFAULT.invoke(target);
        }
    }

    /**
     * Returns is whether destroyed.
     *
     * @return {@code true} destroyed
     */
    default boolean isDestroyed() {
        return false;
    }

}
