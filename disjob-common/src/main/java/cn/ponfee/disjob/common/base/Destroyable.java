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
 * Destroy resources
 *
 * @author Ponfee
 */
@FunctionalInterface
public interface Destroyable {

    NoArgMethodInvoker DEFAULT = new NoArgMethodInvoker("destroy", "close", "release", "stop");

    /**
     * Destroy resources
     */
    void destroy();

    /**
     * Destroy target resources
     *
     * @param target the target object
     * @throws Exception if occur error
     */
    static void destroy(Object target) throws Exception {
        if (target == null) {
            return;
        }

        if (target instanceof Startable) {
            ((Startable) target).stop();
        } else if (target instanceof AutoCloseable) {
            ((AutoCloseable) target).close();
        } else if (target instanceof Destroyable) {
            Destroyable destroyable = (Destroyable) target;
            if (!destroyable.isDestroyed()) {
                ((Destroyable) target).destroy();
            }
        } else {
            DEFAULT.invoke(target);
        }
    }

    /**
     * Returns is whether destroyed.
     *
     * @return {@code true} destroyed
     */
    default boolean isDestroyed() {
        return false;
    }

}
