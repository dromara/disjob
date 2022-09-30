package cn.ponfee.scheduler.common.base;

import cn.ponfee.scheduler.common.util.Jsons;

/**
 * Override {@code Object#toString()} method, implemented to json string.
 *
 * @author Ponfee
 */
public abstract class ToJsonString {

    @Override
    public String toString() {
        return Jsons.toJson(this);
    }
}
