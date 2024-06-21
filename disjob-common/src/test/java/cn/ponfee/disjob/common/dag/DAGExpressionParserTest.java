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

import cn.ponfee.disjob.common.tree.TreeNode;
import cn.ponfee.disjob.common.tree.print.MultiwayTreePrinter;
import cn.ponfee.disjob.common.tuple.Tuple2;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.isEqualCollection;

/**
 * DAGParserTest
 *
 * @author Ponfee
 */
public class DAGExpressionParserTest {

    /*
    private static final MultiwayTreePrinter<TreeNode<DAGExpressionParser.TreeNodeId, Object>> TREE_PRINTER =
        new MultiwayTreePrinter<>(System.out, e -> e.getNid().toString(), TreeNode::getChildren);

    @Test
    public void testProcess() {
        Assertions.assertEquals("((A)->(((B)->(C)->(D)),((A)->(F)))->((G),(H),(X))->(J))", DAGExpressionParser.completeParenthesis("(A->((B->C->D),(A->F))->(G,H,X)->J)"));
        Assertions.assertEquals("(A),(B)->((C)->(D)),(E)->(F)", DAGExpressionParser.completeParenthesis("A,B->(C->D),(E)->F"));
        Assertions.assertEquals("(A),(B)->((C)->(D)),(E)->(F)", DAGExpressionParser.completeParenthesis("A,B->(C->D),E->F"));
    }

    @Test
    public void testPartition() {
        Assertions.assertTrue(isEqualCollection(asList(Tuple2.of(0, 1), Tuple2.of(7, 1)), DAGExpressionParser.group("(A -> B)")));

        Assertions.assertTrue(isEqualCollection(
            asList(Tuple2.of(0, 1), Tuple2.of(4, 2), Tuple2.of(12, 2), Tuple2.of(14, 2), Tuple2.of(19, 2), Tuple2.of(22, 2), Tuple2.of(26, 2), Tuple2.of(30, 1)),
            DAGExpressionParser.group("(A->(B->C->D),(E->F)->(G,H)->J)")
        ));
    }

    @Test
    public void testValidate() {
        Assertions.assertTrue(DAGExpressionParser.checkParenthesis("(A->(B->C->D),(E->F)->(G,H)->J)"));
        Assertions.assertTrue(DAGExpressionParser.checkParenthesis("afdsafd"));
        Assertions.assertFalse(DAGExpressionParser.checkParenthesis("((A->(B->C->D),(E->F)->(G,H)->J)"));
        Assertions.assertFalse(DAGExpressionParser.checkParenthesis(")A->(B->C->D),(E->F)->)G,H(->J("));
        Assertions.assertFalse(DAGExpressionParser.checkParenthesis(")("));
        Assertions.assertFalse(DAGExpressionParser.checkParenthesis("()("));
        Assertions.assertFalse(DAGExpressionParser.checkParenthesis("())"));
        Assertions.assertFalse(DAGExpressionParser.checkParenthesis("(()"));
        Assertions.assertFalse(DAGExpressionParser.checkParenthesis(")()"));
    }

    @Test
    public void testBuildTree() throws IOException {
        List<Tuple2<Integer, Integer>> partitions = DAGExpressionParser.group("(A->(B->C->D),(A->F)->(G,H,X)->J)");
        TreeNode<DAGExpressionParser.TreeNodeId, Object> root = DAGExpressionParser.buildTree(partitions);
        Assertions.assertEquals(root.getChildrenCount(), 3);
        System.out.println("------------------");
        TREE_PRINTER.print(root);

        partitions = DAGExpressionParser.group("((A->((B->C->D),(E->F))->(G,H)->J))");
        root = DAGExpressionParser.buildTree(partitions);
        Assertions.assertEquals(root.getChildrenCount(), 1);
        System.out.println("\n------------------");
        TREE_PRINTER.print(root);

        partitions = DAGExpressionParser.group("(A->((B->C->D),(E->F))->(G,H)->J)");
        root = DAGExpressionParser.buildTree(partitions);
        Assertions.assertEquals(root.getChildrenCount(), 2);
        System.out.println("\n------------------");
        TREE_PRINTER.print(root);
    }
    */

