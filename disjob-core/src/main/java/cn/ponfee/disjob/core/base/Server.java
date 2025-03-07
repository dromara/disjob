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

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.base.Symbol.Char;
import cn.ponfee.disjob.common.util.Strings;
import lombok.Getter;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * The abstract server class definition.
 *
 * @author Ponfee
 */
@Getter
public abstract class Server implements Serializable {
    private static final long serialVersionUID = -783308216490505598L;

    /**
     * Server host
     */
    protected final String host;

    /**
     * Port number
     */
    protected final int port;

    protected Server(String host, int port) {
        Assert.isTrue(0 < port && port <= 65535, "Port must be range (0, 65535].");
        this.host = check(host);
        this.port = port;
    }

    /**
     * Extends {@code Object#hashCode()}
     *
     * @return a hash code value for this object.
     */
    @Override
    public abstract int hashCode();

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Server)) {
            return false;
        }
        Server that = (Server) obj;
        return this.host.equals(that.host)
            && this.port == that.port;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object
     * @see #serialize()
     */
    @Override
    public final String toString() {
        return serialize();
    }

    /**
     * Serialize to string
     *
     * @return string of the worker serialized result
     */
    public abstract String serialize();

    public final String buildHttpUrlPrefix() {
        return "http://" + host + ":" + port;
    }

    public final String buildHttpsUrlPrefix() {
        return "https://" + host + ":" + port;
    }

    static String check(String str) {
        if (str == null || str.isEmpty() || Strings.containsCharOrWhitespace(str, Char.COLON)) {
            throw new IllegalArgumentException("Invalid server part value: " + (str == null ? "null" : "'" + str + "'"));
        }
        return str;
    }

}
