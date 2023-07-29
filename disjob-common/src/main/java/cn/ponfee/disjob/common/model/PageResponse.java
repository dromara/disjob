/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.model;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

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
        int pageSize = request.getPageSize();
        return (int) ((total + pageSize - 1) / pageSize);
    }

}
