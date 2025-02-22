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

import org.springframework.http.MediaType;

/**
 * Job constants
 *
 * @author Ponfee
 */
public final class JobConstants {

    /**
     * Version
     */
    public static final String VERSION = "2.1.8";

    /**
     * 数据库大文本字段最大长度限制
     */
    public static final int CLOB_MAXIMUM_LENGTH = 65535;

    /**
     * Process batch size
     */
    public static final int PROCESS_BATCH_SIZE = 100;

    /**
     * Disjob configuration key prefix
     */
    public static final String DISJOB_KEY_PREFIX = "disjob";

    /**
     * Server bound host
     */
    public static final String DISJOB_BOUND_SERVER_HOST = DISJOB_KEY_PREFIX + ".bound.server.host";

    /**
     * Spring container bean name prefix.
     */
    public static final String SPRING_BEAN_NAME_PREFIX = DISJOB_KEY_PREFIX + ".bean";

    /**
     * Rest template spring bean name
     */
    public static final String SPRING_BEAN_NAME_REST_TEMPLATE = SPRING_BEAN_NAME_PREFIX + ".rest-template";

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
