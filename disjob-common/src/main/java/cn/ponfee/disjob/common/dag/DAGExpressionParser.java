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

package cn.ponfee.disjob.common.dag;

import cn.ponfee.disjob.common.base.Symbol.Char;
import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.tree.BaseNode;
import cn.ponfee.disjob.common.tree.PlainNode;
import cn.ponfee.disjob.common.tree.TreeNode;
import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.Predicates;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parse DAG expression to graph
 *
 * <pre>
 * 解析：new DAGParser("A->((B->C->D),(A->F))->(G,H,X)->J; A->Y").parse();
 * 结果：
 *  "A->((B->C->D),(A->F))->(G,H,X)->J"
 *    <0:0:Start -> 1:1:A>
 *    <1:1:A -> 1:1:B>
 *    <1:1:A -> 1:2:A>
 *    <1:1:B -> 1:1:C>
 *    <1:1:C -> 1:1:D>
 *    <1:2:A -> 1:1:F>
 *    <1:1:D -> 1:1:G>
 *    <1:1:D -> 1:1:H>
 *    <1:1:D -> 1:1:X>
 *    <1:1:F -> 1:1:G>
 *    <1:1:F -> 1:1:H>
 *    <1:1:F -> 1:1:X>
 *    <1:1:G -> 1:1:J>
 *    <1:1:H -> 1:1:J>
 *    <1:1:X -> 1:1:J>
 *    <1:1:J -> 0:0:End>
 *
 *  "A->Y"
 *    <0:0:Start -> 2:3:A>
 *    <2:3:A -> 2:1:Y>
 *    <2:1:Y -> 0:0:End>
 *
 * ---------------------------------------------------
 *
 * 无法用expression来表达的场景：[A->C, A->D, B->D, B->E]
 * ┌─────────────────────────────────┐
 * │               ┌─────>C──┐       │
 * │        ┌──>A──┤         │       │
 * │        │      └──┐      │       │
 * │ Start──┤         ├──>D──┼──>End │
 * │        │      ┌──┘      │       │
 * │        └──>B──┤         │       │
 * │               └─────>E──┘       │
 * └─────────────────────────────────┘
 * 但可通过json graph来表达：
 *   [
 *     {"source": "1:1:A", "target": "1:1:C"},
 *     {"source": "1:1:A", "target": "1:1:D"},
 *     {"source": "1:1:B", "target": "1:1:D"},
 *     {"source": "1:1:B", "target": "1:1:E"}
 *   ]
 * </pre>
 *
 * @author Ponfee
 */
public class DAGExpressionParser {

    /**
     * <pre>
     * 1、(?i) 开启大小写忽略模式，但是只适用于ASCII字符
     * 2、(?u) 开启utf-8编码模式
     * 3、(?s) 单行模式，“.”匹配任意字符(包括空白字符)
     * 4、(?m) 开启多行匹配模式，“.”不匹配空白字符
     * 5、(?d) 单行模式，“.”不匹配空白字符
     *
     * Match json array: [{...}]
     * 有两种方式：
     *   1、(?s)^\s*\[\s*\{.+}\s*]\s*$
     *   2、(?m)^\s*\[\s*\{(\s*\S+\s*)+}\s*]\s*$
     * </pre>
     */
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("(?s)^\\s*\\[\\s*\\{.+}\\s*]\\s*$");

    private static final String SEP_STAGE = "->";
    private static final String SEP_UNION = Str.COMMA;
    private static final List<String> SEP_SYMBOLS = ImmutableList.of(SEP_STAGE, SEP_UNION);
    private static final List<String> ALL_SYMBOLS = ImmutableList.of(SEP_STAGE, SEP_UNION, Str.CLOSE, Str.OPEN);
    private static final char[] SINGLE_SYMBOLS = {Char.OPEN, Char.CLOSE, Char.COMMA};

    private final String expression;

    /**
     * Identity cache of expression wrapped '()'
     */
    private final Map<String, String> wrappedCache = new IdentityHashMap<>();

    /**
     * Identity cache of partition key
     */
    private final Map<PartitionIdentityKey, String> partitionCache = new HashMap<>();

