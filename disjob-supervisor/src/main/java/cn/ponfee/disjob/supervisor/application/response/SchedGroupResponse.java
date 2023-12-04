/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.response;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Sched group response
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedGroupResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -8381578632306318642L;

    private String group;
    private String supervisorToken;
    private String workerToken;
    private String userToken;
    private String ownUser;
    private String alarmUsers;
    private String devUsers;
    private String webHook;

    private Integer version;
    private Date updatedAt;
    private Date createdAt;
    private String updatedBy;
    private String createdBy;
}
