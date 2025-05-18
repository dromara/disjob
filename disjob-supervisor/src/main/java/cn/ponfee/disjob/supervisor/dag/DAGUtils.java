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

import cn.ponfee.disjob.common.dag.DAGExpression;
import cn.ponfee.disjob.common.dag.DAGNode;
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
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * DAG utilities
 *
 * @author Ponfee
 */
public class DAGUtils {

    public static void drawImage(String expression, boolean thumb, int width, OutputStream output) throws IOException {
        drawImage(expression, thumb, "PNG", width, output);
    }

    public static void drawImage(String expression, boolean thumb, String format, int width, OutputStream output) throws IOException {
        MutableGraph graph = Factory.mutGraph().setDirected(true);
        graph.graphAttrs().add(Rank.dir(Rank.RankDir.LEFT_TO_RIGHT));
        graph.graphAttrs().add(GraphAttr.splines(GraphAttr.SplineMode.CURVED));
        MutableInt begin = new MutableInt('A');
        Map<String, String> map = new HashMap<>();
        for (EndpointPair<DAGNode> edge : DAGExpression.parse(expression).edges()) {
            DAGNode sn = edge.source(); // source node
            DAGNode tn = edge.target(); // target node
            String sl = sn.getName();   // source label
            String tl = tn.getName();   // target label
            if (thumb) {
                if (!sn.isStartOrEnd()) {
                    sl = map.computeIfAbsent(sl, k -> String.valueOf((char) begin.getAndIncrement()));
                }
                if (!tn.isStartOrEnd()) {
                    tl = map.computeIfAbsent(tl, k -> String.valueOf((char) begin.getAndIncrement()));
                }
            }
            Node source = Factory.node(sn.toString()).with(sn.isStart() ? Shape.M_DIAMOND : Shape.RECTANGLE, Label.of(sl));
            Node target = Factory.node(tn.toString()).with(tn.isEnd() ? Shape.M_SQUARE : Shape.RECTANGLE, Label.of(tl));
            graph.add(source.link(target));
        }

        Graphviz.fromGraph(graph).width(width).render(Format.valueOf(format)).toOutputStream(output);
    }

}
