/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.model;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.collect.RemovableTypedKeyValue;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Page request for pageable query
 *
 * @author Ponfee
 */
@Getter
@Setter
public class PageRequest extends ToJsonString implements RemovableTypedKeyValue<String, Object>, Serializable {
    private static final long serialVersionUID = 2032344850017264330L;

    /**
     * Is whether page query.
     */
    private boolean paged = true;

    /**
     * Page number, start with 1.
     */
    private int pageNumber;

    /**
     * Page size, cannot less than 0.
     */
    private int pageSize;

    /**
     * Sort string, for example: "updated_at desc, name asc"
     */
    private String sort;

    /**
     * Parameter of query condition
     */
    private Map<String, Object> params;

    @Override
    public Object getValue(String key) {
        return params.get(key);
    }

    @Override
    public Object removeKey(String key) {
        return params.remove(key);
    }

    public long getOffset() {
        return (long) (pageNumber - 1) * pageSize;
    }

    public <P extends PageRequest, A> PageResponse<A> query(ToLongFunction<P> queryCount,
                                                            Function<P, List<A>> queryRecord) {
        return query(queryCount, queryRecord, null);
    }

    public <P extends PageRequest, A, B> PageResponse<B> query(ToLongFunction<P> queryCount,
                                                               Function<P, List<A>> queryRecords,
                                                               Function<A, B> mapper) {
        check();
        P this0 = (P) this;
        long total;
        List<A> list;
        if (isPaged()) {
            total = queryCount.applyAsLong(this0);
            correctPageNumber(total);
            list = (total == 0) ? Collections.emptyList() : queryRecords.apply(this0);
        } else {
            list = queryRecords.apply(this0);
            total = list.size();
        }
        List<B> rows = (mapper == null) ? (List<B>) list : Collects.convert(list, mapper);
        return new PageResponse<>(rows, total, this0);
    }

    // ------------------------------------------------------------------------private methods

    private void check() {
        int minPageSize = paged ? 1 : 0;
        if (pageSize < minPageSize) {
            throw new IllegalArgumentException("Invalid page size value [" + pageSize + "].");
        }
    }

    private void correctPageNumber(long total) {
        if (pageNumber < 1 || total == 0) {
            this.pageNumber = 1;
        } else {
            this.pageNumber = Math.min(pageNumber, PageResponse.computeTotalPages(pageSize, total));
        }
    }

}
