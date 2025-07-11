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

package cn.ponfee.disjob.registry;

import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.discovery.ServerDiscovery;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Worker registry and discovery supervisor.
 *
 * @author Ponfee
 */
public interface WorkerRegistry extends Registry<Worker>, Discovery<Supervisor> {

    /**
     * Returns the registered workers of group
     *
     * @param group the group
     * @return registered worker list
     */
    default List<Worker> getRegisteredWorkers(String group) {
        List<Worker> workers = getRegisteredServers();
        return CollectionUtils.isEmpty(workers) ? Collections.emptyList() :
            ServerDiscovery.toSortedImmutableList(workers.stream().filter(e -> e.equalsGroup(group)));
    }

}
