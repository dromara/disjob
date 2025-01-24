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

import cn.ponfee.disjob.common.util.Files;
import com.google.common.base.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Print binary tree
 *
 * @author Ponfee
 */
public class BinaryTreePrinter<T> {

    public enum Branch {
        /**
         * Rectangle
         */
        RECTANGLE,
        /**
         * Triangle
         */
        TRIANGLE
    }

    private final Appendable output;
    private final Function<T, String> nodeLabel;
    private final UnaryOperator<T> leftChild;
    private final UnaryOperator<T> rightChild;

    /**
     * 分支是方形还是三角形
     */
    private final Branch branch;

    /**
     * 是否区分左右方向：当`branch=RECTANGLE`且只有一个子节点时生效
     */
    private final boolean directed;

    /**
     * 单棵树节点间的空隙
     */
    private final int nodeSpace;

    /**
     * 多棵树时，树间的空隙
     */
    private final int treeSpace;

    BinaryTreePrinter(Appendable output,
                      Function<T, String> nodeLabel,
                      UnaryOperator<T> leftChild,
                      UnaryOperator<T> rightChild,
                      Branch branch,
                      boolean directed,
                      int nodeSpace,
                      int treeSpace) {
        this.output = output;
        this.nodeLabel = nodeLabel;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.branch = branch;
        this.directed = directed;
        this.nodeSpace = nodeSpace;
        this.treeSpace = Math.max((treeSpace / 2) * 2 + 1, 3);
    }

    /**
     * Prints ascii representation of binary tree.
     * Parameter nodeSpace is minimum number of spaces between adjacent node labels.
     * Parameter squareBranches, when set to true, results in branches being printed with ASCII box
     * drawing characters.
     */
    public void print(T root) throws IOException {
        printTreeLines(buildTreeLines(root));
    }

    /**
     * Prints ascii representations of multiple trees across page.
     * Parameter nodeSpace is minimum number of spaces between adjacent node labels in a tree.
     * Parameter treeSpace is horizontal distance between trees, as well as number of blank lines
     * between rows of trees.
     * Parameter lineWidth is maximum width of output
     * Parameter squareBranches, when set to true, results in branches being printed with ASCII box
     * drawing characters.
     *
     * @param trees     the multiple tree
     * @param lineWidth 行的宽度：小于该宽度则多棵树水平排列，否则换行后再来打印下一棵树
     */
    public void print(List<T> trees, int lineWidth) throws IOException {
        List<List<TreeLine>> allTreeLines = new ArrayList<>(trees.size());
        int[] treeWidths = new int[trees.size()];
        int[] minLeftOffsets = new int[trees.size()];
        int[] maxRightOffsets = new int[trees.size()];
        for (int i = 0; i < trees.size(); i++) {
            List<TreeLine> treeLines = buildTreeLines(trees.get(i));
            allTreeLines.add(treeLines);
            minLeftOffsets[i] = minLeftOffset(treeLines);
            maxRightOffsets[i] = maxRightOffset(treeLines);
            treeWidths[i] = maxRightOffsets[i] - minLeftOffsets[i] + 1;
        }

        String halfTreeSpaceStr = spaces(treeSpace / 2);
        int nextTreeIndex = 0;
        while (nextTreeIndex < trees.size()) {
            // print a row of trees starting at nextTreeIndex
            // first figure range of trees we can print for next row
            int sumOfWidths = treeWidths[nextTreeIndex];
            int endTreeIndex = nextTreeIndex + 1;
            while (endTreeIndex < trees.size() && sumOfWidths + treeSpace + treeWidths[endTreeIndex] < lineWidth) {
                sumOfWidths += (treeSpace + treeWidths[endTreeIndex]);
                endTreeIndex++;
            }
            endTreeIndex--;

            // find max number of lines for tallest tree
            int maxLines = allTreeLines.stream().mapToInt(List::size).max().orElse(0);

            // print trees line by line
            for (int i = 0; i < maxLines; i++) {
                for (int j = nextTreeIndex; j <= endTreeIndex; j++) {
                    List<TreeLine> treeLines = allTreeLines.get(j);
                    if (i >= treeLines.size()) {
                        output.append(spaces(treeWidths[j]));
                    } else {
                        int leftSpaces = -(minLeftOffsets[j] - treeLines.get(i).leftOffset);
                        int rightSpaces = maxRightOffsets[j] - treeLines.get(i).rightOffset;
                        output.append(spaces(leftSpaces)).append(treeLines.get(i).line).append(spaces(rightSpaces));
                    }
                    if (j < endTreeIndex) {
                        output.append(halfTreeSpaceStr).append('|').append(halfTreeSpaceStr);
                    }
                }
                output.append(Files.UNIX_LINE_SEPARATOR);
            }

            nextTreeIndex = endTreeIndex + 1;
        }
    }

