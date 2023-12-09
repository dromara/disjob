/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.model.PageRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Sched group page request
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedGroupPageRequest extends PageRequest {
    private static final long serialVersionUID = -213388921649759103L;

    private Set<String> groups;

}
