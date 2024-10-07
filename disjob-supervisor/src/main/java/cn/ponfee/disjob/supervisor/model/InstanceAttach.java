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

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.common.dag.DAGNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Sched instance attach data structure
 *
 * @author Ponfee
 */
@Getter
@Setter
public class InstanceAttach extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -7365475674760089839L;

    /**
     * Current node name
     */
    private String curNode;

    public DAGNode parseCurNode() {
        if (StringUtils.isBlank(curNode)) {
            return null;
        }
        return DAGNode.fromString(curNode);
    }

    public static InstanceAttach of(String curNode) {
        InstanceAttach attach = new InstanceAttach();
        attach.setCurNode(curNode);
        return attach;
    }

}