    public static <T> Builder<T> builder(Function<T, String> nodeLabel,
                                         UnaryOperator<T> leftChild,
                                         UnaryOperator<T> rightChild) {
        return new Builder<>(nodeLabel, leftChild, rightChild);
    }

    public static class Builder<T> {
        private final Function<T, String> nodeLabel;
        private final UnaryOperator<T> leftChild;
        private final UnaryOperator<T> rightChild;

        private Appendable output = System.out;
        private Branch branch = Branch.RECTANGLE;
        private boolean directed = true;
        private int nodeSpace = 2;
        private int treeSpace = 5;

        private Builder(Function<T, String> nodeLabel,
                        UnaryOperator<T> leftChild,
                        UnaryOperator<T> rightChild) {
            this.nodeLabel = nodeLabel;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
        }

        public Builder<T> output(Appendable output) {
            this.output = output;
            return this;
        }

        public Builder<T> branch(Branch branch) {
            this.branch = branch;
            return this;
        }

        public Builder<T> directed(boolean directed) {
            this.directed = directed;
            return this;
        }

        public Builder<T> nodeSpace(int nodeSpace) {
            this.nodeSpace = nodeSpace;
            return this;
        }

        public Builder<T> treeSpace(int treeSpace) {
            this.treeSpace = treeSpace;
            return this;
        }

        public BinaryTreePrinter<T> build() {
            return new BinaryTreePrinter<>(
                output, nodeLabel, leftChild, rightChild,
                branch, directed, nodeSpace, treeSpace
            );
        }
    }

    // --------------------------------------------------------------------private methods

    private void printTreeLines(List<TreeLine> treeLines) throws IOException {
        if (treeLines.isEmpty()) {
            return;
        }
        int minLeftOffset = minLeftOffset(treeLines);
        int maxRightOffset = maxRightOffset(treeLines);
        for (TreeLine treeLine : treeLines) {
            output.append(spaces(-(minLeftOffset - treeLine.leftOffset)))
                  .append(treeLine.line)
                  .append(spaces(maxRightOffset - treeLine.rightOffset))
                  .append(Files.UNIX_LINE_SEPARATOR);
        }
    }

