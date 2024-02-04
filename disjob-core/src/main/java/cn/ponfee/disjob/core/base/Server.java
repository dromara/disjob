/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.base.Symbol.Str;
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
        Assert.isTrue(!host.contains(Str.COLON), "Host cannot contains symbol ':'");
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Server)) {
            return false;
        }
        return sameServer((Server) o);
    }

    public boolean sameServer(Server other) {
        if (other == null) {
            return false;
        }
        return this.host.equals(other.host)
            && this.port == other.port;
    }

    public final String buildHttpUrlPrefix() {
        return String.format("http://%s:%d", host, port);
    }

    public final String buildHttpsUrlPrefix() {
        return String.format("https://%s:%d", host, port);
    }

    /**
     * Extends {@code Object#hashCode()}
     *
     * @return a hash code value for this object.
     */
    @Override
    public abstract int hashCode();

    /**
     * Serialize to string
     *
     * @return string of the worker serialized result
     */
    public abstract String serialize();

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

}
