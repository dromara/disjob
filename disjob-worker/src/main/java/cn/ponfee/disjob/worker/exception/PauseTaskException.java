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

package cn.ponfee.disjob.worker.exception;

/**
 * Executing task failure then should be pausing.
 *
 * @author Ponfee
 */
public class PauseTaskException extends Exception {
    private static final long serialVersionUID = 409247238969878885L;

    public PauseTaskException() {
        super("Pause task.");
    }

    public PauseTaskException(String message) {
        super(message);
    }

    public PauseTaskException(String message, Throwable cause) {
        super(message, cause);
    }

}