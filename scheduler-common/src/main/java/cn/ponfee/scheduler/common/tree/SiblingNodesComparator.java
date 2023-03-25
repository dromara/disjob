/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.tree;

import cn.ponfee.scheduler.common.util.Comparators;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Sibling nodes comparator
 *
 * @author Ponfee
 */
public class SiblingNodesComparator<T extends Serializable & Comparable<? super T>, A> {

    private final Comparator<TreeNode<T, A>> comparator;

    private SiblingNodesComparator(Comparator<TreeNode<T, A>> comparator) {
        this.comparator = comparator;
    }

    public static <T extends Serializable & Comparable<? super T>, A, U extends Comparable<? super U>> SiblingNodesComparator<T, A> comparing(Function<TreeNode<T, A>, U> first) {
        // default nullsLast and ASC
        return comparing(first, false, true);
    }

    public static <T extends Serializable & Comparable<? super T>, A, U extends Comparable<? super U>> SiblingNodesComparator<T, A> comparing(Function<TreeNode<T, A>, U> first, boolean nullsFirst, boolean asc) {
        return new SiblingNodesComparator<>(Comparator.comparing(first, comparator(nullsFirst, asc)));
    }

    public <U extends Comparable<? super U>> SiblingNodesComparator<T, A> thenComparing(Function<TreeNode<T, A>, U> then) {
        // default nullsLast and ASC
        return thenComparing(then, false, true);
    }

    public <U extends Comparable<? super U>> SiblingNodesComparator<T, A> thenComparing(Function<TreeNode<T, A>, U> then, boolean nullsFirst, boolean asc) {
        return new SiblingNodesComparator<>(comparator.thenComparing(then, comparator(nullsFirst, asc)));
    }

    public void sort(TreeNode<T, A> root) {
        sort(root.getChildren());
    }

    public void sort(List<TreeNode<T, A>> children) {
        if (CollectionUtils.isNotEmpty(children)) {
            children.sort(comparator);
        }
    }

    public Comparator get() {
        return comparator;
    }

    private static <U extends Comparable<? super U>> Comparator<U> comparator(boolean nullsFirst, boolean asc) {
        Comparator<U> comparator = Comparators.order(asc);
        return nullsFirst ? Comparator.nullsFirst(comparator) : Comparator.nullsLast(comparator);
    }

}
