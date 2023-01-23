/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.model;

import cn.ponfee.scheduler.common.date.DatePeriods;
import cn.ponfee.scheduler.core.enums.TriggerType;
import lombok.Data;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Date;

/**
 * Period trigger value data structure.
 * <p>trigger_value of trigger_type=PERIOD
 *
 * @author Ponfee
 * @see TriggerType#PERIOD
 */
@Data
public class PeriodTriggerValue implements Serializable {

    private static final long serialVersionUID = -8395535372974631095L;

    /**
     * Period type
     */
    private DatePeriods period;

    /**
     * Calculate start date time
     */
    private Date start;

    /**
     * period step
     */
    private int step = 1;

    @Transient
    public boolean isValid() {
        return period != null && start != null && step > 0;
    }

}
