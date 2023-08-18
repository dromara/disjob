/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.model;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.common.util.Collects;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

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

    public PageResponse() {
    }

    public PageResponse(List<T> rows, long total) {
        this(rows, total, null);
    }

    public PageResponse(List<T> rows, long total, PageRequest request) {
        this.rows = rows;
        this.total = total;
        this.request = request;
    }

    public int getTotalPages() {
        return computeTotalPages(request.getPageSize(), total);
    }

    public static int computeTotalPages(int pageSize, long total) {
        return (int) ((total + pageSize - 1) / pageSize);
    }

    public static <P extends PageRequest, A> PageResponse<A> query(P pageRequest,
                                                                   ToLongFunction<P> queryCount,
                                                                   Function<P, List<A>> queryRecord) {
        return query(pageRequest, queryCount, queryRecord, null);
    }

    public static <P extends PageRequest, A, B> PageResponse<B> query(P pageRequest,
                                                                      ToLongFunction<P> queryCount,
                                                                      Function<P, List<A>> queryRecords,
                                                                      Function<A, B> mapper) {
        pageRequest.check();
        long total;
        List<A> list;
        if (pageRequest.isPaged()) {
            total = queryCount.applyAsLong(pageRequest);
            pageRequest.adjustPageNumber(total);
            list = (total == 0) ? Collections.emptyList() : queryRecords.apply(pageRequest);
        } else {
            list = queryRecords.apply(pageRequest);
            total = list.size();
        }
        List<B> rows = (mapper == null) ? (List<B>) list : Collects.convert(list, mapper);
        return new PageResponse<>(rows, total, pageRequest);
    }

}
