package cn.ponfee.scheduler.common.base;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Jackson TypeReference holder
 *  
 * @author Ponfee
 */
public final class JacksonTypeReferences {

    public static final TypeReference<Map<String, Object>> MAP_NORMAL = new TypeReference<Map<String, Object>>() {};
    public static final TypeReference<Map<String, String>> MAP_STRING = new TypeReference<Map<String, String>>() {};

    public static final TypeReference<List<Object>> LIST_OBJECT = new TypeReference<List<Object>>() {};
    public static final TypeReference<List<String>> LIST_STRING = new TypeReference<List<String>>() {};

    public static final TypeReference<Set<Object>> SET_OBJECT = new TypeReference<Set<Object>>() {};
    public static final TypeReference<Set<String>> SET_STRING = new TypeReference<Set<String>>() {};

    public static final TypeReference<List<Map<String, Object>>> LIST_MAP_NORMAL = new TypeReference<List<Map<String, Object>>>() {};
    public static final TypeReference<List<Map<String, String>>> LIST_MAP_STRING = new TypeReference<List<Map<String, String>>>() {};

    public static final TypeReference<Set<Map<String, Object>>> SET_MAP_NORMAL = new TypeReference<Set<Map<String, Object>>>() {};
    public static final TypeReference<Set<Map<String, String>>> SET_MAP_STRING = new TypeReference<Set<Map<String, String>>>() {};

}
