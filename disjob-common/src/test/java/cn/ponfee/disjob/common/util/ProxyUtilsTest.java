/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.concurrent.AbstractHeartbeatThread;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ProxyUtils test
 *
 * @author Ponfee
 */
public class ProxyUtilsTest {

    @Test
    public void test() throws Exception {
        AbstractHeartbeatThread proxy = ProxyUtils.createBrokenProxy(AbstractHeartbeatThread.class, new Class[]{long.class}, new Object[]{1L});
        Assertions.assertThatThrownBy(proxy::start)
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Broken proxy cannot execute method: public synchronized void java.lang.Thread.start()");
    }

}
