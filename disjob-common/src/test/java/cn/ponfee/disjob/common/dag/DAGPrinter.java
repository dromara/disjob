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

import cn.ponfee.disjob.common.util.MavenProjects;
import com.google.common.graph.EndpointPair;
import guru.nidi.graphviz.attribute.GraphAttr;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.Node;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static guru.nidi.graphviz.attribute.GraphAttr.SplineMode.CURVED;
import static guru.nidi.graphviz.attribute.Rank.RankDir.LEFT_TO_RIGHT;

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
        drawGraph("A->B,C,(D->E)->D,F->G", "dag7.png");

        drawGraph("A->B,C,D",                 "10.png");
        drawGraph("A->B->C,D",                "20.png");
        drawGraph("A->B->C->D->G;A->E->F->G", "30.png");
        drawGraph("A->(B->C->D),(E->F)->G",   "31.png");
        drawGraph("A->B->C,D,E;A->H->I,J,K",  "40.png");
        drawGraph("A->(B->C,D,E),(H->I,J,K)", "41.png");
        drawGraph("A,B,C->D",                 "50.png");

        drawGraph(
            "[                                                \n" +
            "  {\"source\": \"1:1:A\", \"target\": \"1:1:C\"},\n" +
            "  {\"source\": \"1:1:A\", \"target\": \"1:1:D\"},\n" +
            "  {\"source\": \"1:1:B\", \"target\": \"1:1:D\"},\n" +
            "  {\"source\": \"1:1:B\", \"target\": \"1:1:E\"} \n" +
            "]                                                  ",
            "json-graph.png"
        );
    }

    private static void drawGraph(String expr, String fileName) throws IOException {
        MutableGraph graph = Factory.mutGraph("example").setDirected(true);
        graph.graphAttrs().add(Rank.dir(LEFT_TO_RIGHT));
        graph.graphAttrs().add(GraphAttr.splines(CURVED));
        graph.graphAttrs().add(GraphAttr.splines(CURVED));

        for (EndpointPair<DAGNode> edge : new DAGExpressionParser(expr).parse().edges()) {
            DAGNode s = edge.source(), t = edge.target();
            Node source = Factory.node(s.toString()).with(s.isStart() ? Shape.M_DIAMOND : Shape.RECTANGLE, Label.of(s.getName()));
            Node target = Factory.node(t.toString()).with(t.isEnd() ? Shape.M_SQUARE : Shape.RECTANGLE, Label.of(t.getName()));
            graph.add(source.link(target));
        }

        // Render the graph as PNG image
        File file = new File(MavenProjects.getProjectBaseDir() + "/target/dag/" + fileName);
        FileUtils.deleteQuietly(file);
        Graphviz.fromGraph(graph).width(800).render(Format.PNG).toFile(file);
    }
}
