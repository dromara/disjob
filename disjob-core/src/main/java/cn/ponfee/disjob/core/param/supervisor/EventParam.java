/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param.supervisor;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Event param
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class EventParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -4173560082801958499L;

    private Type   type;
    private String data;

    public EventParam(Type type) {
        this(type, null);
    }

    public EventParam(Type type, String data) {
        this.type = Objects.requireNonNull(type);
        this.data = data;
    }

    public enum Type {
        /**
         * Refresh group data
         */
        REFRESH_GROUP {
            @Override
            public <T> T parse(String data) {
                throw new UnsupportedOperationException();
            }
        },

        ;

        public abstract <T> T parse(String data);
    }

}
