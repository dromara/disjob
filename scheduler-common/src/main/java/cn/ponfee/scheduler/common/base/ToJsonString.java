package cn.ponfee.scheduler.common.base;

import com.alibaba.fastjson.JSON;

/**
 * Override {@code Object#toString()} method, implemented to json string.
 *
 * @author Ponfee
 */
public abstract class ToJsonString {

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
