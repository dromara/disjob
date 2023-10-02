/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

/**
 * Initialize resources
 *
 * @author Ponfee
 */
@FunctionalInterface
public interface Initializable {

    NoArgMethodInvoker INITIATOR = new NoArgMethodInvoker("open", "init", "initialize");

    /**
     * Initialize resources
     */
    void init();

    /**
     * Initialize target resources
     *
     * @param target the target object
     */
    static void init(Object target) {
        if (target == null) {
            return;
        }

        if (target instanceof Initializable) {
            ((Initializable) target).init();
        } else {
            INITIATOR.invoke(target);
        }
    }

}
