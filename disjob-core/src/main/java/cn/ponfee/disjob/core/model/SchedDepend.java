/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.model;

import cn.ponfee.disjob.common.model.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * The schedule job entity, mapped database table sched_depend
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class SchedDepend extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 8880747435878186418L;

    /**
     * 父job_id
     */
    private Long parentJobId;

    /**
     * 子job_id
     */
    private Long childJobId;

    /**
     * 序号(从1开始)
     */
    private Integer sequence;

    public SchedDepend(Long parentJobId, Long childJobId, Integer sequence) {
        this.parentJobId = parentJobId;
        this.childJobId = childJobId;
        this.sequence = sequence;
    }
}
