/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import cn.ponfee.disjob.common.util.Jsons;

/**
 * Override {@code Object#toString()} method, implemented to json string.
 *
 * @author Ponfee
 */
public abstract class ToJsonString {

    public final String toJson() {
        return Jsons.toJson(this);
    }

    @Override
    public final String toString() {
        return toJson();
    }

}
