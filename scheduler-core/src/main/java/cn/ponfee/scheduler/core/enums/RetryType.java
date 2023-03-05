/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.base.IntValue;
import cn.ponfee.scheduler.common.util.Enums;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * The retry type enum definition.
 * <p>mapped by sched_job.retry_type
 *
 * @author Ponfee
 */
public enum RetryType implements IntValue<RetryType> {

    /**
     * 不重试
     */
    NONE(0),

    /**
     * 重试所有的Task(re-split job param to task param)
     */
    ALL(1),

    /**
     * 只重试失败的Task(copy previous failed task param)
     */
    FAILED(2),

    ;

    private static final Map<Integer, RetryType> MAPPING = Enums.toMap(RetryType.class, RetryType::value);

    private final int value;

    RetryType(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    public static RetryType of(Integer value) {
        RetryType runType = MAPPING.get(value);
        Assert.notNull(runType, () -> "Invalid retry type value: " + value);
        return runType;
    }

}
