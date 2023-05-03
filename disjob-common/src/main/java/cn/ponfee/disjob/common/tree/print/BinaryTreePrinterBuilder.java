/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.tree.print;

import java.util.function.Function;

/**
 * Binary tree printer builder
 *
 * @author Ponfee
 */
public class BinaryTreePrinterBuilder<T> {

    private final Appendable output;
    private final Function<T, String> nodeLabel;
    private final Function<T, T> leftChild;
    private final Function<T, T> rightChild;

    private BinaryTreePrinter.Branch branch = BinaryTreePrinter.Branch.RECTANGLE;
    private boolean directed = true;
    private int nodeSpace = 2;
    private int treeSpace = 5;

    public BinaryTreePrinterBuilder(Function<T, String> nodeLabel,
                                    Function<T, T> leftChild,
                                    Function<T, T> rightChild) {
        this(System.out, nodeLabel, leftChild, rightChild);
    }

    public BinaryTreePrinterBuilder(Appendable output,
                                    Function<T, String> nodeLabel,
                                    Function<T, T> leftChild,
                                    Function<T, T> rightChild) {
        this.output = output;
        this.nodeLabel = nodeLabel;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    public BinaryTreePrinterBuilder<T> branch(BinaryTreePrinter.Branch branch) {
        this.branch = branch;
        return this;
    }

    public BinaryTreePrinterBuilder<T> directed(boolean directed) {
        this.directed = directed;
        return this;
    }

    public BinaryTreePrinterBuilder<T> nodeSpace(int nodeSpace) {
        this.nodeSpace = nodeSpace;
        return this;
    }

    public BinaryTreePrinterBuilder<T> treeSpace(int treeSpace) {
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
