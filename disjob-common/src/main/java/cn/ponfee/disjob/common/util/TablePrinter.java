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

import cn.ponfee.disjob.common.collect.Collects;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.List;

/**
 * Print table
 *
 * @author Ponfee
 */
public enum TablePrinter {

    /**
     * 半角
     */
    HALF("+", "+", "+", "+", "+", "+", "+", "+", "+", "-", "|"),

    /**
     * 全角
     */
    FULL("┌", "┬", "┐", "└", "┴", "┘", "├", "┼", "┤", "─", "│"),

    ;

    private final String tl; // top left
    private final String tc; // top center
    private final String tr; // top right
    private final String bl; // bottom left
    private final String bc; // bottom center
    private final String br; // bottom right
    private final String ml; // middle left
    private final String mm; // middle-middle (cross)
    private final String mr; // middle right
    private final String hr; // horizontal
    private final String vr; // vertical

    TablePrinter(String tl, String tc, String tr,
                 String bl, String bc, String br,
                 String ml, String mm, String mr,
                 String hr, String vr) {
        this.tl = tl;
        this.tc = tc;
        this.tr = tr;
        this.bl = bl;
        this.bc = bc;
        this.br = br;
        this.ml = ml;
        this.mm = mm;
        this.mr = mr;
        this.hr = hr;
        this.vr = vr;
    }

    public String print(String header, List<String> rows) {
        String[] headers = (header != null) ? new String[]{header} : null;
        return print(headers, Collects.convert(rows, e -> new String[]{e}));
    }

    public String print(String[] headers, List<String[]> rows) {
        StringBuilder builder = new StringBuilder();
        try {
            print(builder, headers, rows);
            return builder.toString();
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public void print(Appendable output, String[] headers, List<String[]> rows) throws IOException {
        // build column line
        boolean withHeader = (headers != null);
        String[] colLines = new String[withHeader ? headers.length : rows.get(0).length];
        for (int i = 0; i < colLines.length; i++) {
            int maxWidth = withHeader ? StringUtils.length(headers[i]) : 0;
            for (String[] row : rows) {
                maxWidth = Math.max(maxWidth, StringUtils.length(row[i]));
            }
            colLines[i] = Strings.repeat(hr, maxWidth + 2);
        }

        // print top border: ┌──┬──┐
        printBorder(output, colLines, tl, tc, tr, true);

        if (withHeader) {
            // print header: │  header   │
            printRow(output, colLines, headers, true);
        }

        if (!rows.isEmpty()) {
            if (withHeader) {
                // print middle border: ├──┼──┤
                printBorder(output, colLines, ml, mm, mr, true);
            }
            for (String[] row : rows) {
                // print row: │ row    │
                printRow(output, colLines, row, false);
            }
        }

        // print bottom border: └──┴──┘
        printBorder(output, colLines, bl, bc, br, false);
    }

    // ------------------------------------------------------------private methods

    private void printBorder(Appendable output, String[] colLines,
                             String left, String center, String right, boolean newLine) throws IOException {
        output.append(left);
        for (int i = 0, n = colLines.length - 1; i <= n; i++) {
            output.append(colLines[i]).append(i < n ? center : right);
        }
        if (newLine) {
            output.append('\n');
        }
    }

    private void printRow(Appendable output, String[] colLines,
                          String[] cells, boolean alignCenter) throws IOException {
        output.append(vr);
        for (int i = 0; i < colLines.length; i++) {
            String cell = (cells[i] != null) ? cells[i] : "";
            int paddingCount = colLines[i].length() - cell.length();
            String leftPadding = alignCenter ? Strings.repeat(" ", paddingCount / 2) : " ";
            String rightPadding = Strings.repeat(" ", paddingCount - leftPadding.length());
            output.append(leftPadding).append(cell).append(rightPadding).append(vr);
        }
        output.append('\n');
    }

}
