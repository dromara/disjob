/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.date.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.Assert;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
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
     */
    public void write(OutputStream output, Object target) {
        try {
            mapper.writeValue(output, target);
        } catch (IOException e) {
            ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Converts an object(POJO, Array, Collection, ...) to json string
     *
     * @param target target object
     * @return json string
     */
    public String string(Object target) {
        try {
            return mapper.writeValueAsString(target);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Serialize the byte array of json
     *
     * @param target object
     * @return byte[] array
     */
    public byte[] bytes(Object target) {
        try {
            return mapper.writeValueAsBytes(target);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    // --------------------------------------------------------deserialization

    /**
     * Deserialize the json string to java object
     *
     * @param json     json string
     * @param javaType JavaType
     * @return the javaType's object
     * @see ObjectMapper#getTypeFactory()
     * @see ObjectMapper#constructType(Type)
     * @see com.fasterxml.jackson.databind.type.TypeFactory#constructGeneralizedType(JavaType, Class)
     */
    public <T> T parse(String json, JavaType javaType) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            return mapper.readValue(json, javaType);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Deserialize the json byte array to java object
     *
     * @param json     json byte array
     * @param javaType JavaType
     * @return the javaType's object
     */
    public <T> T parse(byte[] json, JavaType javaType) {
        if (json == null || json.length == 0) {
            return null;
        }
        try {
            return mapper.readValue(json, javaType);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public <T> T parse(String json, Class<T> target) {
        return parse(json, mapper.constructType(target));
    }

    public <T> T parse(byte[] json, Class<T> target) {
        return parse(json, mapper.constructType(target));
    }

    public <T> T parse(String json, Type type) {
        return parse(json, mapper.constructType(type));
    }

    public <T> T parse(byte[] json, Type type) {
        return parse(json, mapper.constructType(type));
    }

    public <T> T parse(String json, TypeReference<T> type) {
        return parse(json, mapper.constructType(type));
    }

    public <T> T parse(byte[] json, TypeReference<T> type) {
        return parse(json, mapper.constructType(type));
    }

    // ----------------------------------------------------static methods
    public static String toJson(Object target) {
        return NORMAL.string(target);
    }

    public static byte[] toBytes(Object target) {
        return NORMAL.bytes(target);
    }

    public static Object[] parseArray(String body, Class<?>... types) {
        if (body == null) {
            return null;
        }

        ObjectMapper mapper = NORMAL.mapper;
        JsonNode rootNode = readTree(mapper, body);
        Assert.isTrue(rootNode.isArray(), "Not array json data.");
        ArrayNode arrayNode = (ArrayNode) rootNode;

        if (types.length == 1 && arrayNode.size() > 1) {
            return new Object[]{parse(mapper, arrayNode, types[0])};
        }

        Object[] result = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = parse(mapper, arrayNode.get(i), types[i]);
        }
        return result;
    }

    public static Object[] parseMethodArgs(String body, Method method) {
        if (body == null) {
            return null;
        }

        // 不推荐使用fastjson，项目中尽量统一使用一种JSON序列化方式
        //return com.alibaba.fastjson.JSON.parseArray(body, method.getGenericParameterTypes()).toArray();

        Type[] genericArgumentTypes = method.getGenericParameterTypes();
        int argumentCount = genericArgumentTypes.length;
        if (/*method.getParameterCount()*/argumentCount == 0) {
            return null;
        }

        ObjectMapper mapper = NORMAL.mapper;
        JsonNode rootNode = readTree(mapper, body);
        if (rootNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) rootNode;

            // 方法只有一个参数，但请求参数长度大于1
            // ["a", "b"]     -> method(Object[] arg) -> arg=["a", "b"]
            // [["a"], ["b"]] -> method(Object[] arg) -> arg=[["a"], ["b"]]
            if (argumentCount == 1 && arrayNode.size() > 1) {
                return new Object[]{parse(mapper, arrayNode, genericArgumentTypes[0])};
            }

            // 其它情况，在调用方将参数(requestParameters)用数组包一层：new Object[]{ arg-1, arg-2, ..., arg-n }
            // [["a", "b"]]   -> method(Object[] arg)                 -> arg =["a", "b"]
            // [["a"], ["b"]] -> method(Object[] arg1, Object[] arg2) -> arg1=["a"], arg2=["b"]
            // ["a", "b"]     -> method(Object[] arg1, Object[] arg2) -> arg1=["a"], arg2=["b"]  # ACCEPT_SINGLE_VALUE_AS_ARRAY作用：将字符串“a”转为数组arg1[]
            Assert.isTrue(
                argumentCount == arrayNode.size(),
                () -> "Method arguments size: " + argumentCount + ", but actual size: " + arrayNode.size()
            );

            Object[] methodArguments = new Object[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                methodArguments[i] = parse(mapper, arrayNode.get(i), genericArgumentTypes[i]);
            }
            return methodArguments;
        } else {
            Assert.isTrue(argumentCount == 1, "Single object request parameter not support multiple arguments method.");
            return new Object[]{parse(mapper, rootNode, genericArgumentTypes[0])};
        }
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
        JsonFactory jsonFactory = new JsonFactoryBuilder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .build();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        // 设置序列化时的特性
        if (include != null) {
            mapper.setSerializationInclusion(include);
        }
        configObjectMapper(mapper);
        return mapper;
    }

    public static void configObjectMapper(ObjectMapper mapper) {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 反序列化时忽略未知属性
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);    // Date不序列化为时间戳
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);          // 解决报错：No serializer found for class XXX and no properties discovered to create BeanSerializer
        mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);    // BigDecimal禁用科学计数格式输出
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, false);     // 禁止无双引号字段
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, false);            // 禁止单引号字段
        mapper.configure(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature(), true); // 字段加双引号

        // java.util.Date：registerModule > JsonFormat(会使用setTimeZone) > setDateFormat(会使用setTimeZone)
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
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(Dates.DATE_PATTERN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(LocalDateTimeFormat.PATTERN_11));
        javaTimeModule.addDeserializer(LocalDateTime.class, CustomLocalDateTimeDeserializer.INSTANCE);
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        mapper.registerModule(javaTimeModule);

        //mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    private static JsonNode readTree(ObjectMapper mapper, String body) {
        try {
            return mapper.readTree(body);
        } catch (JsonProcessingException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    private static Object parse(ObjectMapper mapper, JsonNode jsonNode, Type type) {
        try {
            return mapper
                .readerFor(mapper.getTypeFactory().constructType(type))
                .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .readValue(mapper.treeAsTokens(jsonNode));
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

}
