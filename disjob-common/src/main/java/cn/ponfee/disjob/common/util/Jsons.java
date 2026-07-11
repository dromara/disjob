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
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ArrayNode;

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

    public static final TypeReference<Map<String, Object>> MAP_NORMAL = new TypeReference<>() {};
    public static final TypeReference<List<String>> LIST_STRING = new TypeReference<>() {};

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
    public static final JsonMapper JSON5 = createJsonMapperBuilder(JsonInclude.Include.NON_NULL)
        .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)                    // 键和值：可以用单引号
        .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER) // 字符串值：可以通过转义换行符来跨越多行
        .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)          // 允许有未转义的控制符
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)                   // 对象或数组：可以有一个尾随逗号
        .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)                    // 允许YAML风格的单行和多行注释
        .build();

    /**
     * Jackson JsonMapper (thread safe)
     */
    private final JsonMapper jsonMapper;

    private Jsons(JsonInclude.Include include) {
        this.jsonMapper = createJsonMapper(include);
    }

    // --------------------------------------------------------serialization

    /**
     * Converts object to json, and write to output stream
     *
     * @param output the output stream
     * @param target the target object
     */
    public void write(OutputStream output, Object target) {
        jsonMapper.writeValue(output, target);
    }

    /**
     * Converts an object(POJO, Array, Collection, ...) to json string
     *
     * @param target target object
     * @return json string
     */
    public String string(Object target) {
        return jsonMapper.writeValueAsString(target);
    }

    /**
     * Serialize the byte array of json
     *
     * @param target object
     * @return byte[] array
     */
    public byte[] bytes(Object target) {
        return jsonMapper.writeValueAsBytes(target);
    }

    // --------------------------------------------------------deserialization

    /**
     * Deserialize the json string to java object
     *
     * @param json     json string
     * @param javaType JavaType
     * @return the javaType's object
     * @see JsonMapper#getTypeFactory()
     * @see JsonMapper#constructType(Type)
     * @see tools.jackson.databind.type.TypeFactory#constructGeneralizedType(JavaType, Class)
     */
    public <T> T parse(String json, JavaType javaType) {
        if (json == null) {
            return null;
        }
        return jsonMapper.readValue(json, javaType);
    }

    /**
     * Deserialize the json byte array to java object
     *
     * @param json     json byte array
     * @param javaType JavaType
     * @return the javaType's object
     */
    public <T> T parse(byte[] json, JavaType javaType) {
        if (json == null) {
            return null;
        }
        return jsonMapper.readValue(json, javaType);
    }

    public <T> T parse(String json, Class<T> target) {
        return parse(json, jsonMapper.constructType(target));
    }

    public <T> T parse(byte[] json, Class<T> target) {
        return parse(json, jsonMapper.constructType(target));
    }

    public <T> T parse(String json, Type type) {
        return parse(json, jsonMapper.constructType(type));
    }

    public <T> T parse(byte[] json, Type type) {
        return parse(json, jsonMapper.constructType(type));
    }

    public <T> T parse(String json, TypeReference<T> type) {
        return parse(json, jsonMapper.constructType(type));
    }

    public <T> T parse(byte[] json, TypeReference<T> type) {
        return parse(json, jsonMapper.constructType(type));
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

        JsonMapper mapper = NORMAL.jsonMapper;
        JsonNode rootNode = mapper.readTree(body);
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
        // 不推荐使用fastjson2，项目中尽量统一使用一种JSON序列化方式
        //return com.alibaba.fastjson2.JSON.parseArray(body, method.getGenericParameterTypes()).toArray();
        return parseArgs(body, method.getGenericParameterTypes());
    }

    public static Object[] parseArgs(String body, Type[] parameterTypes) {
        if (body == null) {
            return null;
        }

        int argumentCount = parameterTypes.length;
        if (/*method.getParameterCount()*/argumentCount == 0) {
            return null;
        }

        JsonMapper mapper = NORMAL.jsonMapper;
        JsonNode rootNode = mapper.readTree(body);
        if (rootNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) rootNode;

            // 方法只有一个参数，但请求参数长度大于1
            // ["a", "b"]     -> method(Object[] arg) -> arg=["a", "b"]
            // [["a"], ["b"]] -> method(Object[] arg) -> arg=[["a"], ["b"]]
            if (argumentCount == 1 && arrayNode.size() > 1) {
                return new Object[]{parse(mapper, arrayNode, parameterTypes[0])};
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
                methodArguments[i] = parse(mapper, arrayNode.get(i), parameterTypes[i]);
            }
            return methodArguments;
        } else {
            Assert.isTrue(argumentCount == 1, "Single object request parameter not support multiple arguments method.");
            return new Object[]{parse(mapper, rootNode, parameterTypes[0])};
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

    public static JsonMapper createJsonMapper(JsonInclude.Include include) {
        return createJsonMapperBuilder(include).build();
    }

    /**
     * Creates JsonMapper.Builder
     *
     * @param include the JsonInclude
     * @return JsonMapper.Builder instance
     */
    private static JsonMapper.Builder createJsonMapperBuilder(JsonInclude.Include include) {
        JsonFactory jsonFactory = JsonFactory.builder().disable(JsonFactory.Feature.INTERN_PROPERTY_NAMES).build();
        JsonMapper.Builder builder = JsonMapper.builder(jsonFactory);
        if (include != null) {
            builder.changeDefaultPropertyInclusion(e -> e.withValueInclusion(include));
        }

        // Common config
        builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // 反序列化时忽略未知属性
        builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);         // Date不序列化为时间戳
        builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);          // 解决报错：No serializer found for class XXX and no properties discovered to create BeanSerializer
        builder.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN);       // BigDecimal禁用科学计数格式输出，new BigDecimal("0.00000000000000001"): 1E-17 -> 0.00000000000000001
        builder.disable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES);     // 禁止无双引号字段
        builder.enable(JsonWriteFeature.QUOTE_PROPERTY_NAMES);              // 字段加双引号

        // java.util.Date config
        // java.util.Date：registerModule > JsonFormat(会使用setTimeZone) > setDateFormat(会使用setTimeZone)
        //   1）如果同时配置了setDateFormat和registerModule，则使用registerModule
        //   2）如果设置了setTimeZone，则会调用setDateFormat#setTimeZone(注：setTimeZone对registerModule无影响)
        //   3）如果实体字段使用了JsonFormat注解，则setDateFormat不生效(会使用jackson内置的格式化器，默认为0时区，此时要setTimeZone)
        //   4）JsonFormat注解对registerModule无影响(registerModule优先级最高)
        builder.defaultTimeZone(JavaUtilDateFormat.DEFAULT.getTimeZone()); // TimeZone.getDefault()
        builder.defaultDateFormat(JavaUtilDateFormat.DEFAULT);

        // register module
        builder.addModule(createCustomizationModule());

        // Others config
        //builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        return builder;
    }

    public static SimpleModule createCustomizationModule() {
        SimpleModule customizationModule = new SimpleModule();

        // java.util.Date config
        customizationModule.addSerializer(Date.class, JacksonDate.INSTANCE.serializer());
        customizationModule.addDeserializer(Date.class, JacksonDate.INSTANCE.deserializer());

        // java8 time config
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(Dates.DATETIME_PATTERN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(Dates.TIME_PATTERN);
        customizationModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        customizationModule.addDeserializer(LocalDateTime.class, new ValueDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser parser, DeserializationContext ctx) {
                String text = parser.getString();
                return StringUtils.isBlank(text) ? null : LocalDateTimeFormat.DEFAULT.parse(text);
            }
        });
        customizationModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_DATE));
        customizationModule.addDeserializer(LocalDate.class, new ValueDeserializer<>() {
            @Override
            public LocalDate deserialize(JsonParser parser, DeserializationContext ctx) {
                String text = parser.getString();
                return StringUtils.isBlank(text) ? null : LocalDateTimeFormat.DEFAULT.parse(text).toLocalDate();
            }
        });
        customizationModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        customizationModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        // 金额序列化
        //customizationModule.addSerializer(Money.class, JacksonMoney.INSTANCE.serializer());
        //customizationModule.addDeserializer(Money.class, JacksonMoney.INSTANCE.deserializer());

        // 返回给端上浏览器JavaScript Number数值过大时会有问题：Number.MAX_SAFE_INTEGER = 9007199254740991，即"0x1FFFFFFFFFFFFFL"
        // 当数值大于`9007199254740991`时就有可能会丢失精度：1234567891011121314 -> 1234567891011121400
        //customizationModule.addSerializer(Long.class, ToStringSerializer.instance);
        //customizationModule.addSerializer(BigInteger.class, ToStringSerializer.instance);

        return customizationModule;
    }

    // -----------------------------------------------------------------------private methods

    private static Object parse(JsonMapper mapper, JsonNode jsonNode, Type type) {
        return mapper
            .readerFor(mapper.getTypeFactory().constructType(type))
            .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .readValue(mapper.treeAsTokens(jsonNode));
    }

}
