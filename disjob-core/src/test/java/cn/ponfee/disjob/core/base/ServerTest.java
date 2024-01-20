/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Server test
 *
 * @author Ponfee
 */
public class ServerTest {

    @Test
    public void testSameServer() {
        Worker worker1 = new Worker("group-a", "workerId1", "localhost", 80);
        Worker worker2 = new Worker("group-a", "workerId2", "localhost", 80);
        Worker worker3 = new Worker("group-b", "workerId2", "localhost", 80);
        assertThat(worker1.sameWorker(worker2)).isTrue();
        assertThat(worker1.sameServer(worker2)).isTrue();
        assertThat(worker3.sameWorker(worker2)).isFalse();
    }

}
