/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.value;

import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.model.SchedGroup;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
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
    private final String workerContextPath;
    private final String webHook;

    private DisjobGroup(String supervisorToken, String workerToken, String userToken,
                        String ownUser, Set<String> devUsers, Set<String> alarmUsers,
                        String workerContextPath, String webHook) {
        this.supervisorToken = supervisorToken;
        this.workerToken = workerToken;
        this.userToken = userToken;
        this.ownUser = ownUser;
        this.devUsers = devUsers;
        this.alarmUsers = alarmUsers;
        this.workerContextPath = workerContextPath;
        this.webHook = webHook;
    }

    public static DisjobGroup of(SchedGroup schedGroup) {
        String ownUser = schedGroup.getOwnUser();
        return new DisjobGroup(
            schedGroup.getSupervisorToken(),
            schedGroup.getWorkerToken(),
            schedGroup.getUserToken(),
            ownUser,
            parse(schedGroup.getDevUsers(), ownUser),
            parse(schedGroup.getAlarmUsers(), ownUser),
            Strings.trimUrlPath(schedGroup.getWorkerContextPath()),
            schedGroup.getWebHook()
        );
    }

    public boolean isDeveloper(String user) {
        return devUsers.contains(user);
    }

    // --------------------------------------------------------------private methods

    private static Set<String> parse(String str, String ownUser) {
        if (StringUtils.isBlank(str)) {
            return Collections.singleton(ownUser);
        }

        String[] array = str.split(Str.COMMA);
        ImmutableSet.Builder<String> builder = ImmutableSet.builderWithExpectedSize(array.length + 1);
        Arrays.stream(array).filter(StringUtils::isNotBlank).map(String::trim).forEach(builder::add);
        return builder.add(ownUser).build();
    }

}
