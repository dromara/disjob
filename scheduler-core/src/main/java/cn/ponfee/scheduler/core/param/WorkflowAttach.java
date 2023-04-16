/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.param;

import cn.ponfee.scheduler.common.graph.GraphNodeId;
import lombok.AllArgsConstructor;
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
     * previous node id
     */
    private String preNodeId;

    /**
     * current node id
     */
    private String curNodeId;

    public WorkflowAttach(String preNodeId, String curNodeId) {
        this.preNodeId = preNodeId;
        this.curNodeId = curNodeId;
    }

    public static WorkflowAttach of(GraphNodeId preNodeId, GraphNodeId curNodeId) {
        return new WorkflowAttach(preNodeId.toString(), curNodeId.toString());
    }

}
