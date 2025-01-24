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
import cn.ponfee.disjob.common.tree.PlainNode;
import cn.ponfee.disjob.common.tree.TreeNode;
import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.common.util.Jsons;
import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parse DAG expression to graph
 *
 * <pre>
 * 解析：DAGExpression.parse( "A->((B->C->D),(A->F))->(G,H,X)->J; A->Y" );
 * 结果：
 *  topology-1: "A->((B->C->D),(A->F))->(G,H,X)->J"
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
 *  topology-2: "A->Y"
 *    <0:0:Start -> 2:3:A>
 *    <2:3:A -> 2:1:Y>
 *    <2:1:Y -> 0:0:End>
 *
 * ---------------------------------------------------
 *
 * 无法用`plain expression`来表达的场景，如：[A->C, A->D, B->D, B->E]
 * ┌─────────────────────────────────┐
 * │               ┌─────>C──┐       │
 * │        ┌──>A──┤         │       │
 * │        │      └──┐      │       │
 * │ Start──┤         ├──>D──┼──>End │
 * │        │      ┌──┘      │       │
 * │        └──>B──┤         │       │
 * │               └─────>E──┘       │
 * └─────────────────────────────────┘
 * 此时可通过`json expression`来表达：
 *   [
 *     "1:1:A -> 1:1:C",
 *     "1:1:A -> 1:1:D",
 *     "1:1:B -> 1:1:D",
 *     "1:1:B -> 1:1:E"
 *   ]
 * </pre>
 *
 * @author Ponfee
 */
public class DAGExpression {

    /**
     * <pre>
     * 1、(?i) 开启大小写忽略模式，但是只适用于ASCII字符
     * 2、(?u) 开启utf-8编码模式
     * 3、(?s) 单行模式，“.”匹配任意字符(包括空白字符)
     * 4、(?m) 开启多行匹配模式，“.”不匹配空白字符
     * 5、(?d) 单行模式，“.”不匹配空白字符
     *
     * Match json array: [ "..." ]
     * 有两种方式：
     *   1、(?s)^\s*\[\s*".+"\s*]\s*$
     *   2、(?m)^\s*\[\s*"(\s*\S+\s*)+"\s*]\s*$
     * </pre>
     */
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("(?s)^\\s*\\[\\s*\".+\"\\s*]\\s*$");

    /**
     * DAGNode pattern, for example `1:1:A`
     */
    private static final Pattern JSON_ITEM_PATTERN = Pattern.compile("^\\d+:\\d+:(\\s*\\S+\\s*)+$");

    /**
     * Thumb split plain expression pattern
     */
    private static final Pattern THUMB_SPLIT_PATTERN = Pattern.compile("(->)|(,)|(\\()|(\\))|(;)");

    private static final String SEP_TOPOLOGY = ";";
    private static final String SEP_STAGE = "->";
    private static final String SEP_UNION = ",";
    private static final List<String> SEP_SYMBOLS   = ImmutableList.of(SEP_STAGE, SEP_UNION);
    private static final List<String> ALL_SYMBOLS   = ImmutableList.of(SEP_STAGE, SEP_UNION, Str.OPEN, Str.CLOSE);
    private static final List<String> THUMB_SYMBOLS = ImmutableList.of(SEP_STAGE, SEP_UNION, Str.OPEN, Str.CLOSE, SEP_TOPOLOGY);
    private static final char[] SINGLE_SYMBOLS      = {Char.OPEN, Char.CLOSE, Char.COMMA};

    /**
     * Expression
     */
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

    /**
     * Parse the text expression to graph
     *
     * @param expression the text expression
     * @return graph
     */
    public static Graph<DAGNode> parse(String expression) {
        return new DAGExpression(expression).parse();
    }

    /**
     * 缩略(压缩)表达式，便于美观及直观的展示：thumb("Extract -> Transform -> Load ; Extract -> Load")  =>  "A->B->C;A->C"
     *
     * @param expression the text expression
     * @return thumbnail expression
     */
    public static String thumb(String expression) {
        List<DAGEdge> edges = parseJsonArray(expression);
        return edges != null ? thumbJsonExpr(edges) : thumbPlainExpr(expression);
    }

