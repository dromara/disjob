/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import java.util.Base64;
import java.util.UUID;

/**
 * UUID utility
 *
 * @author Ponfee
 */
public final class UuidUtils {


    /**
     * uuid byte array
     *
     * @return
     */
    public static byte[] uuid() {
        UUID uuid = UUID.randomUUID();
        byte[] value = new byte[16];
        Bytes.put(uuid.getMostSignificantBits(), value, 0);
        Bytes.put(uuid.getLeastSignificantBits(), value, 8);
        return value;
    }

    /**
     * uuid 32 string
     *
     * @return 32 length uuid
     */
    public static String uuid32() {
        UUID uuid = UUID.randomUUID();
        return Bytes.toHex(uuid.getMostSignificantBits(), true)
            + Bytes.toHex(uuid.getLeastSignificantBits(), true);
    }

    /**
     * 22位uuid
     *
     * @return 22 length uuid
     */
    public static String uuid22() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uuid());
    }

}
