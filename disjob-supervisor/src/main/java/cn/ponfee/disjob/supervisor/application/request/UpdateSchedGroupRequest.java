/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.model.SchedGroup;
import cn.ponfee.disjob.supervisor.application.converter.SchedGroupConverter;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Update sched group request parameter structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class UpdateSchedGroupRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7531416191031943146L;

    private String group;
    private String ownUser;
    private String devUsers;
    private String alarmUsers;
    private String webHook;
    private String updatedBy;
    private int version;

    public SchedGroup toSchedGroup() {
        return SchedGroupConverter.INSTANCE.convert(this);
    }

    public void checkAndTrim() {
        Assert.hasText(ownUser, "Own user cannot be blank.");
        Assert.hasText(updatedBy, "Updated by cannot be blank.");
        this.ownUser = StringUtils.trim(ownUser);
        this.devUsers = prune(devUsers);
        this.alarmUsers = prune(alarmUsers);
        this.webHook = StringUtils.trim(webHook);
    }

    private static String prune(String users) {
        if (users == null) {
            return null;
        }

        return Stream.of(users.split(Str.COMMA))
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .collect(Collectors.joining(Str.COMMA));
    }

}
