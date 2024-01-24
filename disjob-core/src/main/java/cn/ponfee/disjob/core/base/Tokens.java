/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.core.model.TokenType;
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

    public static String createAuthentication(String tokenPlain, TokenType type, String group) {
        return create(tokenPlain, type, Mode.authentication, group);
    }

    public static String createSignature(String tokenPlain, TokenType type, String group) {
        return create(tokenPlain, type, Mode.signature, group);
    }

    public static boolean verifyAuthentication(String tokenSecret, String tokenPlain, TokenType type, String group) {
        return verify(tokenSecret, tokenPlain, type, Mode.authentication, group);
    }

    public static boolean verifySignature(String tokenSecret, String tokenPlain, TokenType type, String group) {
        return verify(tokenSecret, tokenPlain, type, Mode.signature, group);
    }

    // -----------------------------------------------------------------private methods

    private static String create(String tokenPlain, TokenType type, Mode mode, String group) {
        if (StringUtils.isEmpty(tokenPlain)) {
            return null;
        }
        String expiration = Long.toString(System.currentTimeMillis() + EXPIRATION_MILLISECONDS);
        return secret(tokenPlain, type, mode, expiration, group) + DOT + expiration;
    }

    private static boolean verify(String tokenSecret, String tokenPlain, TokenType type, Mode mode, String group) {
        if (StringUtils.isEmpty(tokenPlain)) {
            return true;
        }
        if (StringUtils.isEmpty(tokenSecret) || !tokenSecret.contains(DOT)) {
            return false;
        }

        String[] array = tokenSecret.split("\\.", 2);
        String actual = array[0];
        String expiration = array[1];
        if (Long.parseLong(expiration) < System.currentTimeMillis()) {
            return false;
        }

        String expect = secret(tokenPlain, type, mode, expiration, group);
        return actual.equals(expect);
    }

    private static String secret(String tokenPlain, TokenType type, Mode mode, String expiration, String group) {
        Assert.notNull(type, "Type cannot be null.");
        Assert.notNull(mode, "Mode cannot be null.");
        Assert.hasText(group, "Group cannot be empty.");
        String payload = type.name() + DOT + mode.name() + DOT + expiration + DOT + group;

        HmacUtils hm = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, tokenPlain.getBytes(UTF_8));
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