    @Test
    public void testSameExpression() {
        String text = "(A->((B->C->D),(A->F))->(G,H,X)->J)";
        assertSameExpression(text, text);
        assertSameExpression("(A->((B->C->D),(A->F))->(G,H,X)->J) ", " A->((B->C->D),(A->F))->(G,H,X)->J");
        assertSameExpression("(A->(B->C->D),(A->F)->(G,H,X)->J)", "A->((B->C->D),(A->F))->(G,H,X)->J");
        assertSameExpression("(A->((B->C->D),(A->F))->G,H,X->J)", " A->((B->C->D),(A->F))->(G,H,X)->J");
        assertSameExpression("(A->(B->C->D),(A->F)->G,H,X->J)", "A->((B->C->D),(A->F))->(G,H,X)->J");
    }

    @Test
    public void testEdgesEquals() {
        assertEdgesEquals(
            "(A)->((B),(C))->(E),(F->G)->(H)",
            "[<0:0:Start -> 1:1:A>, <1:1:A -> 1:1:B>, <1:1:A -> 1:1:C>, <1:1:B -> 1:1:E>, <1:1:B -> 1:1:F>, <1:1:E -> 1:1:H>, <1:1:H -> 0:0:End>, <1:1:F -> 1:1:G>, <1:1:G -> 1:1:H>, <1:1:C -> 1:1:E>, <1:1:C -> 1:1:F>]"
        );
        assertEdgesEquals(
            "(A->((B->C->D),(A->F))->(G,H,X)->J);(A->Y)",
            "[<0:0:Start -> 1:1:A>, <0:0:Start -> 2:3:A>, <1:1:A -> 1:1:B>, <1:1:A -> 1:2:A>, <1:1:B -> 1:1:C>, <1:1:C -> 1:1:D>, <1:1:D -> 1:1:G>, <1:1:D -> 1:1:H>, <1:1:D -> 1:1:X>, <1:1:G -> 1:1:J>, <1:1:J -> 0:0:End>, <1:1:H -> 1:1:J>, <1:1:X -> 1:1:J>, <1:2:A -> 1:1:F>, <1:1:F -> 1:1:G>, <1:1:F -> 1:1:H>, <1:1:F -> 1:1:X>, <2:3:A -> 2:1:Y>, <2:1:Y -> 0:0:End>]"
        );
        assertEdgesEquals(
            "(A,B)->(C->D),(A->E),(B->F)->G",
            "[<0:0:Start -> 1:1:A>, <0:0:Start -> 1:2:B>, <1:1:A -> 1:1:C>, <1:1:A -> 1:2:A>, <1:1:A -> 1:1:B>, <1:1:C -> 1:1:D>, <1:1:D -> 1:1:G>, <1:1:G -> 0:0:End>, <1:2:A -> 1:1:E>, <1:1:E -> 1:1:G>, <1:1:B -> 1:1:F>, <1:1:F -> 1:1:G>, <1:2:B -> 1:1:C>, <1:2:B -> 1:2:A>, <1:2:B -> 1:1:B>]"
        );
        assertEdgesEquals(
            "A,B->C,D,C",
            "[<0:0:Start -> 1:1:A>, <0:0:Start -> 1:1:B>, <1:1:A -> 1:1:C>, <1:1:A -> 1:1:D>, <1:1:A -> 1:2:C>, <1:1:C -> 0:0:End>, <1:1:D -> 0:0:End>, <1:2:C -> 0:0:End>, <1:1:B -> 1:1:C>, <1:1:B -> 1:1:D>, <1:1:B -> 1:2:C>]"
        );
        assertEdgesEquals(
            "A,B->(C->D),E->F",
            "[<0:0:Start -> 1:1:A>, <0:0:Start -> 1:1:B>, <1:1:A -> 1:1:C>, <1:1:A -> 1:1:E>, <1:1:C -> 1:1:D>, <1:1:D -> 1:1:F>, <1:1:F -> 0:0:End>, <1:1:E -> 1:1:F>, <1:1:B -> 1:1:C>, <1:1:B -> 1:1:E>]"
        );
        assertEdgesEquals(
            "A,B->(C->D),(E)->F",
            "[<0:0:Start -> 1:1:A>, <0:0:Start -> 1:1:B>, <1:1:A -> 1:1:C>, <1:1:A -> 1:1:E>, <1:1:C -> 1:1:D>, <1:1:D -> 1:1:F>, <1:1:F -> 0:0:End>, <1:1:E -> 1:1:F>, <1:1:B -> 1:1:C>, <1:1:B -> 1:1:E>]"
        );
        assertEdgesEquals(
            "A->B;A->B",
            "[<0:0:Start -> 1:1:A>, <0:0:Start -> 2:2:A>, <1:1:A -> 1:1:B>, <1:1:B -> 0:0:End>, <2:2:A -> 2:2:B>, <2:2:B -> 0:0:End>]"
        );
    }

