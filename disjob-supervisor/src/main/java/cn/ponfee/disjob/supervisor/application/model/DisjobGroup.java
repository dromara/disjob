/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.model;

import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.core.model.SchedGroupUser;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
                        String ownUser, Set<String> alarmUsers, Set<String> devUsers,
                        String webHook) {
        this.supervisorToken = supervisorToken;
        this.workerToken = workerToken;
        this.userToken = userToken;
        this.ownUser = ownUser;
        this.devUsers = devUsers;
        this.alarmUsers = alarmUsers;
        this.webHook = webHook;
    }

    public static DisjobGroup of(SchedGroup schedGroup, List<SchedGroupUser> devUsers) {
        return new DisjobGroup(
            schedGroup.getSupervisorToken(),
            schedGroup.getWorkerToken(),
            schedGroup.getUserToken(),
            schedGroup.getOwnUser(),
            parse(schedGroup.getAlarmUsers()),
            toSet(devUsers, SchedGroupUser::getUser),
            schedGroup.getWebHook()
        );
    }

    public boolean isDeveloper(String user) {
        return devUsers.contains(user);
    }

    // --------------------------------------------------------------private methods

    private static Set<String> parse(String str) {
        return StringUtils.isBlank(str) ? Collections.emptySet() : ImmutableSet.copyOf(str.split(","));
    }

    private static <S, T> Set<T> toSet(List<S> source, Function<S, T> mapper) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptySet();
        }
        ImmutableSet.Builder<T> builder = ImmutableSet.builderWithExpectedSize(source.size() << 1);
        source.stream().map(mapper).forEach(builder::add);
        return builder.build();
    }
}
