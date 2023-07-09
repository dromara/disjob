/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.dag;

import cn.ponfee.disjob.common.util.MavenProjects;
import com.google.common.graph.EndpointPair;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.Node;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static guru.nidi.graphviz.attribute.GraphAttr.SplineMode.CURVED;
import static guru.nidi.graphviz.attribute.GraphAttr.splines;
import static guru.nidi.graphviz.attribute.Rank.RankDir.LEFT_TO_RIGHT;
import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.node;

/**
 * DAG printer
 *
 * @author Ponfee
 */
public class DAGPrinter {

    public static void main(String[] args) throws Exception {
        drawGraph("A", "dag0.png");
        drawGraph("A -> B,C,D", "dag1.png");
        drawGraph("A,B,C -> D", "dag2.png");
        drawGraph("A -> B,C,D -> E", "dag3.png");
        drawGraph("A -> B,C -> E,(F->G) -> H", "dag4.png");
        drawGraph("A -> (B->C->D),(A->F) -> G,H,X -> J ; A->Y", "dag5.png");
        drawGraph("ALoader -> (BMap->CMap->DMap),(AMap->FMap) -> GShuffle,HShuffle,XShuffle -> JReduce ; A->Y", "dag6.png");
        drawGraph("A->B,C,(D->E)->F,E->G", "dag7.png");

        drawGraph("A->B,C,D",                 "10.png");
        drawGraph("A->B->C,D",                "20.png");
        drawGraph("A->B->C->D->G;A->E->F->G", "30.png");
        drawGraph("A->(B->C->D),(E->F)->G",   "31.png");
        drawGraph("A->B->C,D,E;A->H->I,J,K",  "40.png");
        drawGraph("A->(B->C,D,E),(H->I,J,K)", "41.png");
        drawGraph("A,B,C->D",                 "50.png");
    }

    private static void drawGraph(String expr, String fileName) throws IOException {
        MutableGraph graph = mutGraph("example").setDirected(true);
        graph.graphAttrs().add(Rank.dir(LEFT_TO_RIGHT));
        graph.graphAttrs().add(splines(CURVED));
        graph.graphAttrs().add(splines(CURVED));

        for (EndpointPair<DAGNode> edge : new DAGExpressionParser(expr).parse().edges()) {
            DAGNode s = edge.source(), t = edge.target();
            Node source = node(s.toString()).with(s.isStart() ? Shape.M_DIAMOND : Shape.RECTANGLE, Label.of(s.getName()));
            Node target = node(t.toString()).with(t.isEnd() ? Shape.M_SQUARE : Shape.RECTANGLE, Label.of(t.getName()));
            graph.add(source.link(target));
        }

        // Render the graph as PNG image
        File file = new File(MavenProjects.getProjectBaseDir() + "/target/dag/" + fileName);
        FileUtils.deleteQuietly(file);
        Graphviz.fromGraph(graph).width(800).render(Format.PNG).toFile(file);
    }
}
