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

import cn.ponfee.disjob.common.util.Comparators;
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
public class SiblingNodesComparator<T extends Serializable & Comparable<T>, A> {

    private final Comparator<TreeNode<T, A>> comparator;

    private SiblingNodesComparator(Comparator<TreeNode<T, A>> comparator) {
        this.comparator = comparator;
    }

    public static <T extends Serializable & Comparable<T>, A, U extends Comparable<? super U>> SiblingNodesComparator<T, A> comparing(Function<TreeNode<T, A>, U> first) {
        // default nullsLast and ASC
        return comparing(first, false, true);
    }

    public static <T extends Serializable & Comparable<T>, A, U extends Comparable<? super U>> SiblingNodesComparator<T, A> comparing(Function<TreeNode<T, A>, U> first, boolean nullsFirst, boolean asc) {
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