    // ------------------------------------------------------------------------------------private methods

    private DAGExpression(String expression) {
        Assert.hasText(expression, "Expression cannot be blank.");
        this.expression = expression.trim();
    }

    private Graph<DAGNode> parse() {
        ImmutableGraph.Builder<DAGNode> graphBuilder = GraphBuilder.directed().allowsSelfLoops(false).immutable();

        List<DAGEdge> edges = parseJsonArray(expression);
        if (edges != null) {
            parseJsonExpr(graphBuilder, edges);
        } else {
            parsePlainExpr(graphBuilder);
        }

        ImmutableGraph<DAGNode> graph = graphBuilder.build();
        Assert.state(graph.nodes().size() > 2, () -> "Expression not has node: " + expression);
        Assert.state(graph.successors(DAGNode.START).stream().noneMatch(DAGNode::isEnd), () -> "Expression name cannot direct end: " + expression);
        Assert.state(graph.predecessors(DAGNode.END).stream().noneMatch(DAGNode::isStart), () -> "Expression name cannot direct start: " + expression);
        Assert.state(!Graphs.hasCycle(graph), () -> "Expression topology has cycle: " + expression);
        return graph;
    }

    /**
     * <pre>
     * Parse plain text expression graph
     *
     * A->((B->C->D),(A->F))->(G,H,X)->J; A->Y
     * </pre>
     *
     * @param graphBuilder the graph builder
     */
    private void parsePlainExpr(ImmutableGraph.Builder<DAGNode> graphBuilder) {
        Assert.isTrue(checkParenthesis(expression), () -> "Invalid expression parenthesis parse: " + expression);
        List<String> topologies = Stream.of(expression.split(SEP_TOPOLOGY))
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .collect(Collectors.toList());
        Assert.notEmpty(topologies, () -> "Invalid split with ';' expression: " + expression);

        for (int i = 0, n = topologies.size(); i < n; i++) {
            String topology = topologies.get(i);
            Assert.isTrue(checkParenthesis(topology), () -> "Invalid expression parenthesis topology: " + topology);
            String expr = completeParenthesis(topology);
            buildGraph(i + 1, Collections.singletonList(expr), graphBuilder, DAGNode.START, DAGNode.END);
        }
    }