    private List<TreeLine> buildTreeLines(T root) {
        if (root == null) {
            return Collections.emptyList();
        }

        String rootLabel = nodeLabel.apply(root);
        List<TreeLine> leftTreeLines = buildTreeLines(leftChild.apply(root));
        List<TreeLine> rightTreeLines = buildTreeLines(rightChild.apply(root));

        int leftCount = leftTreeLines.size();
        int rightCount = rightTreeLines.size();
        int minCount = Math.min(leftCount, rightCount);
        int maxCount = Math.max(leftCount, rightCount);

        // The left and right subtree print representations have jagged edges, and we essentially we have to
        // figure out how close together we can bring the left and right roots so that the edges just meet on
        // some line.  Then we add hspace, and round up to next odd number.
        int maxRootSpacing = 0;
        for (int i = 0; i < minCount; i++) {
            maxRootSpacing = Math.max(maxRootSpacing, leftTreeLines.get(i).rightOffset - rightTreeLines.get(i).leftOffset);
        }
        int rootSpacing = maxRootSpacing + nodeSpace;
        if ((rootSpacing & 0x01) == 0) {
            rootSpacing++;
        }
        // rootSpacing is now the number of spaces between the roots of the two subtrees

        List<TreeLine> allTreeLines = new ArrayList<>();

        // strip ANSI escape codes to get length of rendered string. Fixes wrong padding when labels use ANSI escapes for colored nodes.
        String renderedRootLabel = rootLabel.replaceAll("\\e\\[[\\d;]*[^\\d;]", "");

        // add the root and the two branches leading to the subtrees
        allTreeLines.add(new TreeLine(rootLabel, -(renderedRootLabel.length() - 1) / 2, renderedRootLabel.length() / 2));

        // also calculate offset adjustments for left and right subtrees
        int leftTreeAdjust = 0;
        int rightTreeAdjust = 0;

        boolean hasLeftTreeLines = !leftTreeLines.isEmpty();
        boolean hasRightTreeLines = !rightTreeLines.isEmpty();
        if (hasLeftTreeLines && hasRightTreeLines) {
            // there's a left and right subtree
            if (branch == Branch.RECTANGLE) {
                int adjust = (rootSpacing / 2) + 1;
                String horizontal = String.join("", Collections.nCopies(rootSpacing / 2, "─"));
                String branch0 = "┌" + horizontal + "┴" + horizontal + "┐";
                allTreeLines.add(new TreeLine(branch0, -adjust, adjust));
                rightTreeAdjust = adjust;
                leftTreeAdjust = -adjust;
            } else {
                if (rootSpacing == 1) {
                    allTreeLines.add(new TreeLine("/ \\", -1, 1));
                    rightTreeAdjust = 2;
                    leftTreeAdjust = -2;
                } else {
                    for (int i = 1; i < rootSpacing; i += 2) {
                        String branches = "/" + spaces(i) + "\\";
                        allTreeLines.add(new TreeLine(branches, -((i + 1) / 2), (i + 1) / 2));
                    }
                    rightTreeAdjust = (rootSpacing / 2) + 1;
                    leftTreeAdjust = -((rootSpacing / 2) + 1);
                }
            }
        } else if (hasLeftTreeLines) {
            // there's a left subtree only
            if (branch == Branch.RECTANGLE) {
                if (directed) {
                    allTreeLines.add(new TreeLine("┌┘", -1, 0));
                    leftTreeAdjust = -1;
                } else {
                    allTreeLines.add(new TreeLine("│", 0, 0));
                }
            } else {
                allTreeLines.add(new TreeLine("/", -1, -1));
                leftTreeAdjust = -2;
            }
        } else if (hasRightTreeLines) {
            // there's a right subtree only
            if (branch == Branch.RECTANGLE) {
                if (directed) {
                    allTreeLines.add(new TreeLine("└┐", 0, 1));
                    rightTreeAdjust = 1;
                } else {
                    allTreeLines.add(new TreeLine("│", 0, 0));
                }
            } else {
                allTreeLines.add(new TreeLine("\\", 1, 1));
                rightTreeAdjust = 2;
            }
        }

        // now add joined lines of subtrees, with appropriate number of separating spaces, and adjusting offsets
        for (int i = 0; i < maxCount; i++) {
            TreeLine left, right;
            if (i >= leftTreeLines.size()) {
                // nothing remaining on left subtree
                right = rightTreeLines.get(i);
                right.leftOffset += rightTreeAdjust;
                right.rightOffset += rightTreeAdjust;
                allTreeLines.add(right);
            } else if (i >= rightTreeLines.size()) {
                // nothing remaining on right subtree
                left = leftTreeLines.get(i);
                left.leftOffset += leftTreeAdjust;
                left.rightOffset += leftTreeAdjust;
                allTreeLines.add(left);
            } else {
                left = leftTreeLines.get(i);
                right = rightTreeLines.get(i);
                int adjustedRootSpacing;
                if (rootSpacing == 1) {
                    adjustedRootSpacing = (branch == Branch.RECTANGLE) ? 1 : 3;
                } else {
                    adjustedRootSpacing = rootSpacing;
                }
                TreeLine combined = new TreeLine(
                    left.line + spaces(adjustedRootSpacing - left.rightOffset + right.leftOffset) + right.line,
                    left.leftOffset + leftTreeAdjust,
                    right.rightOffset + rightTreeAdjust
                );
                allTreeLines.add(combined);
            }
        }
        return allTreeLines;
    }

    private static int minLeftOffset(List<TreeLine> treeLines) {
        return treeLines.stream().mapToInt(e -> e.leftOffset).min().orElse(0);
    }

    private static int maxRightOffset(List<TreeLine> treeLines) {
        return treeLines.stream().mapToInt(e -> e.rightOffset).max().orElse(0);
    }

    private static String spaces(int n) {
        return Strings.repeat(" ", n);
    }

    private static class TreeLine {
        final String line;
        int leftOffset;
        int rightOffset;

        TreeLine(String line, int leftOffset, int rightOffset) {
            this.line = line;
            this.leftOffset = leftOffset;
            this.rightOffset = rightOffset;
        }
    }

}
