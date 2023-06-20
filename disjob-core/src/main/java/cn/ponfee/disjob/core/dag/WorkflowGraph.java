/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.dag;

import cn.ponfee.disjob.common.dag.DAGEdge;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.core.model.SchedWorkflow;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * DAG graph for workflow
 *
 * @author Ponfee
 */
public class WorkflowGraph {

    private final Map<DAGEdge, SchedWorkflow> map;
    private final Graph<DAGNode> graph;

    public WorkflowGraph(List<SchedWorkflow> workflows) {
        this.map = buildMap(workflows);
        this.graph = buildGraph(workflows);
    }

    public Map<DAGEdge, SchedWorkflow> predecessors(DAGNode node) {
        return find(graph.predecessors(node));
    }

    public Map<DAGEdge, SchedWorkflow> successors(DAGNode node) {
        return find(graph.successors(node));
    }

    public Map<DAGEdge, SchedWorkflow> map() {
        return map;
    }

    public SchedWorkflow get(DAGNode source, DAGNode target) {
        return map.get(DAGEdge.of(source, target));
    }

    public boolean allMatch(Predicate<Map.Entry<DAGEdge, SchedWorkflow>> predicate) {
        return map.entrySet().stream().allMatch(predicate);
    }

    public boolean anyMatch(Predicate<Map.Entry<DAGEdge, SchedWorkflow>> predicate) {
        return map.entrySet().stream().anyMatch(predicate);
    }

    // --------------------------------------------------------------private methods

    private Map<DAGEdge, SchedWorkflow> find(Set<DAGNode> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return Collections.emptyMap();
        }
        return map.entrySet()
            .stream()
            .filter(e -> nodes.contains(e.getKey().getTarget()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<DAGEdge, SchedWorkflow> buildMap(List<SchedWorkflow> workflows) {
        return Collections.unmodifiableMap(
            workflows.stream().collect(Collectors.toMap(SchedWorkflow::toEdge, Function.identity()))
        );
    }

    private static Graph<DAGNode> buildGraph(List<SchedWorkflow> workflows) {
        ImmutableGraph.Builder<DAGNode> builder = GraphBuilder.directed().allowsSelfLoops(false).immutable();
        for (SchedWorkflow edge : workflows) {
            builder.putEdge(DAGNode.fromString(edge.getPreNode()), DAGNode.fromString(edge.getCurNode()));
        }
        return builder.build();
    }

}
