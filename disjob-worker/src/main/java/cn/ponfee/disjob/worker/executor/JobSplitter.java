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

package cn.ponfee.disjob.worker.executor;

import cn.ponfee.disjob.core.exception.JobException;

import java.util.List;

/**
 * Split schedule job to one instance and many tasks.
 *
 * @author Ponfee
 */
interface JobSplitter<T extends SplitParam> {

    /**
     * Verifies job param
     *
     * @param jobParam the job param
     * @return {@code true} if verified success
     */
    default boolean verify(String jobParam) {
        return true;
    }

    /**
     * Provides default split single task.
     * <p>Subclass can override this method to customize implementation.
     *
     * @param splitParam the split param
     * @return list of task param
     * @throws JobException if split failed
     */
    List<String> split(T splitParam) throws JobException;

}
