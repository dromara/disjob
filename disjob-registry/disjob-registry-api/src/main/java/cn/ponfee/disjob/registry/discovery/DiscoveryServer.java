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

package cn.ponfee.disjob.registry.discovery;

import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.ServerRole;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * Discovery server.
 *
 * @author Ponfee
 */
public interface DiscoveryServer<S extends Server> {

    void refreshServers(List<S> servers);

    List<S> getServers(String group);

    boolean hasServers();

    boolean isAlive(S server);

    /**
     * Returns sorted ImmutableList
     *
     * @param servers the server list
     * @return sorted ImmutableList
     */
    default ImmutableList<S> toSortedImmutableList(List<S> servers) {
        if (CollectionUtils.isEmpty(servers)) {
            return ImmutableList.of();
        }
        return servers.stream().sorted().collect(ImmutableList.toImmutableList());
    }

    @SuppressWarnings("unchecked")
    static <S extends Server> DiscoveryServer<S> of(ServerRole discoveryRole) {
        switch (discoveryRole) {
            case WORKER:
                return (DiscoveryServer<S>) new DiscoveryWorker();
            case SUPERVISOR:
                return (DiscoveryServer<S>) new DiscoverySupervisor();
            default:
                throw new UnsupportedOperationException("Unsupported discovery server '" + discoveryRole + "'");
        }
    }

}
