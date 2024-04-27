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

package cn.ponfee.disjob.common.base;

/**
 * Initialize resources
 *
 * @author Ponfee
 */
@FunctionalInterface
public interface Initializable {

    NoArgMethodInvoker DEFAULT = new NoArgMethodInvoker("init", "initialize", "open", "start");

    /**
     * Initialize resources
     */
    void init();

    /**
     * Initialize target resources
     *
     * @param target the target object
     */
    static void init(Object target) {
        if (target == null) {
            return;
        }

        if (target instanceof Startable) {
            ((Startable) target).start();
        } else if (target instanceof Initializable) {
            ((Initializable) target).init();
        } else {
            DEFAULT.invoke(target);
        }
    }

}
