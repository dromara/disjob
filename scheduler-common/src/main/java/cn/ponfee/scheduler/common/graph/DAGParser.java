/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.graph;

import cn.ponfee.scheduler.common.base.Symbol.Char;
import cn.ponfee.scheduler.common.base.Symbol.Str;
import cn.ponfee.scheduler.common.base.tuple.Tuple2;
import cn.ponfee.scheduler.common.base.tuple.Tuple3;
import cn.ponfee.scheduler.common.tree.BaseNode;
import cn.ponfee.scheduler.common.tree.PlainNode;
import cn.ponfee.scheduler.common.tree.TreeNode;
import cn.ponfee.scheduler.common.tree.TreeNodeBuilder;
import cn.ponfee.scheduler.common.util.Collects;
import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parse DAG expression to graph
 *
 * <pre>
 * new DAGParser("(A->((B->C->D),(A->F))->(G,H,X)->J);(A->Y)").parse();
 *
 * (A->((B->C->D),(A->F))->(G,H,X)->J)
 *   <0:0:HEAD -> 1:1:A>
 *   <1:1:A -> 1:1:B>
 *   <1:1:A -> 1:2:A>
 *   <1:1:B -> 1:1:C>
 *   <1:1:C -> 1:1:D>
 *   <1:2:A -> 1:1:F>
 *   <1:1:D -> 1:1:G>
 *   <1:1:D -> 1:1:H>
 *   <1:1:D -> 1:1:X>
 *   <1:1:F -> 1:1:G>
 *   <1:1:F -> 1:1:H>
 *   <1:1:F -> 1:1:X>
 *   <1:1:G -> 1:1:J>
 *   <1:1:H -> 1:1:J>
 *   <1:1:X -> 1:1:J>
 *   <1:1:J -> 0:0:TAIL>
 *
 * (A->Y)
 *   <0:0:HEAD -> 2:3:A>
 *   <2:3:A -> 2:1:Y>
 *   <2:1:Y -> 0:0:TAIL>
 * </pre>
 *
 * @author Ponfee
 */
public class DAGParser {

    private static final String SEP_FLOW = ";";
    private static final String SEP_STAGE = "->";
    private static final String SEP_UNION = ",";
    static final String SEP_NAMING = ":";
    private static final List<String> SEP_LIST = ImmutableList.of(SEP_STAGE, SEP_UNION);

    /**
     * Map<name, List<Tuple2<flow-id, stage-id, name-id>>>
     */
    private final Map<String, List<Tuple3<Integer, Integer, Integer>>> incrementer = new HashMap<>();
    private final String expression;

    public DAGParser(String expression) {
        Assert.hasText(expression, "Expression cannot be blank.");
        Assert.isTrue(checkParenthesis(expression), () -> "Expression have invalid parenthesis: " + expression);
        Assert.isTrue(!expression.contains(SEP_NAMING), () -> "Expression cannot contains ':' symbol: " + expression);
        this.expression = expression;
    }

    public Graph<GraphNodeId> parse() {
        ImmutableGraph.Builder<GraphNodeId> graphBuilder = GraphBuilder.directed().allowsSelfLoops(false).immutable();

        List<String> flows = Stream.of(expression.split(SEP_FLOW)).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        Assert.notEmpty(flows, () -> "Invalid split with ';' expression: " + expression);

        for (int i = 0; i < flows.size(); i++) {
            String expr = flows.get(i);
            buildGraph(i + 1, 1, Collections.singletonList(expr), graphBuilder, GraphNodeId.HEAD, GraphNodeId.TAIL);
        }
        ImmutableGraph<GraphNodeId> graph = graphBuilder.build();
        Assert.state(!Graphs.hasCycle(graph), "Expression task flow has cycle: " + expression);
        return graph;
    }

    private void buildGraph(int flowId, int stageId, List<String> expressions,
                            ImmutableGraph.Builder<GraphNodeId> graphBuilder, GraphNodeId prev, GraphNodeId next) {
        // 划分第一个stage
        Tuple2<List<String>, List<String>> stage = divideFirstStage(expressions);
        if (stage == null) {
            return;
        }

        List<String> first = stage.a, remains = stage.b;
        for (int i = 0, n = first.size() - 1; i <= n; i++) {
            List<String> list = resolve(first.get(i));
            Assert.notEmpty(list, () -> "Invalid expression: " + String.join("", expressions));
            if (list.size() == 1) {
                String task = list.get(0);
                int nameId = increment(flowId, stageId, task);
                GraphNodeId node = GraphNodeId.of(flowId, nameId, task);
                graphBuilder.putEdge(prev, node);
                if (remains == null) {
                    graphBuilder.putEdge(node, next);
                } else {
                    buildGraph(flowId, stageId, remains, graphBuilder, node, next);
                }
            } else {
                buildGraph(flowId, stageId + 1, join(list, remains), graphBuilder, prev, next);
            }
        }
    }