    /**
     * Map<name, List<Tuple2<name, ordinal>>>
     */
    private final Map<String, List<Tuple2<String, Integer>>> incrementer = new HashMap<>();

    public static Graph<DAGNode> parse(String text) {
        return new DAGExpressionParser(text).parse();
    }

    private DAGExpressionParser(String text) {
        Assert.hasText(text, "Expression cannot be blank.");
        this.expression = text.trim();
    }

    private Graph<DAGNode> parse() {
        ImmutableGraph.Builder<DAGNode> graphBuilder = GraphBuilder.directed().allowsSelfLoops(false).immutable();

        List<DAGEdge> edges;
        if (JSON_ARRAY_PATTERN.matcher(expression).matches() && (edges = GraphEdge.fromJson(expression)) != null) {
            parseJsonGraph(graphBuilder, edges);
        } else {
            parsePlainExpr(graphBuilder);
        }

        ImmutableGraph<DAGNode> graph = graphBuilder.build();
        Assert.state(graph.nodes().size() > 2, () -> "Expression not any name: " + expression);
        Assert.state(graph.successors(DAGNode.START).stream().noneMatch(DAGNode::isEnd), () -> "Expression name cannot direct end: " + expression);
        Assert.state(graph.predecessors(DAGNode.END).stream().noneMatch(DAGNode::isStart), () -> "Expression name cannot direct start: " + expression);
        Assert.state(!Graphs.hasCycle(graph), () -> "Expression name section has cycle: " + expression);
        return graph;
    }

    /**
     * Parse graph from json array
     *
     * <pre>
     * [
     *   {"source":"1:1:A","target":"1:1:C"},
     *   {"source":"1:1:A","target":"1:1:D"},
     *   {"source":"1:1:B","target":"1:1:D"},
     *   {"source":"1:1:B","target":"1:1:E"}
     * ]
     * </pre>
     *
     * @param graphBuilder the graph builder
     * @param edges        the edges
     */
    private void parseJsonGraph(ImmutableGraph.Builder<DAGNode> graphBuilder, List<DAGEdge> edges) {
        Assert.notEmpty(edges, "Graph edges cannot be empty.");
        Set<DAGNode> allNode = new HashSet<>();
        Set<DAGNode> nonHead = new HashSet<>();
        Set<DAGNode> nonTail = new HashSet<>();
        for (DAGEdge edge : edges) {
            DAGNode source = edge.getSource();
            DAGNode target = edge.getTarget();
            Assert.isTrue(!source.isStartOrEnd(), () -> "Graph edge cannot be start or end: " + source);
            Assert.isTrue(!target.isStartOrEnd(), () -> "Graph edge cannot be start or end: " + target);

            graphBuilder.putEdge(source, target);
            allNode.add(source);
            allNode.add(target);
            nonHead.add(target);
            nonTail.add(source);
        }
        allNode.stream().filter(Predicates.not(nonHead::contains)).forEach(e -> graphBuilder.putEdge(DAGNode.START, e));
        allNode.stream().filter(Predicates.not(nonTail::contains)).forEach(e -> graphBuilder.putEdge(e, DAGNode.END));
    }

    /**
     * Parse graph from plain text
     *
     * A->((B->C->D),(A->F))->(G,H,X)->J; A->Y
     *
     * @param graphBuilder the graph builder
     */
    private void parsePlainExpr(ImmutableGraph.Builder<DAGNode> graphBuilder) {
        Assert.isTrue(checkParenthesis(expression), () -> "Invalid expression parenthesis: " + expression);
        List<String> sections = Stream.of(expression.split(";")).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
        Assert.notEmpty(sections, () -> "Invalid split with ';' expression: " + expression);

        for (int i = 0, n = sections.size(); i < n; i++) {
            String section = sections.get(i);
            Assert.isTrue(checkParenthesis(section), () -> "Invalid expression parenthesis: " + section);
            String expr = completeParenthesis(section);
            buildGraph(i + 1, Collections.singletonList(expr), graphBuilder, DAGNode.START, DAGNode.END);
        }
    }

