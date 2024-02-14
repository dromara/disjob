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

package cn.ponfee.disjob;

/**
 * Test suite for supervisor
 *
 * @author Ponfee
 */

/*
@org.junit.runner.RunWith(org.junit.runners.Suite.class)
@org.junit.runners.Suite.SuiteClasses({
    cn.ponfee.disjob.supervisor.util.CommonTest.class,
    cn.ponfee.disjob.supervisor.transaction.JobTxManagerTest.class
})
*/

/*
@org.junit.runner.RunWith(org.junit.platform.runner.JUnitPlatform.class)
@org.junit.platform.suite.api.SelectPackages("cn.ponfee.disjob.supervisor")
*/

@org.junit.platform.suite.api.Suite
@org.junit.platform.suite.api.SelectPackages("cn.ponfee.disjob.supervisor")
@org.junit.platform.suite.api.SuiteDisplayName("Disjob supervisor test suite")
public class AllTestSuite {

}
