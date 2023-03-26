/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.graph;

import cn.ponfee.scheduler.common.base.tuple.Tuple2;
import cn.ponfee.scheduler.common.tree.TreeNode;
import cn.ponfee.scheduler.common.tree.print.MultiwayTreePrinter;
import com.google.common.graph.Graph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.isEqualCollection;

/**
 * DAGParserTest
 *
 * @author Ponfee
 */
public class DAGParserTest {

    private static final MultiwayTreePrinter<TreeNode<DAGParser.TreeNodeId, Object>> TREE_PRINTER =
        new MultiwayTreePrinter<>(System.out, e -> e.getNid().toString(), TreeNode::getChildren);

    @Test
    public void testSplit() {
        Assertions.assertTrue(isEqualCollection(asList("->", "->", "->"), DAGParser.split("->->->", "->")));
        Assertions.assertTrue(isEqualCollection(asList("a", "->", "->", "->"), DAGParser.split("a->->->", "->")));
        Assertions.assertTrue(isEqualCollection(asList("->", "->", "->", "b"), DAGParser.split("->->->b", "->")));
        Assertions.assertTrue(isEqualCollection(asList("a", "->", "b", "->", "c", "->", "d"), DAGParser.split("a->b->c->d", "->")));
    }

    @Test
    public void testPartition() {
        Assertions.assertTrue(isEqualCollection(asList(Tuple2.of(0, 1), Tuple2.of(7, 1)), DAGParser.partition("(A -> B)")));

        Assertions.assertTrue(isEqualCollection(
            asList(Tuple2.of(0, 1), Tuple2.of(4, 2), Tuple2.of(12, 2), Tuple2.of(14, 2), Tuple2.of(19, 2), Tuple2.of(22, 2), Tuple2.of(26, 2), Tuple2.of(30, 1)),
            DAGParser.partition("(A->(B->C->D),(E->F)->(G,H)->J)")
        ));

        System.out.println(DAGParser.partition(")A->(B->C->D),(E->F)->)G,H(->J("));
    }

    @Test
    public void testValidate() {
        Assertions.assertTrue(DAGParser.checkParenthesis("(A->(B->C->D),(E->F)->(G,H)->J)"));
        Assertions.assertTrue(DAGParser.checkParenthesis("afdsafd"));
        Assertions.assertFalse(DAGParser.checkParenthesis("((A->(B->C->D),(E->F)->(G,H)->J)"));
        Assertions.assertFalse(DAGParser.checkParenthesis(")A->(B->C->D),(E->F)->)G,H(->J("));
        Assertions.assertFalse(DAGParser.checkParenthesis(")("));
    }

    @Test
    public void testResolve() {
        Assertions.assertTrue(isEqualCollection(asList("A", "->", "B"), DAGParser.resolve("A->B")));
        Assertions.assertTrue(isEqualCollection(asList("A", ",", "B"), DAGParser.resolve("A,B")));
        Assertions.assertTrue(isEqualCollection(asList("B->C->D", ",", "E->F"), DAGParser.resolve("((B->C->D),(E->F))")));
        Assertions.assertTrue(isEqualCollection(asList("B->C->D", ",", "E->F"), DAGParser.resolve("(B->C->D),(E->F)")));
    }

    @Test
    public void testBuildTree() throws IOException {
        List<Tuple2<Integer, Integer>> partitions = DAGParser.partition("(A->(B->C->D),(A->F)->(G,H,X)->J)");
        TreeNode<DAGParser.TreeNodeId, Object> root = DAGParser.buildTree(partitions);
        Assertions.assertEquals(root.getChildrenCount(), 3);
        System.out.println("------------------");
        TREE_PRINTER.print(root);

        partitions = DAGParser.partition("((A->((B->C->D),(E->F))->(G,H)->J))");
        root = DAGParser.buildTree(partitions);
        Assertions.assertEquals(root.getChildrenCount(), 1);
        System.out.println("\n------------------");
        TREE_PRINTER.print(root);

        partitions = DAGParser.partition("(A->((B->C->D),(E->F))->(G,H)->J)");
        root = DAGParser.buildTree(partitions);
        Assertions.assertEquals(root.getChildrenCount(), 2);
        System.out.println("\n------------------");
        TREE_PRINTER.print(root);
    }

