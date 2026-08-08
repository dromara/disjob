/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.date.JacksonDate;
import cn.ponfee.disjob.common.date.JavaUtilDateFormat;
import cn.ponfee.disjob.common.date.LocalDateTimeFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The json utility based jackson
 * <p><a href="https://json-5.com/">json5</a>
 * <p><a href="https://stackoverflow.com/questions/68312227/can-the-jackson-parser-be-used-to-parse-json5">json5-QA</a>
 * <p>线程安全
 *
 * @author Ponfee
 */
public final class Jsons {

    public static final TypeReference<Map<String, Object>> MAP_NORMAL = new TypeReference<Map<String, Object>>() {};
    public static final TypeReference<List<String>> LIST_STRING = new TypeReference<List<String>>() {};

    /**
     * Jackson ObjectMapper support json5 (thread safe)
     */
    public static final ObjectMapper JSON5 = createObjectMapper()
        .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature())                    // 键和值：可以用单引号
        .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature()) // 字符串值：可以通过转义换行符来跨越多行
        .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())          // 允许有未转义的控制符
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())                   // 对象或数组：可以有一个尾随逗号
        .enable(JsonReadFeature.ALLOW_YAML_COMMENTS.mappedFeature());                   // 允许YAML风格的单行和多行注释

    /**
     * Jackson ObjectMapper (thread safe)
     */
    private static final ObjectMapper JSON = createObjectMapper();

    // --------------------------------------------------------serialization

    /**
     * Converts object value to json, and write to output stream
     *
     * @param output the output
     * @param value  the object value
     */
    public static void writeValue(OutputStream output, Object value) {
        try {
            JSON.writeValue(output, value);
        } catch (IOException e) {
            ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Converts object value (POJO, Array, Collection, ...) to JSON
     *
     * @param value the object value
     * @return JSON
     */
    public static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Serialize the object value to json byte array
     *
     * @param value the object value
     * @return JSON byte array
     */
    public static byte[] toBytes(Object value) {
        try {
            return JSON.writeValueAsBytes(value);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    // --------------------------------------------------------deserialization

    /**
     * Deserialize the JSON to object
     *
     * @param json the JSON
     * @param type the value
     * @return object of type
     * @see ObjectMapper#getTypeFactory()
     * @see ObjectMapper#constructType(Type)
     * @see com.fasterxml.jackson.databind.type.TypeFactory#constructGeneralizedType(JavaType, Class)
     */
    public static <T> T fromJson(String json, JavaType type) {
        return json == null ? null : parse(json, type);
    }

    /**
     * Deserialize the json byte array to object
     *
     * @param json the JSON byte array
     * @param type the value
     * @return object of type
     */
    public static <T> T fromJson(byte[] json, JavaType type) {
        return json == null ? null : parse(json, type);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return json == null ? null : parse(json, JSON.constructType(type));
    }

    public static <T> T fromJson(byte[] json, Class<T> type) {
        return json == null ? null : parse(json, JSON.constructType(type));
    }

    public static <T> T fromJson(String json, Type type) {
        return json == null ? null : parse(json, JSON.constructType(type));
    }

    public static <T> T fromJson(byte[] json, Type type) {
        return json == null ? null : parse(json, JSON.constructType(type));
    }

    public static <T> T fromJson(String json, TypeReference<T> type) {
        return json == null ? null : parse(json, JSON.constructType(type));
    }

    public static <T> T fromJson(byte[] json, TypeReference<T> type) {
        return json == null ? null : parse(json, JSON.constructType(type));
    }

    public static Object[] parseArray(String arrayJson, Type[] types) throws IOException {
        if (arrayJson == null) {
            return null;
        }

        JsonNode rootNode = JSON.readTree(arrayJson);
        if (rootNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) rootNode;

            // 方法只有一个参数，但请求参数长度大于1
            // ["a", "b"]     -> method(Object[] arg) -> arg=["a", "b"]
            // [["a"], ["b"]] -> method(Object[] arg) -> arg=[["a"], ["b"]]
            if (types.length == 1 && arrayNode.size() > 1) {
                return new Object[]{parse(arrayNode, types[0])};
            }

            // 其它情况，在调用方将参数(requestParameters)用数组包一层：new Object[]{ arg-1, arg-2, ..., arg-n }
            // [["a", "b"]]   -> method(Object[] arg)                 -> arg =["a", "b"]
            // [["a"], ["b"]] -> method(Object[] arg1, Object[] arg2) -> arg1=["a"], arg2=["b"]
            // ["a", "b"]     -> method(Object[] arg1, Object[] arg2) -> arg1=["a"], arg2=["b"]  # ACCEPT_SINGLE_VALUE_AS_ARRAY作用：将字符串“a”转为数组arg1[]
            if (types.length != arrayNode.size()) {
                throw new IllegalArgumentException("Inconsistent array size: " + types.length + " != " + arrayNode.size());
            }

            Object[] array = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                array[i] = parse(arrayNode.get(i), types[i]);
            }
            return array;
        } else {
            Assert.isTrue(types.length == 1, "Must be single array json.");
            return new Object[]{parse(rootNode, types[0])};
        }
    }

    public static Object[] parseMethodArgs(String argsJson, Method method) {
        // 不推荐使用fastjson2，项目中尽量统一使用一种JSON序列化方式
        //return com.alibaba.fastjson2.JSON.parseArray(argsJson, method.getGenericParameterTypes()).toArray();
        try {
            return parseArray(argsJson, method.getGenericParameterTypes());
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
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
     *    .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)                    // 允许YAML风格的单行和多行注释
     *    .build();
     * }</pre>
     *
     * @return ObjectMapper instance
     */
    public static ObjectMapper createObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.factory(JsonFactory.builder().disable(JsonFactory.Feature.INTERN_FIELD_NAMES).build());
        configureObjectMapperBuilder(builder);
        return builder.build();
    }

    public static void configureObjectMapperBuilder(Jackson2ObjectMapperBuilder builder) {
        // Common config
        builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 反序列化时忽略未知属性
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)       // Date不序列化为时间戳
            .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)             // 解决报错：No serializer found for class XXX and no properties discovered to create BeanSerializer
            .featuresToEnable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)       // BigDecimal禁用科学计数格式输出，new BigDecimal("0.00000000000000001"): 1E-17 -> 0.00000000000000001
            .featuresToDisable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)        // 禁止无双引号字段
            .featuresToEnable(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature());   // 字段加双引号

        // java.util.Date config
        // java.util.Date：registerModule > JsonFormat(会使用setTimeZone) > setDateFormat(会使用setTimeZone)
        //   1）如果同时配置了setDateFormat和registerModule，则使用registerModule
        //   2）如果设置了setTimeZone，则会调用setDateFormat#setTimeZone(注：setTimeZone对registerModule无影响)
        //   3）如果实体字段使用了JsonFormat注解，则setDateFormat不生效(会使用jackson内置的格式化器，默认为0时区，此时要setTimeZone)
        //   4）JsonFormat注解对registerModule无影响(registerModule优先级最高)
        builder.timeZone(JavaUtilDateFormat.DEFAULT.getTimeZone()) // TimeZone.getDefault()
            .dateFormat(JavaUtilDateFormat.DEFAULT);               // default date format

        // register module
        builder.modulesToInstall(createSimpleModule())
            .modulesToInstall(createJavaTimeModule())
            .modulesToInstall(new Jdk8Module());

        // Others config
        //builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public static SimpleModule createSimpleModule() {
        SimpleModule simpleModule = new SimpleModule();

        // java.util.Date module config
        simpleModule.addSerializer(Date.class, JacksonDate.INSTANCE.serializer());
        simpleModule.addDeserializer(Date.class, JacksonDate.INSTANCE.deserializer());

        // 金额序列化
        //simpleModule.addSerializer(Money.class, JacksonMoney.INSTANCE.serializer());
        //simpleModule.addDeserializer(Money.class, JacksonMoney.INSTANCE.deserializer());

        // 返回给端上浏览器JavaScript Number数值过大时会有问题：Number.MAX_SAFE_INTEGER = 9007199254740991，即“0x1FFFFFFFFFFFFFL”
        // 当数值大于`9007199254740991`时就有可能会丢失精度：1234567891011121314 -> 1234567891011121400
        //simpleModule.addSerializer(long.class, ToStringSerializer.instance);
        //simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);

        return simpleModule;
    }

    public static JavaTimeModule createJavaTimeModule() {
        // java new time module config
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(Dates.DATETIME_PATTERN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(Dates.TIME_PATTERN);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
                String text = parser.getText();
                return StringUtils.isBlank(text) ? null : LocalDateTimeFormat.DEFAULT.parse(text);
            }
        });
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_DATE));
        javaTimeModule.addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
                String text = parser.getText();
                return StringUtils.isBlank(text) ? null : LocalDateTimeFormat.DEFAULT.parse(text).toLocalDate();
            }
        });
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        return javaTimeModule;
    }

    // --------------------------------------------------------private methods

    private static Object parse(JsonNode jsonNode, Type type) throws IOException {
        return JSON
            .readerFor(JSON.getTypeFactory().constructType(type))
            .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .readValue(JSON.treeAsTokens(jsonNode));
    }

    public static <T> T parse(String json, JavaType type) {
        try {
            return JSON.readValue(json, type);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static <T> T parse(byte[] json, JavaType type) {
        try {
            return JSON.readValue(json, type);
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

}
