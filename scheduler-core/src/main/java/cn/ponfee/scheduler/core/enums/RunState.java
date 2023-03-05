/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.base.IntValue;
import cn.ponfee.scheduler.common.util.Enums;
import com.google.common.collect.ImmutableList;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * The run state enum definition.
 * <p>mapped by sched_instance.run_state
 *
 * @author Ponfee
 */
public enum RunState implements IntValue<RunState> {

    /**
     * 待运行
     */
    WAITING(10, false, false),

    /**
     * 运行中
     */
    RUNNING(20, false, false),

    /**
     * 已暂停
     */
    PAUSED(30, false, false),

    /**
     * 已完成
     */
    FINISHED(40, true, false),

    /**
     * 已取消
     */
    CANCELED(50, true, true),

    ;

    /**
     * State list of can transit to CANCELED
     *
     * @see #isTerminal()
     */
    public static final List<RunState> CANCELABLE_LIST = ImmutableList.of(WAITING, RUNNING, PAUSED);

    /**
     * State list of can transit to PAUSED
     */
    public static final List<RunState> PAUSABLE_LIST = ImmutableList.of(WAITING, RUNNING);

    /**
     * State list of can transit to EXECUTING
     */
    public static final List<RunState> EXECUTABLE_LIST = ImmutableList.of(WAITING, PAUSED);

    private static final Map<Integer, RunState> MAPPING = Enums.toMap(RunState.class, RunState::value);

    /**
     * state value
     */
    private final int value;
    /**
     * is terminal state
     */
    private final boolean terminal;
    /**
     * is failure state
     */
    private final boolean failure;

    RunState(int value, boolean terminal, boolean failure) {
        this.value = value;
        this.terminal = terminal;
        this.failure = failure;
    }

    @Override
    public int value() {
        return value;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isFailure() {
        return failure;
    }

    public static RunState of(Integer value) {
        RunState runState = MAPPING.get(value);
        Assert.notNull(runState, () -> "Invalid run state value: " + value);
        return runState;
    }

}
