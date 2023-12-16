/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Search job request
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SearchJobRequest {
    private static final long serialVersionUID = -538371009995926914L;

    private Set<String> groups;
    private String jobName;
    private Long jobId;

}
