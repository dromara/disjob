/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param;

import cn.ponfee.disjob.common.base.RemovableTypedKeyValue;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Page parameter for pageable query
 *
 * @author Ponfee
 */
public class PageParameter extends PageRequest implements RemovableTypedKeyValue<String, Object> {
    private static final long serialVersionUID = -63543088495670255L;

    private Map<String, Object> params;

    public PageParameter(int page, int size, Sort sort) {
        super(page, size, sort);
    }

    @Override
    public Object getValue(String key) {
        return params.get(key);
    }

    @Override
    public Object removeKey(String key) {
        return params.remove(key);
    }

}
