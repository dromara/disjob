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

import cn.ponfee.disjob.core.enums.TokenType;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.Base64;

import static cn.ponfee.disjob.common.base.Symbol.Str.DOT;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Token utilities
 *
 * @author Ponfee
 */
public class Tokens {

    private static final long EXPIRATION_MILLISECONDS = 60_000L;

    public static String createAuthentication(String tokenKey, TokenType type, String group) {
        return create(tokenKey, type, Mode.authentication, group);
    }

    public static String createSignature(String tokenKey, TokenType type, String group) {
        return create(tokenKey, type, Mode.signature, group);
    }

    public static boolean verifyAuthentication(String tokenSecret, String tokenKey, TokenType type, String group) {
        return verify(tokenSecret, tokenKey, type, Mode.authentication, group);
    }

    public static boolean verifySignature(String tokenSecret, String tokenKey, TokenType type, String group) {
        return verify(tokenSecret, tokenKey, type, Mode.signature, group);
    }

    // -----------------------------------------------------------------private methods

    private static String create(String tokenKey, TokenType type, Mode mode, String group) {
        if (StringUtils.isEmpty(tokenKey)) {
            return null;
        }
        String expiration = Long.toString(System.currentTimeMillis() + EXPIRATION_MILLISECONDS);
        return secret(tokenKey, type, mode, expiration, group) + DOT + expiration;
    }

    private static boolean verify(String tokenSecret, String tokenKey, TokenType type, Mode mode, String group) {
        if (StringUtils.isEmpty(tokenKey)) {
            return true;
        }
        if (StringUtils.isEmpty(tokenSecret)) {
            return false;
        }
        String[] array = tokenSecret.split("\\.");
        if (array.length != 2) {
            return false;
        }

        String actual = array[0];
        String expiration = array[1];
        if (Long.parseLong(expiration) < System.currentTimeMillis()) {
            return false;
        }

        String expect = secret(tokenKey, type, mode, expiration, group);
        return actual.equals(expect);
    }

    private static String secret(String tokenKey, TokenType type, Mode mode, String expiration, String group) {
        Assert.notNull(type, "Type cannot be null.");
        Assert.notNull(mode, "Mode cannot be null.");
        Assert.hasText(group, "Group cannot be empty.");
        String payload = type.name() + DOT + mode.name() + DOT + expiration + DOT + group;

        HmacUtils hm = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, tokenKey.getBytes(UTF_8));
        byte[] digest = hm.hmac(payload.getBytes(UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private enum Mode {
        /**
         * For authentication
         */
        authentication,
        /**
         * For signature
         */
        signature,
    }

}
