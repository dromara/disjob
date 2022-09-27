package cn.ponfee.scheduler.core.model;

import cn.ponfee.scheduler.common.date.DatePeriods;
import cn.ponfee.scheduler.core.enums.TriggerType;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Period trigger config data structure.
 * <p>trigger_conf of trigger_type=PERIOD
 *
 * @author Ponfee
 * @see TriggerType#PERIOD
 */
@Data
public class PeriodTriggerConf implements Serializable {

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

    public boolean isValid() {
        return period != null && start != null && step > 0;
    }

}