    private void buildGraph(int section, List<String> expressions,
                            ImmutableGraph.Builder<DAGNode> graphBuilder, DAGNode prev, DAGNode next) {
        // 划分第一个stage
        Tuple2<List<String>, List<String>> tuple = divideFirstStage(expressions);
        if (tuple == null) {
            return;
        }

        List<String> first = tuple.a, remains = tuple.b;
        for (int i = 0, n = first.size() - 1; i <= n; i++) {
            List<String> list = resolve(first.get(i));
            Assert.notEmpty(list, () -> "Invalid expression: " + String.join("", expressions));
            if (list.size() == 1) {
                String name = list.get(0);
                DAGNode node = DAGNode.of(section, incrementOrdinal(name), name);
                graphBuilder.putEdge(prev, node);
                if (remains == null) {
                    graphBuilder.putEdge(node, next);
                } else {
                    buildGraph(section, remains, graphBuilder, node, next);
                }
            } else {
                buildGraph(section, concat(list, remains), graphBuilder, prev, next);
            }
        }
    }

    private List<String> resolve(String text) {
        String expr = text.trim();
        if (ALL_SYMBOLS.stream().noneMatch(expr::contains)) {
            // unnecessary resolve
            return Collections.singletonList(expr);
        }

        if (!expr.startsWith(Str.OPEN) || !expr.endsWith(Str.CLOSE)) {
            return resolve(wrappedCache.computeIfAbsent(expr, DAGExpressionParser::wrap));
        }

        List<Tuple2<Integer, Integer>> groups = group(expr);

        // 取被"()"包裹的最外层表达式
        List<Tuple2<Integer, Integer>> outermost = groups.stream().filter(e -> e.b == 1).collect(Collectors.toList());
        if (outermost.size() == 2) {
            // 首尾括号，如：(A,B -> C,D)
            Assert.isTrue(outermost.get(0).a == 0 && outermost.get(1).a == expr.length() - 1, () -> "Invalid expression: " + text);
        } else if (outermost.size() > 2) {
            // 多组括号情况，需要在外层再包层括号，如：
            //   1）“(A,B) -> (C,D)”    =>    “((A,B) -> (C,D))”
            //   2）“(B->C->D),(A->F)”  =>    “((B->C->D),(A->F))”
            return resolve(wrappedCache.computeIfAbsent(expr, DAGExpressionParser::wrap));
        } else {
            throw new IllegalArgumentException("Invalid expression: " + expr);
        }

        TreeNode<TreeNodeId, Object> root = buildTree(groups);
        List<Integer> list = new ArrayList<>(root.getChildrenCount() * 2 + 2);
        list.add(root.getNid().open);
        root.forEachChild(child -> {
            list.add(child.getNid().open);
            list.add(child.getNid().close);
        });
        list.add(root.getNid().close);
        return partition(expr, list);
    }

    private List<String> partition(String expr, List<Integer> groups) {
        List<String> result = new ArrayList<>(groups.size());
        for (int i = 0, n = groups.size() - 1; i < n; i++) {
            PartitionIdentityKey key = new PartitionIdentityKey(expr, groups.get(i) + 1, groups.get(i + 1));
            // if twice open “((”，then str is empty content
            String str = partitionCache.computeIfAbsent(key, PartitionIdentityKey::partition);
            if (StringUtils.isNotBlank(str)) {
                result.add(str);
            }
        }
        return result;
    }

    private int incrementOrdinal(String name) {
        List<Tuple2<String, Integer>> list = incrementer.computeIfAbsent(name, k -> new LinkedList<>());
        // compare object address
        Tuple2<String, Integer> tuple = list.stream().filter(e -> name == e.a).findAny().orElse(null);
        if (tuple == null) {
            // increment name ordinal
            list.add(tuple = Tuple2.of(name, list.size() + 1));
        }
        return tuple.b;
    }

    // ------------------------------------------------------------------------------------static methods

    private static Tuple2<List<String>, List<String>> divideFirstStage(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }

        Assert.isTrue(!SEP_SYMBOLS.contains(Collects.getFirst(list)), () -> "Invalid expression: " +  String.join("", list));
        Assert.isTrue(!SEP_SYMBOLS.contains(Collects.getLast(list)), () -> "Invalid expression: " + String.join("", list));

