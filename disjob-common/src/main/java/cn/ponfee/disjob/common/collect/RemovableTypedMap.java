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
 * Removable typed for {@link Map}
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Ponfee
 */
public interface RemovableTypedMap<K, V> extends Map<K, V>, RemovableTypedKeyValue<K, V> {

    @Override
    default V getValue(K key) {
        return this.get(key);
    }

    @Override
    default V removeKey(K key) {
        return this.remove(key);
    }

}
