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

package cn.ponfee.disjob.common.tree;

import cn.ponfee.disjob.common.tree.print.MultiwayTreePrinter;
import cn.ponfee.disjob.common.util.MavenProjects;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * FilePrinter test
 *
 * @author Ponfee
 */
public class FilePrinterTest {

    @Test
    public void test1() throws IOException {
        File root = new File(MavenProjects.getTestJavaPath("cn.ponfee.disjob.common"));
        Function<File, List<File>> nodeChildren = f -> Optional.ofNullable(f.listFiles()).map(Arrays::asList).orElse(Collections.emptyList());
        print(root, nodeChildren, File::getName, System.out);
    }

    @Test
    public void test2() throws IOException {
        MultiwayTreePrinter<File> printer = new MultiwayTreePrinter<>(
            System.out,
            File::getName,
            f -> Optional.ofNullable(f.listFiles()).map(Arrays::asList).orElse(Collections.emptyList())
        );
        printer.print(new File(MavenProjects.getTestJavaPath("cn.ponfee.disjob.common")));
    }

    public static <T> void print(T node, Function<T, List<T>> nodeChildren, Function<T, CharSequence> nodeLabel, Appendable output) throws IOException {
        print(node, nodeChildren, nodeLabel, output, null, true);
    }

    private static <T> void print(T node, Function<T, List<T>> nodeChildren, Function<T, CharSequence> nodeLabel, Appendable output, String indent, boolean isLast) throws IOException {
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
        output.append(nodeLabel.apply(node)).append("\n");
        List<T> children = nodeChildren.apply(node);
        if (children != null) {
            for (int i = 0, n = children.size() - 1; i <= n; i++) {
                print(children.get(i), nodeChildren, nodeLabel, output, indent, i == n);
            }
        }
    }

}
