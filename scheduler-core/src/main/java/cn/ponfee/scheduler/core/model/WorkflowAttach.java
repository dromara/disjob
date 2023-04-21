/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.model;

import cn.ponfee.scheduler.common.graph.DAGNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Workflow attach data structure
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkflowAttach implements Serializable {

    private static final long serialVersionUID = -7365475674760089839L;

    /**
     * current node id
     */
    private String curNode;

    public WorkflowAttach(String curNode) {
        this.curNode = curNode;
    }

    public static WorkflowAttach of(DAGNode curNode) {
        return new WorkflowAttach(curNode.toString());
    }

}