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

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.supervisor.application.request.SchedJobAddRequest;
import cn.ponfee.disjob.supervisor.application.request.SchedJobUpdateRequest;
import cn.ponfee.disjob.supervisor.application.response.SchedGroupResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.application.response.SchedTaskResponse;
import cn.ponfee.disjob.supervisor.model.SchedGroup;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModelClassFileDiffTest
 *
 * @author Ponfee
 */
public class ModelClassFileDiffTest {

    @Test
    public void test() {
        assertThat(Objects.equals(null, null)).isTrue();

        // SchedJob ⇋ AddSchedJobRequest
        assertSame(ClassUtils.fieldDiff(SchedJob.class, SchedJobAddRequest.class), "createdAt", "createdBy", "id", "updatedAt", "updatedBy", "lastTriggerTime", "version", "jobId", "nextTriggerTime", "nextScanTime", "scanFailedCount");

        // SchedJob ⇋ UpdateSchedJobRequest
        assertSame(ClassUtils.fieldDiff(SchedJob.class, SchedJobUpdateRequest.class), "createdAt", "createdBy", "id", "updatedAt", "updatedBy", "lastTriggerTime", "nextTriggerTime", "nextScanTime", "scanFailedCount");

        // SchedJob ⇋ SchedJobResponse
        assertSame(ClassUtils.fieldDiff(SchedJob.class, SchedJobResponse.class), "id", "nextScanTime", "scanFailedCount");

        // SchedInstance ⇋ SchedInstanceResponse
        assertSame(ClassUtils.fieldDiff(SchedInstance.class, SchedInstanceResponse.class), "version", "uniqueFlag", "createdAt", "runDuration", "id", "updatedAt", "isTreeLeaf", "tasks", "nextScanTime");

        // SchedTask ⇋ SchedTaskResponse
        assertSame(ClassUtils.fieldDiff(SchedTask.class, SchedTaskResponse.class), "createdAt", "id", "executeDuration", "updatedAt", "dispatchFailedCount");

        // SchedGroup ⇋ SchedGroupResponse
        assertSame(ClassUtils.fieldDiff(SchedGroup.class, SchedGroupResponse.class), "id");
    }

    private static void assertSame(Set<String> set, String... array) {
        assertThat(set).containsOnly(array);
    }

}
