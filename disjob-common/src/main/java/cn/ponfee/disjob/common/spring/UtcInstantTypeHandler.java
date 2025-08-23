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

package cn.ponfee.disjob.common.spring;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * <pre>
 * 在国际化的项目中时间使用规范建议：created_at_utc
 *  1）Java中使用`Instant`（也可以使用`OffsetDateTime`）
 *  2）Database中使用`DATETIME(6)`
 *  3）前后端交互使用UTC字符串`2000-01-01T00:00:00.000Z`，在前端JS代码中转换为本地时间展示`new Date("2000-01-01T00:00:00.000Z")`
 *  4）Java把Instant转成LocalDateTime → 保存数据库：instant.atZone(ZoneOffset.UTC).toLocalDateTime();
 *  5）读取数据库 → Java把LocalDateTime转成Instant：datetime.atZone(ZoneOffset.UTC).toInstant()
 *
 * Java Instant与Mysql datetime之间的相互转换，在Mybatis的配置文件`mybatis-config.xml`中添加：
 * &lt;typeHandlers>
 *     &lt;typeHandler handler="cn.ponfee.disjob.common.spring.UtcInstantTypeHandler" javaType="java.time.Instant" />
 * &lt;/typeHandlers>
 * </pre>
 *
 * @author Ponfee
 * @see org.apache.ibatis.type.InstantTypeHandler
 */
public class UtcInstantTypeHandler extends BaseTypeHandler<Instant> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Instant parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter.atZone(ZoneOffset.UTC).toLocalDateTime());
    }

    @Override
    public Instant getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return utcDatetime2Instant(rs.getObject(columnName, LocalDateTime.class));
    }

    @Override
    public Instant getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return utcDatetime2Instant(rs.getObject(columnIndex, LocalDateTime.class));
    }

    @Override
    public Instant getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return utcDatetime2Instant(cs.getObject(columnIndex, LocalDateTime.class));
    }

    // -------------------------------------------------------private static methods

    private static Instant utcDatetime2Instant(LocalDateTime datetime) {
        return (datetime != null) ? datetime.atZone(ZoneOffset.UTC).toInstant() : null;
    }

}
