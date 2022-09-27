package cn.ponfee.scheduler.core.model;

import cn.ponfee.scheduler.common.base.model.BaseEntity;
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

    public SchedDepend(Long parentJobId, Long childJobId) {
        this.parentJobId = parentJobId;
        this.childJobId = childJobId;
    }
}