    @Test
    public void test3() throws IOException {
        //String text = "(A->(B->C->D),(E->F)->(G,H)->J)";
        String text = "(A->((B->C->D),(A->F))->(G,H,X)->J)";
        //String text = "((B->C->D),(E->F))";
        //String text = "(A->B)";
        //String text = "(A)";
        //String text = "((A->((B->C->D),(E->F))->(G,H)->J))";
        //String text = "((B->C->D),(E->F)->(G,H)->J)";
        //String text = "(A,B)";
        System.out.println("graph result: " + new DAGParser(text).parse());

        assertParseEquals(text, text);
        assertParseEquals("(A->((B->C->D),(A->F))->(G,H,X)->J) ", " A->((B->C->D),(A->F))->(G,H,X)->J");
        assertParseEquals("(A->(B->C->D),(A->F)->(G,H,X)->J)", "A->((B->C->D),(A->F))->(G,H,X)->J");
        assertParseEquals("(A->((B->C->D),(A->F))->G,H,X->J)", " A->((B->C->D),(A->F))->(G,H,X)->J");
        assertParseEquals("(A->(B->C->D),(A->F)->G,H,X->J)", "A->((B->C->D),(A->F))->(G,H,X)->J");
    }

    @Test
    public void test4() {
        //String text = "(A->((B->C->D),(A->F))->(G,H,X)->J)";
        String text = "(A->((B->C->D),(A->F))->(G,H,X)->J);(A->Y)";
        Graph<GraphNodeId> graph = new DAGParser(text).parse();
        Assertions.assertEquals("[<0:0:HEAD -> 1:1:A>, <0:0:HEAD -> 2:3:A>, <1:1:A -> 1:1:B>, <1:1:A -> 1:2:A>, <1:1:B -> 1:1:C>, <1:1:C -> 1:1:D>, <1:1:D -> 1:1:G>, <1:1:D -> 1:1:H>, <1:1:D -> 1:1:X>, <1:1:G -> 1:1:J>, <1:1:J -> 0:0:TAIL>, <1:1:H -> 1:1:J>, <1:1:X -> 1:1:J>, <1:2:A -> 1:1:F>, <1:1:F -> 1:1:G>, <1:1:F -> 1:1:H>, <1:1:F -> 1:1:X>, <2:3:A -> 2:1:Y>, <2:1:Y -> 0:0:TAIL>]", graph.edges().toString());
        System.out.println("graph result: " + graph);
    }

    @Test
    public void test5() {
        String text = "(A,B)->(C->D),(A->E),(B->F)->G";
        Graph<GraphNodeId> graph = new DAGParser(text).parse();
        Assertions.assertEquals("[<0:0:HEAD -> 1:1:A>, <0:0:HEAD -> 1:2:B>, <1:1:A -> 1:1:C>, <1:1:A -> 1:2:A>, <1:1:A -> 1:1:B>, <1:1:C -> 1:1:D>, <1:1:D -> 1:1:G>, <1:1:G -> 0:0:TAIL>, <1:2:A -> 1:1:E>, <1:1:E -> 1:1:G>, <1:1:B -> 1:1:F>, <1:1:F -> 1:1:G>, <1:2:B -> 1:1:C>, <1:2:B -> 1:2:A>, <1:2:B -> 1:1:B>]", graph.edges().toString());
        System.out.println("graph result: " + graph);
    }

    private static void assertParseEquals(String text1, String text2) {
        Assertions.assertEquals(new DAGParser(text1).parse(), new DAGParser(text2).parse());
    }

}
