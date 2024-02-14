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
