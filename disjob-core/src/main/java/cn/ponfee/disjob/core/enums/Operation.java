/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.core.enums;

import static cn.ponfee.disjob.core.enums.ExecuteState.*;

/**
 * Task operate type
 *
 * @author Ponfee
 */
public enum Operation {

    /**
     * Trigger from WAITING to EXECUTING
     */
    TRIGGER(WAITING, EXECUTING),

    /**
     * Pause from EXECUTING to PAUSED
     */
    PAUSE(EXECUTING, PAUSED),

    /**
     * Exception cancel from EXECUTING to EXECUTE_EXCEPTION
     */
    EXCEPTION_CANCEL(EXECUTING, EXECUTE_EXCEPTION),

    /**
     * Collided cancel from EXECUTING to EXECUTE_COLLIDED
     */
    COLLIDED_CANCEL(EXECUTING, EXECUTE_COLLIDED),

    /**
     * Manual cancel from EXECUTING to MANUAL_CANCELED
     */
    MANUAL_CANCEL(EXECUTING, MANUAL_CANCELED),

    ;

    private final ExecuteState fromState;
    private final ExecuteState toState;

    Operation(ExecuteState fromState, ExecuteState toState) {
        this.fromState = fromState;
        this.toState = toState;
    }

    public ExecuteState fromState() {
        return fromState;
    }

    public ExecuteState toState() {
        return toState;
    }

    public boolean isTrigger() {
        return this == TRIGGER;
    }

    public boolean isNotTrigger() {
        return !isTrigger();
    }
}