    @Test
    public void testGraph() {
        String expression = "(A->((B->C->D),(A->F))->(G,H,X)->J);(A->Y)";
        Graph<DAGNode> graph = DAGExpressionParser.parse(expression);
        Assertions.assertEquals("[1:1:A, 2:3:A]", graph.successors(DAGNode.START).toString());
        Assertions.assertTrue(graph.predecessors(DAGNode.START).isEmpty());

        Assertions.assertEquals("[1:1:J, 2:1:Y]", graph.predecessors(DAGNode.END).toString());
        Assertions.assertTrue(graph.successors(DAGNode.END).isEmpty());

        Assertions.assertEquals("[1:1:B, 1:2:A]", graph.successors(DAGNode.of(1, 1, "A")).toString());

        //graph.adjacentNodes();
        //graph.incidentEdges();
    }

    @Test
    public void testGraphSequence() {
        String expression = "A -> B,C -> E,(F->G) -> H";
        Graph<DAGNode> graph = DAGExpressionParser.parse(expression);
        for (EndpointPair<DAGNode> edge : graph.edges()) {
            System.out.println(edge.source() + " -> " + edge.target());
        }

        Set<DAGNode> predecessors = graph.predecessors(DAGNode.START);
        Assertions.assertTrue(predecessors instanceof Set);
        Assertions.assertTrue(predecessors.isEmpty());

        Set<DAGNode> successors = graph.successors(DAGNode.END);
        Assertions.assertTrue(successors instanceof Set);
        Assertions.assertTrue(successors.isEmpty());
    }

    @Test
    public void testDAGNode() {
        Assertions.assertTrue(DAGNode.fromString(DAGNode.START.toString()) == DAGNode.START);
        Assertions.assertTrue(DAGNode.fromString(DAGNode.END.toString()) == DAGNode.END);
        Assertions.assertEquals(DAGNode.fromString("1:1:test").toString(), "1:1:test");
        Assertions.assertEquals(DAGNode.fromString("1:1:test:ANY").getName(), "test:ANY");
        Assertions.assertEquals(DAGNode.fromString("1:1:test:ALL").getName(), "test:ALL");
        Assertions.assertEquals(DAGNode.fromString("1:1:test:ALL").toString(), "1:1:test:ALL");
    }

    // ------------------------------------------------------------------------

    private static void assertSameExpression(String text1, String text2) {
        System.out.println("\n\n------\n\n");
        Assertions.assertEquals(DAGExpressionParser.parse(text1), DAGExpressionParser.parse(text2));
    }

    private static void assertEdgesEquals(String expression, String edges) {
        System.out.println("\n\n------\n\n");
        System.out.println(expression);
        Graph<DAGNode> graph = DAGExpressionParser.parse(expression);
        Assertions.assertEquals(edges, graph.edges().toString());
        System.out.println(expression + " graph result: " + graph);
    }

}
