/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.base;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import org.springframework.http.MediaType;

/**
 * Job constants
 *
 * @author Ponfee
 */
public class JobConstants {

    /**
     * Process batch size
     */
    public static final int PROCESS_BATCH_SIZE = 200;

    /**
     * Spring web server port
     */
    public static final String SPRING_WEB_SERVER_PORT = "server.port";

    /**
     * Disjob configuration key prefix
     */
    public static final String DISJOB_KEY_PREFIX = "disjob";

    /**
     * Server bound host
     */
    public static final String DISJOB_BOUND_SERVER_HOST = DISJOB_KEY_PREFIX + ".bound.server.host";

    /**
     * Disjob server registry key prefix
     */
    public static final String DISJOB_REGISTRY_KEY_PREFIX = DISJOB_KEY_PREFIX + ".registry";

    /**
     * Disjob worker configuration key prefix.
     */
    public static final String WORKER_KEY_PREFIX = DISJOB_KEY_PREFIX + ".worker";

    /**
     * Disjob supervisor configuration key prefix.
     */
    public static final String SUPERVISOR_KEY_PREFIX = DISJOB_KEY_PREFIX + ".supervisor";

    /**
     * Http rest configuration key prefix.
     */
    public static final String HTTP_KEY_PREFIX = DISJOB_KEY_PREFIX + ".http";

    /**
     * Retry configuration key prefix.
     */
    public static final String RETRY_KEY_PREFIX = DISJOB_KEY_PREFIX + ".retry";

    /**
     * Spring container bean name prefix.
     */
    public static final String SPRING_BEAN_NAME_PREFIX = DISJOB_KEY_PREFIX + ".bean";

    /**
     * Current supervisor spring bean name
     */
    public static final String SPRING_BEAN_NAME_CURRENT_SUPERVISOR = SPRING_BEAN_NAME_PREFIX + ".current-supervisor";

    /**
     * Timing wheel spring bean name
     */
    public static final String SPRING_BEAN_NAME_TIMING_WHEEL = SPRING_BEAN_NAME_PREFIX + ".timing-wheel";

    /**
     * Current worker spring bean name
     */
    public static final String SPRING_BEAN_NAME_CURRENT_WORKER = SPRING_BEAN_NAME_PREFIX + ".current-worker";

    /**
     * Authenticate header group
     */
    public static final String AUTHENTICATE_HEADER_GROUP = "X-Disjob-Group";

    /**
     * Authenticate header username
     */
    public static final String AUTHENTICATE_HEADER_USER = "X-Disjob-User";

    /**
     * Authenticate header token
     */
    public static final String AUTHENTICATE_HEADER_TOKEN = "X-Disjob-Token";

    /**
     * Http url pattern
     */
    public static final String HTTP_URL_PATTERN = "http://%s:%d/";

    /**
     * Https url pattern
     */
    public static final String HTTPS_URL_PATTERN = "https://%s:%d/";

    /**
     * Instance lock pool
     */
    public static final Interner<Long> INSTANCE_LOCK_POOL = Interners.newWeakInterner();

    /**
     * UTF-8 charset
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * Media type for application/json;charset=UTF-8
     */
    public static final String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON_VALUE + ";charset=" + UTF_8;

    /**
     * Media type for text/plain;charset=UTF-8
     */
    public static final String TEXT_PLAIN_UTF8 = MediaType.TEXT_PLAIN_VALUE + ";charset=" + UTF_8;

    /**
     * Media type for text/html;charset=UTF-8
     */
    public static final String TEXT_HTML_UTF8 = MediaType.TEXT_HTML_VALUE + ";charset=" + UTF_8;

}
