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

package cn.ponfee.disjob.common.tree.print;

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.tuple.Tuple4;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

/**
 * Print multiway tree
 *
 * <pre>{@code
 *   MultiwayTreePrinter<File> printer = new MultiwayTreePrinter<>(
 *       System.out,
 *       File::getName,
 *       f -> Optional.ofNullable(f.listFiles()).map(Arrays::asList).orElse(Collections.emptyList())
 *   );
 *   printer.print(new File("/path/dir"));
 * }</pre>
 *
 * @author Ponfee
 */
public final class MultiwayTreePrinter<T> {

    private final Appendable output;
    private final Function<T, CharSequence> nodeLabel;
    private final Function<T, List<T>> nodeChildren;

    public MultiwayTreePrinter(Appendable output,
                               Function<T, CharSequence> nodeLabel,
                               Function<T, List<T>> nodeChildren) {
        this.output = output;
        this.nodeLabel = nodeLabel;
        this.nodeChildren = nodeChildren;
    }

    /*
    // DFS递归方式
    public void print(T root) throws IOException {
        print(root, null, true);
    }

    private void print(T node, String indent, boolean isLast) throws IOException {
        if (indent == null) {
            indent = "";
        } else {
            output.append(indent);
            if (isLast) {
                output.append("└── ");
                indent += "    ";
            } else {
                output.append("├── ");
                indent += "│   ";
            }
        }
        output.append(nodeLabel.apply(node)).append('\n');

        // print children
        List<T> children = nodeChildren.apply(node);
        if (children != null) {
            for (int i = 0, n = children.size() - 1; i <= n; i++) {
                print(children.get(i), indent, i == n);
            }
        }
    }
    */

    public void print(T root) throws IOException {
        Deque<Tuple4<T, String, String, String>> stack = Collects.newArrayDeque(Tuple4.of(root, "", null, ""));
        while (!stack.isEmpty()) {
            Tuple4<T, String, String, String> tuple = stack.pop();
            output.append(tuple.b).append(tuple.d).append(nodeLabel.apply(tuple.a)).append('\n');

            List<T> children = nodeChildren.apply(tuple.a);
            if (children != null && !children.isEmpty()) {
                String indent = (tuple.c == null) ? tuple.b : tuple.b + tuple.c;
                int index = 0;
                for (T child : Lists.reverse(children)) {
                    if (index++ == 0) {
                        // last child of parent
                        stack.push(Tuple4.of(child, indent, "    ", "└── "));
                    } else {
                        stack.push(Tuple4.of(child, indent, "│   ", "├── "));
                    }
                }
            }
        }
    }

}
