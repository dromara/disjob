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

package cn.ponfee.disjob.common.model;

import lombok.Getter;
import lombok.Setter;

import java.beans.Transient;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Page query response
 *
 * @author Ponfee
 */
@Getter
@Setter
public class PageResponse<T> extends Page {
    private static final long serialVersionUID = 3175875483341043538L;

    /**
     * Total available records.
     */
    private long total;

    /**
     * Current page records.
     */
    private List<T> records = Collections.emptyList();

    @Transient
    public int getTotalPages() {
        return computeTotalPages(pageSize, total);
    }

    @Transient
    public boolean isFirst() {
        return !hasPrevious();
    }

    @Transient
    public boolean isLast() {
        return !hasNext();
    }

    public boolean hasPrevious() {
        return pageNumber > 1 && getTotalPages() > 1;
    }

    public boolean hasNext() {
        return pageNumber < getTotalPages();
    }

    public void forEachRecord(Consumer<T> action) {
        if (records != null) {
            records.forEach(action);
        }
    }

}
