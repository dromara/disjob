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

import java.io.IOException;
import java.io.OutputStream;

/**
 * DAG utilities
 *
 * @author Ponfee
 */
public class DAGUtils {

    public static void drawPngImage(String expression, String name, int width, OutputStream output) throws IOException {
        MutableGraph graph = Factory.mutGraph(name).setDirected(true);
        graph.graphAttrs().add(Rank.dir(Rank.RankDir.LEFT_TO_RIGHT));
        graph.graphAttrs().add(GraphAttr.splines(GraphAttr.SplineMode.CURVED));
        for (EndpointPair<DAGNode> edge : DAGExpression.parse(expression).edges()) {
            DAGNode s = edge.source(), t = edge.target();
            Node source = Factory.node(s.toString()).with(s.isStart() ? Shape.M_DIAMOND : Shape.RECTANGLE, Label.of(s.getName()));
            Node target = Factory.node(t.toString()).with(t.isEnd() ? Shape.M_SQUARE : Shape.RECTANGLE, Label.of(t.getName()));
            graph.add(source.link(target));
        }

        Graphviz.fromGraph(graph).width(width).render(Format.PNG).toOutputStream(output);
    }

}
