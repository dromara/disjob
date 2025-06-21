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

package cn.ponfee.disjob.common.util;

import java.io.IOException;
import java.util.stream.Stream;

import static com.google.common.base.Strings.repeat;

/**
 * Print text box
 *
 * @author Ponfee
 */
public class TextBoxPrinter {

    public static String print(String title, String... rows) {
        StringBuilder builder = new StringBuilder();
        try {
            print(builder, title, rows);
        } catch (IOException e) {
            // cannot happen
            throw new RuntimeException(e);
        }
        return builder.toString();
    }

    public static void print(Appendable output, String title, String... rows) throws IOException {
        // 内容左右各保留一个空格
        int padding = 1;
        // 计算最大长度作为宽度
        int width = Stream.of(rows).mapToInt(String::length).reduce(title.length(), Math::max) + padding * 2;
        String line = repeat("─", width);

        // print top border
        drawRow(output, "┌", line, "┐");
        // print title
        drawRow(output, "│", textCenter(title, width), "│");
        if (rows.length > 0) {
            // print line separator
            drawRow(output, "├", line, "┤");
            for (String row : rows) {
                // print each row
                drawRow(output, "│", textPadding(row, width, padding), "│");
            }
        }
        // print bottom border
        drawRow(output, "└", line, "┘");
    }

    // ------------------------------------------------------------private methods

    private static void drawRow(Appendable output, String left, String content, String right) throws IOException {
        output.append(left).append(content).append(right).append('\n');
    }

    private static String textCenter(String text, int width) {
        return textPadding(text, width, (width - text.length()) / 2);
    }

    private static String textPadding(String text, int width, int leftPaddingCount) {
        int rightPaddingCount = width - text.length() - leftPaddingCount;
        return repeat(" ", leftPaddingCount) + text + repeat(" ", rightPaddingCount);
    }

}