        if (list.size() == 1) {
            return Tuple2.of(list, null);
        }

        List<String> head = new ArrayList<>();
        for (int i = 0, n = list.size() - 1; i <= n; ) {
            head.add(list.get(i++));
            if (i > n) {
                return Tuple2.of(head, null);
            }
            switch (list.get(i++)) {
                case SEP_STAGE:
                    return Tuple2.of(head, list.subList(i, list.size()));
                case SEP_UNION:
                    // skip “,”
                    break;
                default:
                    throw new IllegalArgumentException("Invalid expression: " + String.join("", list));
            }
        }
        return Tuple2.of(head, null);
    }

    private static TreeNode<TreeNodeId, Object> buildTree(List<Tuple2<Integer, Integer>> groups) {
        List<BaseNode<TreeNodeId, Object>> nodes = new ArrayList<>(groups.size() + 1);
        buildTree(groups, TreeNodeId.ROOT_ID, 1, 0, nodes);

        // create a dummy root node
        TreeNode<TreeNodeId, Object> dummyRoot = TreeNode.builder(TreeNodeId.ROOT_ID).build();

        // mount nodes
        dummyRoot.mount(nodes);

        // gets the actual root
        Assert.state(dummyRoot.getChildrenCount() == 1, "Build tree root node must be has a single child.");
        return dummyRoot.getChildren().get(0);
    }

    private static void buildTree(List<Tuple2<Integer, Integer>> groups,
                                  TreeNodeId pid, int level, int start,
                                  List<BaseNode<TreeNodeId, Object>> nodes) {
        int open = -1;
        for (int i = start, n = groups.size(); i < n; i++) {
            if (groups.get(i).b < level) {
                return;
            }
            if (groups.get(i).b == level) {
                if (open == -1) {
                    open = i;
                } else {
                    // find "()" position
                    TreeNodeId nid = TreeNodeId.of(groups.get(open).a, groups.get(i).a);
                    nodes.add(new PlainNode<>(nid, pid, null));
                    buildTree(groups, nid, level + 1, open + 1, nodes);
                    open = -1;
                }
            }
        }
    }

    private static List<String> concat(List<String> left, List<String> right) {
        if (CollectionUtils.isEmpty(right)) {
            return left;
        }

        List<String> result = new ArrayList<>(left.size() + 1 + right.size());
        result.addAll(left);
        result.add(SEP_STAGE);
        result.addAll(right);
        return result;
    }

    /**
     * Checks the text is wrapped '()' is valid.
     *
     * @param text the text string
     * @return {@code true} if valid
     */
    private static boolean checkParenthesis(String text) {
        int openCount = 0;
        for (int i = 0, n = text.length(); i < n; i++) {
            char c = text.charAt(i);
            if (c == Char.OPEN) {
                openCount++;
            } else if (c == Char.CLOSE) {
                openCount--;
            }
            if (openCount < 0) {
                // For example "())("
                return false;
            }
        }
        return openCount == 0;
    }

    /**
     * Complete the text wrapped with '()'
     *
     * @param text the text string
     * @return wrapped text string
     */
    private static String completeParenthesis(String text) {
        List<String> list = new ArrayList<>();
        int mark = 0, position = 0;
        for (int len = text.length() - 1; position <= len; ) {
            char ch = text.charAt(position++);
            Assert.isTrue(ch != '>', () -> "Invalid '" + ch + "': " + text);
            if (ArrayUtils.contains(SINGLE_SYMBOLS, ch)) {
                list.add(text.substring(mark, position - 1).trim());
                list.add(Character.toString(ch));
                mark = position;
            } else if (ch == '-') {
                // position not equals len, because expression cannot end with '>'
                Assert.isTrue(position <= len && text.charAt(position) == '>', () -> "Invalid '->' :" + text);
                list.add(text.substring(mark, position - 1).trim());
                list.add(SEP_STAGE);
                mark = ++position;
            }
        }
        if (position > mark) {
            list.add(text.substring(mark, position).trim());
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0, n = list.size() - 1; i <= n; i++) {
            String item = list.get(i);
            if (StringUtils.isBlank(item)) {
                // skip empty string
                continue;
            }
            if (ALL_SYMBOLS.contains(item)) {
                builder.append(item);
            } else if (Str.OPEN.equals(Collects.get(list, i - 1)) && Str.CLOSE.equals(Collects.get(list, i + 1))) {
                builder.append(item);
            } else {
                builder.append(Str.OPEN).append(item).append(Str.CLOSE);
            }
        }
        return builder.toString();
    }

    /**
     * Group expression by "()"
     *
     * @param expr the expression
     * @return groups of "()"
     */
    private static List<Tuple2<Integer, Integer>> group(String expr) {
        Assert.isTrue(checkParenthesis(expr), () -> "Invalid expression parenthesis: " + expr);
        int depth = 0;
        // Tuple2<position, level>
        List<Tuple2<Integer, Integer>> list = new ArrayList<>();
        for (int i = 0, n = expr.length(); i < n; i++) {
            if (expr.charAt(i) == Char.OPEN) {
                ++depth;
                if (depth <= 2) {
                    // 只取两层
                    list.add(Tuple2.of(i, depth));
                }
            } else if (expr.charAt(i) == Char.CLOSE) {
                if (depth <= 2) {
                    list.add(Tuple2.of(i, depth));
                }
                --depth;
            }
        }
        Assert.isTrue((list.size() & 0x01) == 0, () -> "Expression not pair with '()': " + expr);
        return list;
    }

    private static String wrap(String text) {
        return Str.OPEN + text + Str.CLOSE;
    }

    @Getter
    @Setter
    private static final class GraphEdge {
        private static final TypeReference<List<GraphEdge>> LIST_TYPE = new TypeReference<List<GraphEdge>>() {};

        private String source;
        private String target;

        private DAGEdge toDAGEdge() {
            return DAGEdge.of(source, target);
        }

        private static List<DAGEdge> fromJson(String text) {
            try {
                List<GraphEdge> list = Jsons.fromJson(text, LIST_TYPE);
                if (CollectionUtils.isEmpty(list)) {
                    return null;
                }
                return list.stream().map(GraphEdge::toDAGEdge).collect(Collectors.toList());
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static final class TreeNodeId implements Serializable, Comparable<TreeNodeId> {
        private static final long serialVersionUID = -468548698179536500L;
        private static final TreeNodeId ROOT_ID = new TreeNodeId(-1, -1);

        /**
         * position of "("
         */
        private final int open;

        /**
         * position of ")"
         */
        private final int close;

        private TreeNodeId(int open, int close) {
            this.open = open;
            this.close = close;
        }

        private static TreeNodeId of(int open, int close) {
            Assert.isTrue(open > -1, "Tree node id open must be greater than -1: " + open);
            Assert.isTrue(close > 0, "Tree node id close must be greater than 0: " + close);
            return new TreeNodeId(open, close);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TreeNodeId)) {
                return false;
            }
            TreeNodeId that = (TreeNodeId) obj;
            return this.open  == that.open
                && this.close == that.close;
        }

        @Override
        public int hashCode() {
            return open + close;
        }

        @Override
        public int compareTo(TreeNodeId other) {
            int n = this.open - other.open;
            return n != 0 ? n : (this.close - other.close);
        }

        @Override
        public String toString() {
            return "(" + open + "," + close + ")";
        }
    }

    private static final class PartitionIdentityKey {
        private final String expr;
        private final int open;
        private final int close;

        private PartitionIdentityKey(String expr, int open, int close) {
            Assert.hasText(expr, () -> "Partition expression cannot be blank: " + expr);
            Assert.isTrue(open > -1, () -> "Partition key open must be greater than -1: " + open);
            Assert.isTrue(close > 0, () -> "Partition key close must be greater than 0: " + close);
            this.expr = expr;
            this.open = open;
            this.close = close;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PartitionIdentityKey)) {
                return false;
            }
            PartitionIdentityKey that = (PartitionIdentityKey) obj;
            // 比较对象地址
            return this.expr  == that.expr
                && this.open  == that.open
                && this.close == that.close;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(expr) + open + close;
        }

        private String partition() {
            return expr.substring(open, close).trim();
        }
    }

}
