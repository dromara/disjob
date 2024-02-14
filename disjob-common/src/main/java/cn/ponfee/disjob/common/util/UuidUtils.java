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

import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;

import java.util.Base64;
import java.util.UUID;

/**
 * UUID utility
 *
 * @author Ponfee
 */
public final class UuidUtils {

    private static final IdGenerator UUID_GENERATOR = new AlternativeJdkIdGenerator();

    /**
     * Returns 16 length byte array uuid
     *
     * @return 16 length uuid byte array
     */
    public static byte[] uuid() {
        UUID uuid = UUID_GENERATOR.generateId();
        byte[] value = new byte[16];
        Bytes.put(uuid.getMostSignificantBits(), value, 0);
        Bytes.put(uuid.getLeastSignificantBits(), value, 8);
        return value;
    }

    /**
     * Returns 32 length string uuid, use hex encoding
     *
     * @return 32 length uuid string
     */
    public static String uuid32() {
        UUID uuid = UUID_GENERATOR.generateId();
        return Bytes.toHex(uuid.getMostSignificantBits(),  true)
             + Bytes.toHex(uuid.getLeastSignificantBits(), true);
    }

    /**
     * Returns 22 length string uuid, use base64 url encoding and without padding
     *
     * @return 22 length uuid string
     */
    public static String uuid22() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uuid());
    }

}