    private void buildGraph(int topology, List<String> expressions,
                            ImmutableGraph.Builder<DAGNode> graphBuilder, DAGNode prev, DAGNode next) {
        // 划分第一个stage
        Tuple2<List<String>, List<String>> tuple = divideFirstStage(expressions);
        if (tuple == null) {
            return;
        }

        List<String> first = tuple.a, remains = tuple.b;
        for (int i = 0, n = first.size() - 1; i <= n; i++) {
            List<String> list = resolve(first.get(i));
            Assert.notEmpty(list, () -> "Invalid expression build graph: " + String.join("", expressions));
            if (list.size() == 1) {
                String name = list.get(0);
                DAGNode node = DAGNode.of(topology, incrementOrdinal(name), name);
                graphBuilder.putEdge(prev, node);
                if (remains == null) {
                    graphBuilder.putEdge(node, next);
                } else {
                    buildGraph(topology, remains, graphBuilder, node, next);
                }
            } else {
                buildGraph(topology, concat(list, remains), graphBuilder, prev, next);
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
            return resolve(wrappedCache.computeIfAbsent(expr, DAGExpression::wrap));
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
            return resolve(wrappedCache.computeIfAbsent(expr, DAGExpression::wrap));
        } else {
            throw new IllegalArgumentException("Invalid expression outermost size: " + expr + ", " + outermost.size());
        }

        TreeNode<TreeNodeId, Object> root = buildTree(groups);
        List<Integer> list = new ArrayList<>(root.getNodeDegree() * 2 + 2);
        list.add(root.getId().open);
        root.forEachChild(child -> {
            list.add(child.getId().open);
            list.add(child.getId().close);
        });
        list.add(root.getId().close);
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

    // ------------------------------------------------------------------------------------private static methods

    private static List<DAGEdge> parseJsonArray(String json) {
        if (!JSON_ARRAY_PATTERN.matcher(json).matches()) {
            return null;
        }
        try {
            List<String> list = Jsons.fromJson(json, Jsons.LIST_STRING);
            if (CollectionUtils.isEmpty(list)) {
                return null;
            }

            List<DAGEdge> edges = new ArrayList<>(list.size());
            for (String item : list) {
                String[] array = item.split(SEP_STAGE);
                Assert.isTrue(array.length == 2, () -> "Invalid json graph item: " + item);
                DAGNode source = processJsonItem(array[0].trim());
                DAGNode target = processJsonItem(array[1].trim());
                edges.add(new DAGEdge(source, target));
            }
            return edges;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static DAGNode processJsonItem(String item) {
        Assert.hasText(item, "Json array item cannot be blank.");
        if (!JSON_ITEM_PATTERN.matcher(item).matches()) {
            item = "1:1:" + item;
        }
        return DAGNode.fromString(item);
    }

    /**
     * Parse json array expression graph
     *
     * <pre>
     * [
     *   "1:1:A -> 1:1:C",
     *   "1:1:A -> 1:1:D",
     *   "1:1:B -> 1:1:D",
     *   "1:1:B -> 1:1:E"
     * ]
     * </pre>
     *
     * @param graphBuilder the graph builder
     * @param edges        the edges
     */
    private static void parseJsonExpr(ImmutableGraph.Builder<DAGNode> graphBuilder, List<DAGEdge> edges) {
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
        allNode.stream().filter(e -> !nonHead.contains(e)).forEach(e -> graphBuilder.putEdge(DAGNode.START, e));
        allNode.stream().filter(e -> !nonTail.contains(e)).forEach(e -> graphBuilder.putEdge(e, DAGNode.END));
    }

    private static Tuple2<List<String>, List<String>> divideFirstStage(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        if (SEP_SYMBOLS.contains(Collects.getFirst(list)) || SEP_SYMBOLS.contains(Collects.getLast(list))) {
            throw new IllegalArgumentException("Invalid expression divide stage: " + String.join("", list));
        }

        if (list.size() == 1) {
            return Tuple2.of(list, null);
        }

        List<String> head = new ArrayList<>(list.size());
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
                    throw new IllegalArgumentException("Invalid expression separator: " + String.join("", list));
            }
        }
        return Tuple2.of(head, null);
    }

    private static TreeNode<TreeNodeId, Object> buildTree(List<Tuple2<Integer, Integer>> groups) {
        List<PlainNode<TreeNodeId, Object>> nodes = new ArrayList<>(groups.size() + 1);
        buildNodes(nodes, groups, null, 1, 0);
        return TreeNode.build(nodes);
    }

    private static void buildNodes(List<PlainNode<TreeNodeId, Object>> nodes,
                                   List<Tuple2<Integer, Integer>> groups,
                                   TreeNodeId parentId, int level, int start) {
        int open = -1;
        for (int i = start, n = groups.size(); i < n; i++) {
            if (groups.get(i).b < level) {
                // exit the current method
                return;
            }
            if (groups.get(i).b == level) {
                if (open == -1) {
                    open = i;
                } else {
                    // find "()" position
                    TreeNodeId id = new TreeNodeId(groups.get(open).a, groups.get(i).a);
                    nodes.add(new PlainNode<>(id, parentId));
                    buildNodes(nodes, groups, id, level + 1, open + 1);
                    open = -1;
                }
            }
        }
    }

    private static List<String> concat(List<String> sources, List<String> targets) {
        if (CollectionUtils.isEmpty(targets)) {
            return sources;
        }

        List<String> result = new ArrayList<>(sources.size() + 1 + targets.size());
        result.addAll(sources);
        result.add(SEP_STAGE);
        result.addAll(targets);
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
            if (ArrayUtils.contains(SINGLE_SYMBOLS, ch)) {
                list.add(text.substring(mark, position - 1).trim());
                list.add(Character.toString(ch));
                mark = position;
            } else if (ch == '-' && position <= len && text.charAt(position) == '>') {
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
        Assert.isTrue(checkParenthesis(expr), () -> "Invalid expression parenthesis group: " + expr);
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
        Assert.isTrue((list.size() % 2) == 0, () -> "Expression not pair with '()': " + expr);
        return list;
    }

    private static String wrap(String text) {
        return Str.OPEN + text + Str.CLOSE;
    }

    // ------------------------------------------------------------------------------------private static thumb methods

    private static String thumbJsonExpr(List<DAGEdge> edges) {
        MutableInt begin = new MutableInt('A');
        Map<String, String> map = new HashMap<>();
        List<DAGEdge> list = new ArrayList<>(edges.size());
        for (DAGEdge edge : edges) {
            DAGNode source = edge.getSource();
            String sourceThumb = map.computeIfAbsent(source.getName(), k -> String.valueOf((char) begin.getAndIncrement()));

            DAGNode target = edge.getTarget();
            String targetThumb = map.computeIfAbsent(target.getName(), k -> String.valueOf((char) begin.getAndIncrement()));

            list.add(new DAGEdge(
                DAGNode.of(source.getTopology(), source.getOrdinal(), sourceThumb),
                DAGNode.of(target.getTopology(), target.getOrdinal(), targetThumb)
            ));
        }
        return Jsons.toJson(list.stream().map(e -> e.getSource() + " -> " + e.getTarget()).toArray());
    }

    private static String thumbPlainExpr(String expression) {
        MutableInt begin = new MutableInt('A');
        Map<String, String> map = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        for (String str : splitPlainExpr(expression)) {
            if (THUMB_SYMBOLS.contains(str)) {
                builder.append(str);
            } else {
                builder.append(map.computeIfAbsent(str, k -> String.valueOf((char) begin.getAndIncrement())));
            }
        }
        return builder.toString();
    }

    private static List<String> splitPlainExpr(String expression) {
        Matcher matcher = THUMB_SPLIT_PATTERN.matcher(expression);
        List<String> list = new ArrayList<>();
        int start = 0;
        while (matcher.find()) {
            addIfNotBlank(list, expression.substring(start, matcher.start()));
            addIfNotBlank(list, matcher.group());
            start = matcher.end();
        }
        addIfNotBlank(list, expression.substring(start));
        return list;
    }

    private static void addIfNotBlank(List<String> list, String str) {
        if (StringUtils.isNotBlank(str)) {
            list.add(str.trim());
        }
    }

    // ------------------------------------------------------------------------------------private static classes

    private static final class TreeNodeId implements Serializable, Comparable<TreeNodeId> {
        private static final long serialVersionUID = -468548698179536500L;

        /**
         * position of "("
         */
        private final int open;

        /**
         * position of ")"
         */
        private final int close;

        private TreeNodeId(int open, int close) {
            Assert.isTrue(open >= 0, () -> "Tree node id open must be >= 0: " + open);
            Assert.isTrue(close > open, () -> "Tree node id close must be > open: " + close + ", " + open);
            this.open = open;
            this.close = close;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
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
        public int compareTo(TreeNodeId that) {
            int n = this.open - that.open;
            return n != 0 ? n : (this.close - that.close);
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
            Assert.isTrue(open >= 0, () -> "Partition open must be >= 0: " + open);
            Assert.isTrue(close >= open, () -> "Partition close must be >= open: " + close + ", " + open);
            this.expr = expr;
            this.open = open;
            this.close = close;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PartitionIdentityKey)) {
                return false;
            }
            PartitionIdentityKey that = (PartitionIdentityKey) obj;
            // compare object address
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
