/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Split task structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class SplitTask implements java.io.Serializable {
    private static final long serialVersionUID = 5200874217689134007L;

    private String taskParam;

    public SplitTask(String taskParam) {
        this.taskParam = taskParam;
    }

    @Override
    public String toString() {
        return taskParam;
    }

}
