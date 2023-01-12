/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.util;

import cn.ponfee.scheduler.common.base.exception.JsonException;
import cn.ponfee.scheduler.common.date.CustomLocalDateTimeDeserializer;
import cn.ponfee.scheduler.common.date.JacksonDate;
import cn.ponfee.scheduler.common.date.JavaUtilDateFormat;
import cn.ponfee.scheduler.common.date.LocalDateTimeFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * The json utility based jackson
 *
 * @author Ponfee
 * @ThreadSafe
 */
@ThreadSafe
public final class Jsons {

    public static final TypeReference<Map<String, Object>> MAP_NORMAL = new TypeReference<Map<String, Object>>() {};

    /**
     * 标准：忽略对象中值为null的属性
     */
    public static final Jsons NORMAL = new Jsons(JsonInclude.Include.NON_NULL);

    /**
     * 不排除任何属性
     */
    public static final Jsons ALL = new Jsons(null);

    /**
     * Jackson ObjectMapper(thread safe)
     */
    private final ObjectMapper mapper;

    private Jsons(JsonInclude.Include include) {
        this.mapper = createObjectMapper(include);
    }

    // --------------------------------------------------------serialization

    /**
     * Converts object to json, and write to output stream
     *
     * @param output the output stream
     * @param target the target object
     * @throws JsonException if occur exception
     */
    public void write(OutputStream output, Object target) throws JsonException {
        try {
            mapper.writeValue(output, target);
        } catch (IOException e) {
            throw new JsonException(e);
        }
    }

    /**
     * Converts an object(POJO, Array, Collection, ...) to json string
     *
     * @param target target object
     * @return json string
     * @throws JsonException the exception for json
     */
    public String string(Object target) throws JsonException {
        try {
            return mapper.writeValueAsString(target);
        } catch (IOException e) {
            throw new JsonException(e);
        }
    }

    /**
     * Serialize the byte array of json
     *
     * @param target object
     * @return byte[] array
     * @throws JsonException the exception for json
     */
    public byte[] bytes(Object target) throws JsonException {
        try {
            return mapper.writeValueAsBytes(target);
        } catch (IOException e) {
            throw new JsonException(e);
        }
    }

    // --------------------------------------------------------deserialization

    /**
     * Deserialize the json string to java object
     *
     * @param json     json string
     * @param javaType JavaType
     * @return the javaType's object
     * @throws JsonException the exception for json
     * @see ObjectMapper#getTypeFactory()
     * @see ObjectMapper#constructType(Type)
     * @see com.fasterxml.jackson.databind.type.TypeFactory#constructGeneralizedType(JavaType, Class)
     */
    public <T> T parse(String json, JavaType javaType) throws JsonException {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            return mapper.readValue(json, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize the json byte array to java object
     *
     * @param json     json byte array
     * @param javaType JavaType
     * @return the javaType's object
     * @throws JsonException the exception for json
     */
    public <T> T parse(byte[] json, JavaType javaType) throws JsonException {
        if (json == null || json.length == 0) {
            return null;
        }
        try {
            return mapper.readValue(json, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public <T> T parse(String json, Class<T> target) throws JsonException {
        return parse(json, mapper.constructType(target));
    }

    public <T> T parse(byte[] json, Class<T> target) throws JsonException {
        return parse(json, mapper.constructType(target));
    }

    public <T> T parse(String json, Type type) throws JsonException {
        return parse(json, mapper.constructType(type));
    }

    public <T> T parse(byte[] json, Type type) throws JsonException {
        return parse(json, mapper.constructType(type));
    }

    public <T> T parse(String json, TypeReference<T> type) throws JsonException {
        return parse(json, mapper.constructType(type));
    }

    public <T> T parse(byte[] json, TypeReference<T> type) throws JsonException {
        return parse(json, mapper.constructType(type));
    }

    // ----------------------------------------------------static methods
    public static String toJson(Object target) {
        return NORMAL.string(target);
    }

    public static byte[] toBytes(Object target) {
        return NORMAL.bytes(target);
    }

    public static <T> T fromJson(String json, JavaType javaType) {
        return NORMAL.parse(json, javaType);
    }

    public static <T> T fromJson(byte[] json, JavaType javaType) {
        return NORMAL.parse(json, javaType);
    }

    public static <T> T fromJson(String json, Class<T> target) {
        return NORMAL.parse(json, target);
    }

    public static <T> T fromJson(byte[] json, Class<T> target) {
        return NORMAL.parse(json, target);
    }

    public static <T> T fromJson(String json, Type target) {
        return NORMAL.parse(json, target);
    }

    public static <T> T fromJson(byte[] json, Type target) {
        return NORMAL.parse(json, target);
    }

    public static <T> T fromJson(String json, TypeReference<T> type) {
        return NORMAL.parse(json, type);
    }

    public static <T> T fromJson(byte[] json, TypeReference<T> type) {
        return NORMAL.parse(json, type);
    }

    public static ObjectMapper createObjectMapper(JsonInclude.Include include) {
        ObjectMapper mapper = new ObjectMapper();

        // 设置序列化时的特性
        if (include != null) {
            mapper.setSerializationInclusion(include);
        }

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 反序列化时忽略未知属性
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);    // Date不序列化为时间戳
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);          // 解决报错：No serializer found for class XXX and no properties discovered to create BeanSerializer
        mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);    // BigDecimal禁用科学计数格式输出
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, false);     // 禁止无双引号字段
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, false);            // 禁止单引号字段
        mapper.configure(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature(), true); // 字段加双引号
        //objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);  // 禁止反序列化时，如果目标对象为空对象的报错问题

        // java.util.Date
        //   1）如果同时配置了setDateFormat和registerModule，则使用registerModule
        //   2）如果设置了setTimeZone，则会调用setDateFormat#setTimeZone(注：setTimeZone对registerModule无影响)
        //   3）如果实体字段使用了JsonFormat注解，则setDateFormat不生效(会使用jackson内置的格式化器，默认为0时区，此时要setTimeZone)
        //   4）JsonFormat注解对registerModule无影响(registerModule优先级最高)
        mapper.setTimeZone(JavaUtilDateFormat.DEFAULT.getTimeZone()); // TimeZone.getDefault()
        mapper.setDateFormat(JavaUtilDateFormat.DEFAULT);
        //mapper.setConfig(mapper.getDeserializationConfig().with(mapper.getDateFormat()));
        //mapper.setConfig(mapper.getSerializationConfig().with(mapper.getDateFormat()));

        SimpleModule module = new SimpleModule();
        module.addSerializer(Date.class, JacksonDate.INSTANCE.serializer());
        module.addDeserializer(Date.class, JacksonDate.INSTANCE.deserializer());
        //module.addSerializer(Money.class, JacksonMoney.INSTANCE.serializer());
        //module.addDeserializer(Money.class, JacksonMoney.INSTANCE.deserializer());
        mapper.registerModule(module);

        // java.time.LocalDateTime
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(LocalDateTimeFormat.PATTERN_11));
        javaTimeModule.addDeserializer(LocalDateTime.class, CustomLocalDateTimeDeserializer.INSTANCE);
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        mapper.registerModule(javaTimeModule);

        //mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        return mapper;
    }

}
