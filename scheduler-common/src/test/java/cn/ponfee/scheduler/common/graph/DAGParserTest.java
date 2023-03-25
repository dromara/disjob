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
import org.junit.Assert;
import org.junit.Test;

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

    private static final MultiwayTreePrinter<TreeNode<TreeNodeId, Object>> TREE_PRINTER =
        new MultiwayTreePrinter<>(System.out, e -> e.getNid().toString(), TreeNode::getChildren);

    @Test
    public void testSplit() {
        Assert.assertTrue(isEqualCollection(asList("->", "->", "->"), DAGParser.split("->->->", "->")));
        Assert.assertTrue(isEqualCollection(asList("a", "->", "->", "->"), DAGParser.split("a->->->", "->")));
        Assert.assertTrue(isEqualCollection(asList("->", "->", "->", "b"), DAGParser.split("->->->b", "->")));
        Assert.assertTrue(isEqualCollection(asList("a", "->", "b", "->", "c", "->", "d"), DAGParser.split("a->b->c->d", "->")));
    }

    @Test
    public void testPartition() {
        Assert.assertTrue(isEqualCollection(asList(Tuple2.of(0, 1), Tuple2.of(7, 1)), DAGParser.partition("(A -> B)")));

        Assert.assertTrue(isEqualCollection(
            asList(Tuple2.of(0, 1), Tuple2.of(4, 2), Tuple2.of(12, 2), Tuple2.of(14, 2), Tuple2.of(19, 2), Tuple2.of(22, 2), Tuple2.of(26, 2), Tuple2.of(30, 1)),
            DAGParser.partition("(A->(B->C->D),(E->F)->(G,H)->J)")
        ));
    }

    @Test
    public void testResolve() {
        Assert.assertTrue(isEqualCollection(asList("A", "->", "B"), DAGParser.resolve("A->B")));
        Assert.assertTrue(isEqualCollection(asList("A", ",", "B"), DAGParser.resolve("A,B")));
        Assert.assertTrue(isEqualCollection(asList("B->C->D", ",", "E->F"), DAGParser.resolve("((B->C->D),(E->F))")));
        Assert.assertTrue(isEqualCollection(asList("B->C->D", ",", "E->F"), DAGParser.resolve("(B->C->D),(E->F)")));
    }

    @Test
    public void testBuildTree() throws IOException {
        List<Tuple2<Integer, Integer>> partitions = DAGParser.partition("(A->(B->C->D),(A->F)->(G,H,X)->J)");
        TreeNode<TreeNodeId, Object> root = DAGParser.buildTree(partitions);
        Assert.assertEquals(root.getChildrenCount(), 3);
        System.out.println("------------------");
        TREE_PRINTER.print(root);

        partitions = DAGParser.partition("((A->((B->C->D),(E->F))->(G,H)->J))");
        root = DAGParser.buildTree(partitions);
        Assert.assertEquals(root.getChildrenCount(), 1);
        System.out.println("\n------------------");
        TREE_PRINTER.print(root);

        partitions = DAGParser.partition("(A->((B->C->D),(E->F))->(G,H)->J)");
        root = DAGParser.buildTree(partitions);
        Assert.assertEquals(root.getChildrenCount(), 2);
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

        assertParse(text, text);
        assertParse("(A->((B->C->D),(A->F))->(G,H,X)->J) ", " A->((B->C->D),(A->F))->(G,H,X)->J");
        assertParse("(A->(B->C->D),(A->F)->(G,H,X)->J)", "A->((B->C->D),(A->F))->(G,H,X)->J");
        assertParse("(A->((B->C->D),(A->F))->G,H,X->J)", " A->((B->C->D),(A->F))->(G,H,X)->J");
        assertParse("(A->(B->C->D),(A->F)->G,H,X->J)", "A->((B->C->D),(A->F))->(G,H,X)->J");
    }

    @Test
    public void test4() {
        //String text = "(A->((B->C->D),(A->F))->(G,H,X)->J)";
        String text = "(A->((B->C->D),(A->F))->(G,H,X)->J);(A->Y)";
        System.out.println("graph result: " + new DAGParser(text).parse());
    }


    private static void assertParse(String text1, String text2) {
        Assert.assertEquals(new DAGParser(text1).parse(), new DAGParser(text2).parse());
    }
}
