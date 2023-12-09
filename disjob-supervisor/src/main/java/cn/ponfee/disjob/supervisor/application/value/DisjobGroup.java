/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.value;

import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.core.model.SchedGroup;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Set;

/**
 * Disjob group
 *
 * @author Ponfee
 */
@Getter
public class DisjobGroup {

    private final String supervisorToken;
    private final String workerToken;
    private final String userToken;
    private final String ownUser;
    private final Set<String> alarmUsers;
    private final Set<String> devUsers;
    private final String webHook;

    private DisjobGroup(String supervisorToken, String workerToken, String userToken,
                        String ownUser, Set<String> devUsers, Set<String> alarmUsers,
                        String webHook) {
        this.supervisorToken = supervisorToken;
        this.workerToken = workerToken;
        this.userToken = userToken;
        this.ownUser = ownUser;
        this.devUsers = devUsers;
        this.alarmUsers = alarmUsers;
        this.webHook = webHook;
    }

    public static DisjobGroup of(SchedGroup schedGroup) {
        return new DisjobGroup(
            schedGroup.getSupervisorToken(),
            schedGroup.getWorkerToken(),
            schedGroup.getUserToken(),
            schedGroup.getOwnUser(),
            parse(schedGroup.getDevUsers()),
            parse(schedGroup.getAlarmUsers()),
            schedGroup.getWebHook()
        );
    }

    public boolean isDeveloper(String user) {
        return devUsers.contains(user);
    }

    // --------------------------------------------------------------private methods

    private static Set<String> parse(String str) {
        return StringUtils.isBlank(str) ? Collections.emptySet() : ImmutableSet.copyOf(str.split(Str.COMMA));
    }

}
