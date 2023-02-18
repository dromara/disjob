/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.base;

/**
 * Constant utility class
 *
 * @author Ponfee
 */
public class Constants {

    // -------------------------------------------------------------Datasource Configuration options

    public static final String DATA_SOURCE_SUFFIX = "DataSource";
    public static final String SQL_SESSION_FACTORY_SUFFIX = "SqlSessionFactory";
    public static final String SQL_SESSION_TEMPLATE_SUFFIX = "SqlSessionTemplate";
    public static final String TX_MANAGER_SUFFIX = "TransactionManager";
    public static final String TX_TEMPLATE_SUFFIX = "TransactionTemplate";
    public static final String JDBC_TEMPLATE_SUFFIX = "JDBCTemplate";

    // -------------------------------------------------------------String constants

    /**
     * Colon symbol
     */
    public static final String COLON = ":";

    /**
     * Comma symbol
     */
    public static final String COMMA = ",";

    /**
     * Dot symbol
     */
    public static final char DOT = '.';

    /**
     * Hyphen symbol
     */
    public static final char HYPHEN = '-';

    /**
     * Slash symbol
     */
    public static final char SLASH = '/';

}
