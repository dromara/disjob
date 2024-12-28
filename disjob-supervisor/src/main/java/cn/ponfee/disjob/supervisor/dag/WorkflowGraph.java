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

package cn.ponfee.disjob.supervisor.dag;

import cn.ponfee.disjob.common.dag.DAGEdge;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.supervisor.model.SchedWorkflow;
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
 * <pre>
 * DAG graph for workflow
 *
 * source -> pre_node
 * target -> cur_node
 * </pre>
 *
 * @author Ponfee
 */
public class WorkflowGraph {

    private final Map<DAGEdge, SchedWorkflow> map;
    private final Graph<DAGNode> graph;

    private WorkflowGraph(List<SchedWorkflow> workflows) {
        this.map = buildMap(workflows);
        this.graph = buildGraph(workflows);
    }

    public static WorkflowGraph of(List<SchedWorkflow> workflows) {
        return new WorkflowGraph(workflows);
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
        return map.get(new DAGEdge(source, target));
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
            builder.putEdge(edge.parsePreNode(), edge.parseCurNode());
        }
        return builder.build();
    }

}
