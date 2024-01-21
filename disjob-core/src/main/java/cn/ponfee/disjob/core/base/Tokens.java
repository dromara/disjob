/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

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

    private static final long EXPIRE_MILLISECONDS = 60_000L;

    public enum Type {
        /**
         * Supervisor token
         */
        SUPERVISOR,
        /**
         * Worker token
         */
        WORKER,
        /**
         * User token
         */
        USER,
    }

    public enum Mode {
        /**
         * For authenticate
         */
        AUTHENTICATE,
        /**
         * For signature
         */
        SIGNATURE,
    }

    public static String create(String tokenPlain, Type type, Mode mode, String group) {
        if (StringUtils.isEmpty(tokenPlain)) {
            return null;
        }
        String expire = Long.toString(System.currentTimeMillis() + EXPIRE_MILLISECONDS);
        return secret(tokenPlain, type, mode, expire, group) + DOT + expire;
    }

    public static boolean verify(String tokenSecret, String tokenPlain, Type type, Mode mode, String group) {
        if (StringUtils.isEmpty(tokenPlain)) {
            return true;
        }
        if (StringUtils.isEmpty(tokenSecret) || !tokenSecret.contains(DOT)) {
            return false;
        }

        String[] array = tokenSecret.split("\\.", 2);
        String actual = array[0];
        String expire = array[1];
        if (Long.parseLong(expire) < System.currentTimeMillis()) {
            return false;
        }

        String expect = secret(tokenPlain, type, mode, expire, group);
        return actual.equals(expect);
    }

    // -----------------------------------------------------------------private methods

    private static String secret(String tokenPlain, Type type, Mode mode, String expire, String group) {
        Assert.notNull(type, "Type cannot be null.");
        Assert.notNull(mode, "Mode cannot be null.");
        Assert.hasText(group, "Group cannot be empty.");
        String payload = type + DOT + mode + DOT + expire + DOT + group;

        HmacUtils hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, tokenPlain.getBytes(UTF_8));
        byte[] data = hmac.hmac(payload.getBytes(UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

}
