/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.model;

import cn.ponfee.disjob.common.base.RemovableTypedKeyValue;
import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

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
     * Sort string, for example: "last_update_time desc, name"
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

}
