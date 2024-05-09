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

package cn.ponfee.disjob.common.util;

import org.apache.commons.io.HexDump;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * MavenProjects Test
 *
 * @author Ponfee
 */
@Disabled
public class MavenProjectsTest {

    @Test
    public void testClass() throws IOException {
        byte[] mainClassFileAsBytes = MavenProjects.getMainClassFileAsBytes(MavenProjects.class);
        HexDump.dump(mainClassFileAsBytes, System.out);
        System.out.print("\n\n---------------------------------------------------\n\n");

        byte[] testClassFileAsBytes = MavenProjects.getTestClassFileAsBytes(MavenProjectsTest.class);
        HexDump.dump(testClassFileAsBytes, System.out);
    }
}
