/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.collect;

import java.util.Map;

/**
 * Get the value with typed for {@link Map}
 * 
 * @author Ponfee
 * @param <K> the key type
 * @param <V> the value type
 */
public interface TypedMap<K, V> extends Map<K, V>, TypedKeyValue<K, V> {

    @Override
    default V getValue(K key) {
        return this.get(key);
    }

}
