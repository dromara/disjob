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

package cn.ponfee.disjob.registry;

import java.util.function.Consumer;

/**
 * Connection state changed listener
 *
 * @param <C> the client
 * @author Ponfee
 */
public interface ConnectionStateListener<C> {

    /**
     * Connected
     *
     * @param client the client
     */
    void onConnected(C client);

    /**
     * Disconnected
     *
     * @param client the client
     */
    void onDisconnected(C client);

    static <C> Builder<C> builder() {
        return new Builder<>();
    }

    final class Builder<C> {
        private Consumer<C> onConnected;
        private Consumer<C> onDisconnected;

        private Builder() {
        }

        public Builder<C> onConnected(Consumer<C> onConnected) {
            this.onConnected = onConnected;
            return this;
        }

        public Builder<C> onDisconnected(Consumer<C> onDisconnected) {
            this.onDisconnected = onDisconnected;
            return this;
        }

        public ConnectionStateListener<C> build() {
            return new ConnectionStateListener<C>() {
                @Override
                public void onConnected(C client) {
                    if (onConnected != null) {
                        onConnected.accept(client);
                    }
                }

                @Override
                public void onDisconnected(C client) {
                    if (onDisconnected != null) {
                        onDisconnected.accept(client);
                    }
                }
            };
        }
    }

}
