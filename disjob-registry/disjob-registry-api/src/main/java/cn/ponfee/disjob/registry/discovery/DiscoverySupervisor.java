/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.discovery;

import cn.ponfee.disjob.common.collect.ImmutableHashList;
import cn.ponfee.disjob.core.base.Supervisor;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Discovery supervisor
 *
 * @author Ponfee
 */
public final class DiscoverySupervisor implements DiscoveryServer<Supervisor> {

    /**
     * ImmutableHashList<serialize, Supervisor>
     */
    private volatile ImmutableHashList<String, Supervisor> supervisors = ImmutableHashList.empty();

    /**
     * <pre>
     * synchronized的执行过程：
     *   1）获得同步锁
     *   2）清空工作内存
     *   3）从主内存中复制数据副本到工作内存
     *   4）执行代码
     *   5）刷新数据到主内存
     *   6）释放锁
     * </pre>
     *
     * @param discoveredSupervisors the discovered supervisors
     */
    @Override
    public synchronized void refreshServers(List<Supervisor> discoveredSupervisors) {
        this.supervisors = ImmutableHashList.of(discoveredSupervisors, Supervisor::serialize);
    }

    @Override
    public List<Supervisor> getServers(String group) {
        Assert.isNull(group, "Get discovery supervisor group must be null.");
        return supervisors.values();
    }

    @Override
    public boolean hasServers() {
        return !supervisors.isEmpty();
    }

    @Override
    public boolean isAlive(Supervisor supervisor) {
        return supervisors.contains(supervisor);
    }

}
