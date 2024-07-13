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

import cn.ponfee.disjob.common.date.CustomLocalDateTimeDeserializer;
import cn.ponfee.disjob.common.date.JacksonDate;
import cn.ponfee.disjob.common.date.JavaUtilDateFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
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
 * <p><a href="https://json-5.com/">json5</a>
 * <p><a href="https://stackoverflow.com/questions/68312227/can-the-jackson-parser-be-used-to-parse-json5">json5-QA</a>
 * <p>线程安全
 *
 * @author Ponfee
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
     * Object mapper support json5
     */
    public static final ObjectMapper JSON5 = Jsons.createObjectMapper(JsonInclude.Include.NON_NULL)
        .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature())                    // 键和值：可以用单引号
        .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature()) // 字符串值：可以通过转义换行符来跨越多行
        .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())          // 允许有未转义的控制符
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())                   // 对象或数组：可以有一个尾随逗号
        .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature());                   // 允许单行和多行注释

    /**
     * Jackson ObjectMapper(thread safe)
     */
    private final ObjectMapper objectMapper;

    private Jsons(JsonInclude.Include include) {
        this.objectMapper = createObjectMapper(include);
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
            objectMapper.writeValue(output, target);
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
            return objectMapper.writeValueAsString(target);
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
            return objectMapper.writeValueAsBytes(target);
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
            return objectMapper.readValue(json, javaType);
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
            return objectMapper.readValue(json, javaType);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public <T> T parse(String json, Class<T> target) {
        return parse(json, objectMapper.constructType(target));
    }

    public <T> T parse(byte[] json, Class<T> target) {
        return parse(json, objectMapper.constructType(target));
    }

    public <T> T parse(String json, Type type) {
        return parse(json, objectMapper.constructType(type));
    }

    public <T> T parse(byte[] json, Type type) {
        return parse(json, objectMapper.constructType(type));
    }

    public <T> T parse(String json, TypeReference<T> type) {
        return parse(json, objectMapper.constructType(type));
    }

    public <T> T parse(byte[] json, TypeReference<T> type) {
        return parse(json, objectMapper.constructType(type));
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

        ObjectMapper objectMapper = NORMAL.objectMapper;
        JsonNode rootNode = readTree(objectMapper, body);
        Assert.isTrue(rootNode.isArray(), "Not array json data.");
        ArrayNode arrayNode = (ArrayNode) rootNode;

        if (types.length == 1 && arrayNode.size() > 1) {
            return new Object[]{parse(objectMapper, arrayNode, types[0])};
        }

        Object[] result = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = parse(objectMapper, arrayNode.get(i), types[i]);
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

        ObjectMapper objectMapper = NORMAL.objectMapper;
        JsonNode rootNode = readTree(objectMapper, body);
        if (rootNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) rootNode;

            // 方法只有一个参数，但请求参数长度大于1
            // ["a", "b"]     -> method(Object[] arg) -> arg=["a", "b"]
            // [["a"], ["b"]] -> method(Object[] arg) -> arg=[["a"], ["b"]]
            if (argumentCount == 1 && arrayNode.size() > 1) {
                return new Object[]{parse(objectMapper, arrayNode, genericArgumentTypes[0])};
            }

            // 其它情况，在调用方将参数(requestParameters)用数组包一层：new Object[]{ arg-1, arg-2, ..., arg-n }
            // [["a", "b"]]   -> method(Object[] arg)                 -> arg =["a", "b"]
            // [["a"], ["b"]] -> method(Object[] arg1, Object[] arg2) -> arg1=["a"], arg2=["b"]
            // ["a", "b"]     -> method(Object[] arg1, Object[] arg2) -> arg1=["a"], arg2=["b"]  # ACCEPT_SINGLE_VALUE_AS_ARRAY作用：将字符串“a”转为数组arg1[]
            if (argumentCount != arrayNode.size()) {
                throw new IllegalArgumentException("Inconsistent method arguments size: " + argumentCount + " != " + arrayNode.size());
            }

            Object[] methodArguments = new Object[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                methodArguments[i] = parse(objectMapper, arrayNode.get(i), genericArgumentTypes[i]);
            }
            return methodArguments;
        } else {
            Assert.isTrue(argumentCount == 1, "Single object request parameter not support multiple arguments method.");
            return new Object[]{parse(objectMapper, rootNode, genericArgumentTypes[0])};
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

    /**
     * <pre>也可以使用JsonMapper.Builder来构建：{@code
     *  ObjectMapper objectMapper = JsonMapper.builder()
     *    .serializationInclusion(JsonInclude.Include.NON_NULL)           // 序列化时忽略值为null的字段
     *    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)     // 反序列化时忽略未知字段
     *    .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)                    // 键和值：可以用单引号
     *    .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER) // 字符串值：可以通过转义换行符来跨越多行
     *    .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)                   // 对象或数组：可以有一个尾随逗号
     *    .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)          // 允许有未转义的控制符
     *    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)                    // 允许单行和多行注释
     *    .build();
     * }</pre>
     *
     * @param include the JsonInclude
     * @return ObjectMapper instance
     */
    public static ObjectMapper createObjectMapper(JsonInclude.Include include) {
        JsonFactory jsonFactory = new JsonFactoryBuilder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .build();
        ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
        // 设置序列化时的特性
        if (include != null) {
            objectMapper.setSerializationInclusion(include);
        }
        configObjectMapper(objectMapper);
        return objectMapper;
    }

    public static void configObjectMapper(ObjectMapper objectMapper) {
        // Common config
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // 反序列化时忽略未知属性
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);    // Date不序列化为时间戳
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);          // 解决报错：No serializer found for class XXX and no properties discovered to create BeanSerializer
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);    // BigDecimal禁用科学计数格式输出，new BigDecimal("0.00000000000000001"): 1E-17 -> 0.00000000000000001
        objectMapper.disable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);     // 禁止无双引号字段
        objectMapper.enable(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature()); // 字段加双引号

        // java.util.Date config
        // java.util.Date：registerModule > JsonFormat(会使用setTimeZone) > setDateFormat(会使用setTimeZone)
        //   1）如果同时配置了setDateFormat和registerModule，则使用registerModule
        //   2）如果设置了setTimeZone，则会调用setDateFormat#setTimeZone(注：setTimeZone对registerModule无影响)
        //   3）如果实体字段使用了JsonFormat注解，则setDateFormat不生效(会使用jackson内置的格式化器，默认为0时区，此时要setTimeZone)
        //   4）JsonFormat注解对registerModule无影响(registerModule优先级最高)
        objectMapper.setTimeZone(JavaUtilDateFormat.DEFAULT.getTimeZone()); // TimeZone.getDefault()
        objectMapper.setDateFormat(JavaUtilDateFormat.DEFAULT);
        //objectMapper.setConfig(mapper.getDeserializationConfig().with(mapper.getDateFormat()));
        //objectMapper.setConfig(mapper.getSerializationConfig().with(mapper.getDateFormat()));

        // register module
        registerSimpleModule(objectMapper);
        registerJavaTimeModule(objectMapper);
        objectMapper.registerModule(new Jdk8Module());

        // Others config
        //objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public static void registerSimpleModule(ObjectMapper objectMapper) {
        SimpleModule simpleModule = new SimpleModule();

        // java.util.Date module config
        simpleModule.addSerializer(Date.class, JacksonDate.INSTANCE.serializer());
        simpleModule.addDeserializer(Date.class, JacksonDate.INSTANCE.deserializer());

        // 金额序列化
        //simpleModule.addSerializer(Money.class, JacksonMoney.INSTANCE.serializer());
        //simpleModule.addDeserializer(Money.class, JacksonMoney.INSTANCE.deserializer());

        // 返回给端上浏览器JavaScript Number数值过大时会有问题：Number.MAX_SAFE_INTEGER = 9007199254740991，即“0x1FFFFFFFFFFFFFL”
        // 当数值大于`9007199254740991`时就有可能会丢失精度：1234567891011121314 -> 1234567891011121400
        //simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        //simpleModule.addSerializer(long.class, ToStringSerializer.instance);
        //simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);

        objectMapper.registerModule(simpleModule);
    }

    public static void registerJavaTimeModule(ObjectMapper objectMapper) {
        // java new time module config
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, CustomLocalDateTimeDeserializer.INSTANCE);
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        objectMapper.registerModule(javaTimeModule);
    }

    // -----------------------------------------------------------------------private methods

    private static JsonNode readTree(ObjectMapper objectMapper, String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    private static Object parse(ObjectMapper objectMapper, JsonNode jsonNode, Type type) {
        try {
            return objectMapper
                .readerFor(objectMapper.getTypeFactory().constructType(type))
                .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .readValue(objectMapper.treeAsTokens(jsonNode));
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

}