    private int increment(int flowId, int stageId, String task) {
        List<Tuple3<Integer, Integer, Integer>> list = incrementer.get(task);
        Tuple3<Integer, Integer, Integer> tuple;
        if (CollectionUtils.isEmpty(list)) {
            tuple = Tuple3.of(flowId, stageId, 1);
            incrementer.put(task, Collects.newLinkedList(tuple));
        } else {
            List<Tuple3<Integer, Integer, Integer>> find = list.stream()
                .filter(each -> flowId == each.a && stageId == each.b)
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(find)) {
                // increment
                tuple = Tuple3.of(flowId, stageId, list.size() + 1);
                list.add(tuple);
            } else if (find.size() == 1) {
                tuple = find.get(0);
            } else {
                // cannot happen
                throw new IllegalStateException("Increment name id failed: " + expression);
            }
        }
        return tuple.c;
    }

    private static Tuple2<List<String>, List<String>> divideFirstStage(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }

        Assert.isTrue(!SEP_LIST.contains(Collects.getFirst(list)), () -> "Invalid expression: " + String.join("", list));
        Assert.isTrue(!SEP_LIST.contains(Collects.getLast(list)), () -> "Invalid expression: " + String.join("", list));

        if (list.size() == 1) {
            return Tuple2.of(list, null);
        }

        List<String> head = new ArrayList<>();
        for (int i = 0, n = list.size() - 1; i <= n; ) {
            head.add(list.get(i++));
            if (i > n) {
                return Tuple2.of(head, null);
            }
            String str = list.get(i++);
            if (SEP_STAGE.equals(str)) {
                return Tuple2.of(head, list.subList(i, list.size()));
            } else if (SEP_UNION.equals(str)) {
                // skip ","
            } else {
                throw new IllegalArgumentException("Invalid expression: " + String.join("", list));
            }
        }
        return Tuple2.of(head, null);
    }

    private static List<String> join(List<String> head, List<String> tail) {
        if (CollectionUtils.isEmpty(tail)) {
            return head;
        }

        List<String> result = new ArrayList<>(head.size() + 1 + tail.size());
        result.addAll(head);
        result.add(SEP_STAGE);
        result.addAll(tail);
        return result;
    }

    static List<String> resolve(String text) {
        String expression = text.trim();
        if (!expression.startsWith(Str.OPEN) || !expression.endsWith(Str.CLOSE)) {
            return resolve(Str.OPEN + expression + Str.CLOSE);
        }

        List<Tuple2<Integer, Integer>> partitions = partition(expression);

        // 取被"()"包裹的最外层表达式
        List<Tuple2<Integer, Integer>> outermost = partitions.stream().filter(e -> e.b == 1).collect(Collectors.toList());
        if (outermost.size() == 2) {
            // 首尾括号，如：(A,B -> C,D)
            Assert.isTrue(outermost.get(0).a == 0 && outermost.get(1).a == expression.length() - 1, () -> "Invalid expression: " + text);
        } else if (outermost.size() > 2) {
            // 多组括号情况，需要在外层再包层括号，如：
            //   1）“(A,B) -> (C,D)”    =>    “((A,B) -> (C,D))”
            //   2）“(B->C->D),(A->F)”  =>    “((B->C->D),(A->F))”
            return resolve(Str.OPEN + expression + Str.CLOSE);
        } else {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }

        TreeNode<TreeNodeId, Object> root = buildTree(partitions);
        List<Integer> list = new ArrayList<>();
        list.add(root.getNid().openPos);
        root.forEachChild(child -> {
            list.add(child.getNid().openPos);
            list.add(child.getNid().closePos);
        });
        list.add(root.getNid().closePos);
        return split(expression, list);
    }

    static TreeNode<TreeNodeId, Object> buildTree(List<Tuple2<Integer, Integer>> partitions) {
        List<BaseNode<TreeNodeId, Object>> nodes = new ArrayList<>();
        buildTree(partitions, TreeNodeId.ROOT_ID, 1, 0, nodes);

        // create a dummy root node
        TreeNode<TreeNodeId, Object> dummyRoot = TreeNodeBuilder.newBuilder(TreeNodeId.ROOT_ID).build();

        // mount nodes
        dummyRoot.mount(nodes);

        // gets the actual root
        Assert.state(dummyRoot.getChildrenCount() == 1, "Build tree root node has not a single child.");
        return dummyRoot.getChildren().get(0);
    }

    private static void buildTree(List<Tuple2<Integer, Integer>> partitions,
                                  TreeNodeId pid, int level, int start,
                                  List<BaseNode<TreeNodeId, Object>> nodes) {
        int open = -1;
        for (int i = start; i < partitions.size(); i++) {
            if (partitions.get(i).b < level) {
                return;
            }
            if (partitions.get(i).b == level) {
                if (open == -1) {
                    open = i;
                } else {
                    // find "()" position
                    TreeNodeId nid = TreeNodeId.of(partitions.get(open).a, partitions.get(i).a);
                    nodes.add(new PlainNode<>(nid, pid, null));
                    buildTree(partitions, nid, level + 1, open + 1, nodes);
                    open = -1;
                }
            }
        }
    }

    /**
     * Partition expression by "()"
     *
     * @param text the expression
     * @return positions of "()"
     */
    static List<Tuple2<Integer, Integer>> partition(String text) {
        int d = 0;
        // Tuple2<position, level>
        List<Tuple2<Integer, Integer>> list = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == Char.OPEN) {
                ++d;
                list.add(Tuple2.of(i, d));
            } else if (text.charAt(i) == Char.CLOSE) {
                list.add(Tuple2.of(i, d));
                --d;
            }
        }
        Assert.isTrue(list.size() % 2 == 0, () -> "Expression not pair with '()': " + text);
        return list;
    }

    private static List<String> split(String expression, List<Integer> partitions) {
        List<String> result = new ArrayList<>(partitions.size());
        for (int i = 0, n = partitions.size() - 1; i < n; i++) {
            int start = partitions.get(i) + 1;
            int end = partitions.get(i + 1);
            String str = expression.substring(start, end).trim();
            if (str.isEmpty()) {
                result.add(str);
            } else if (!expression.contains(SEP_STAGE) && partitions.size() == 2) {
                // For example: (A)、(A,B)
                result.addAll(split(str, SEP_UNION));
            } else if (i == 0) {
                result.addAll(split(str, SEP_STAGE));
            } else if (i < n - 1) {
                result.add(str);
            } else {
                result.addAll(split(str, SEP_STAGE));
            }
        }

        return result.stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList());
    }

    static List<String> split(String str, String separator) {
        if (!str.contains(separator)) {
            return Collections.singletonList(str);
        }

        List<String> result = new ArrayList<>();
        int a = 0, b = 0;
        for (; (b = str.indexOf(separator, b)) != -1; a = b) {
            if (a != b) {
                result.add(str.substring(a, b).trim());
            }
            result.add(str.substring(b, b = b + separator.length()));
        }
        if (a < str.length()) {
            result.add(str.substring(a).trim());
        }
        return result;
    }

    static boolean checkParenthesis(String str) {
        int openCount = 0;
        for (int i = 0, n = str.length(); i < n; i++) {
            char c = str.charAt(i);
            if (c == Char.OPEN) {
                openCount++;
            } else if (c == Char.CLOSE) {
                openCount--;
            }
            // Currently, don't have enough open parentheses to match close parentheses-> Invalid
            // For example: ())(
            if (openCount < 0) {
                return false;
            }
        }

        // Fully match open parentheses with close parentheses
        return openCount == 0;
    }

    static class TreeNodeId implements Serializable, Comparable<TreeNodeId> {
        private static final long serialVersionUID = -468548698179536500L;

        private static final TreeNodeId ROOT_ID = TreeNodeId.of(-1, -1);

        /**
         * position of "("
         */
        private final int openPos;

        /**
         * position of ")"
         */
        private final int closePos;

        private TreeNodeId(int openPos, int closePos) {
            this.openPos = openPos;
            this.closePos = closePos;
        }

        private static TreeNodeId of(int openPos, int closePos) {
            return new TreeNodeId(openPos, closePos);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TreeNodeId)) {
                return false;
            }
            TreeNodeId other = (TreeNodeId) obj;
            return this.openPos == other.openPos
                && this.closePos == other.closePos;
        }

        @Override
        public int hashCode() {
            return Objects.hash(openPos, closePos);
        }

        @Override
        public int compareTo(TreeNodeId other) {
            int n = this.openPos - other.openPos;
            return n != 0 ? n : (this.closePos - other.closePos);
        }

        @Override
        public String toString() {
            return "(" + openPos + "," + closePos + ")";
        }
    }

}
