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

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.beans.Transient;
import java.io.Serializable;
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
public class PageResponse<T> extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 3175875483341043538L;

    /**
     * Current page records
     */
    private List<T> rows;

    /**
     * Total of result records
     */
    private long total;

    /**
     * Page request
     */
    private PageRequest request;

    @Transient
    public int getTotalPages() {
        return computeTotalPages(request.getPageSize(), total);
    }

    public boolean hasPrevious() {
        return request.isPaged() && request.getPageNumber() > 1 && getTotalPages() > 1;
    }

    public boolean hasNext() {
        return request.isPaged() && request.getPageNumber() < getTotalPages();
    }

    @Transient
    public boolean isFirst() {
        return !hasPrevious();
    }

    @Transient
    public boolean isLast() {
        return !hasNext();
    }

    public void forEachRow(Consumer<T> action) {
        if (rows != null) {
            rows.forEach(action);
        }
    }

    // ------------------------------------------------------------static methods

    public static int computeTotalPages(int pageSize, long total) {
        return (int) ((total + pageSize - 1) / pageSize);
    }

    public static <T> PageResponse<T> empty() {
        return of(Collections.emptyList(), 0, null);
    }

    public static <T> PageResponse<T> of(List<T> rows, long total) {
        return of(rows, total, null);
    }

    public static <T> PageResponse<T> of(List<T> rows, long total, PageRequest request) {
        PageResponse<T> response = new PageResponse<>();
        response.setRows(rows);
        response.setTotal(total);
        response.setRequest(request);
        return response;
    }

}
