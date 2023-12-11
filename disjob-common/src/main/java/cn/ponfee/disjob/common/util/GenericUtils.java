/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 泛型工具类
 *
 * https://segmentfault.com/a/1190000018319217
 *
 * @author Ponfee
 */
public final class GenericUtils {

    private static final Map<Class<?>, Map<String, Class<?>>> VARIABLE_TYPE_MAPPING = new ConcurrentHashMap<>();

    /**
     * map泛型协变
     * @param origin
     * @return
     */
    public static Map<String, String> covariant(Map<String, ?> origin) {
        if (origin == null) {
            return null;
        }

        Map<String, String> target = new HashMap<>(origin.size());
        for (Entry<String, ?> entry : origin.entrySet()) {
            target.put(entry.getKey(), Objects.toString(entry.getValue(), null));
        }
        return target;
    }

    public static Map<String, String> covariant(Properties properties) {
        return (Map) properties;
    }

    // ----------------------------------------------------------------------------class actual type argument
    /**
     * 获取泛型的实际类型参数
     *
     * @param clazz
     * @return
     */
    public static <T> Class<T> getActualTypeArgument(Class<?> clazz) {
        return getActualTypeArgument(clazz, 0);
    }

    /**
     * public class GenericClass extends GenericSuperClass<Long,Integer,...,String> implements GenericInterface<String,Short,..,Long> {}
     *
     * @param clazz
     * @param genericArgsIndex
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getActualTypeArgument(Class<?> clazz, int genericArgsIndex) {
        int index = 0;
        for (Type type : getGenericTypes(clazz)) {
            if (type instanceof ParameterizedType) {
                Type[] acts = ((ParameterizedType) type).getActualTypeArguments();
                if (acts.length + index < genericArgsIndex) {
                    index += acts.length;
                } else {
                    return getActualType(null, acts[genericArgsIndex - index]);
                }
            }
        }

        return (Class<T>) Object.class;
    }

    // ----------------------------------------------------------------------------method actual arg type argument

    public static <T> Class<T> getActualArgTypeArgument(Method method, int methodArgsIndex) {
        return getActualArgTypeArgument(method, methodArgsIndex, 0);
    }

    /**
     * public void genericMethod(List<Long> list, Map<String, String> map){}
     *
     * @param method            方法对象
     * @param methodArgsIndex 方法参数索引号
     * @param genericArgsIndex  泛型参数索引号
     * @return
     */
    public static <T> Class<T> getActualArgTypeArgument(Method method, int methodArgsIndex, int genericArgsIndex) {
        return getActualTypeArgument(method.getGenericParameterTypes()[methodArgsIndex], genericArgsIndex);
    }

    // ----------------------------------------------------------------------------method actual return type argument

    public static <T> Class<T> getActualReturnTypeArgument(Method method) {
        return getActualReturnTypeArgument(method, 0);
    }

    /**
     * public List<String> genericMethod(){}
     *
     * @param method the method
     * @param genericArgsIndex the generic argument index
     * @return
     */
    public static <T> Class<T> getActualReturnTypeArgument(Method method, int genericArgsIndex) {
        return getActualTypeArgument(method.getGenericReturnType(), genericArgsIndex);
    }

    // ----------------------------------------------------------------------------get actual type argument

    public static <T> Class<T> getActualTypeArgument(Field field) {
        return getActualTypeArgument(field, 0);
    }

    /**
     * private List<Long> list; -> Long
     *
     * @param field            the class field
     * @param genericArgsIndex the genericArgsIndex
     * @return
     */
    public static <T> Class<T> getActualTypeArgument(Field field, int genericArgsIndex) {
        return getActualTypeArgument(field.getGenericType(), genericArgsIndex);
    }

    // -------------------------------------------------------------------get actual variable type

    public static <T> Class<T> getFieldActualType(Class<?> clazz, String fieldName) {
        Field field = ClassUtils.getField(clazz, fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Type " + clazz + " not exists field '" + fieldName + "'");
        }
        return getFieldActualType(clazz, field);
    }

    /**
     * <pre>{@code
     * public abstract class BaseEntity<I> {
     *   private I id;
     * }
     * }</pre>
     *
     * <pre>{@code
     * public class BeanClass extends BaseEntity<String> {}
     * }</pre>
     *
     * @param clazz the sub class
     * @param field the super class defined field
     * @return a Class of field actual type
     */
    public static <T> Class<T> getFieldActualType(Class<?> clazz, Field field) {
        return Modifier.isStatic(field.getModifiers())
             ? (Class<T>) field.getType()
             : getActualType(clazz, field.getGenericType());
    }

    /**
     * Returns method arg actual type
     * <pre>{@code
     * public abstract class ClassA<T> {
     *   public void method(T arg) {}
     * }
     * }</pre>
     * <pre>{@code
     * public class ClassB extends classA<String>{}
     * }</pre>
     *
     * @param clazz            the sub class
     * @param method           the super class defined method
     * @param methodArgsIndex  the method arg index
     * @return a Class of method arg actual type
     */
    public static <T> Class<T> getMethodArgActualType(Class<?> clazz, Method method, int methodArgsIndex) {
        return getActualType(clazz, method.getGenericParameterTypes()[methodArgsIndex]);
    }

    /**
     * Returns method return actual type
     * <pre>{@code
     * public abstract class ClassA<T> {
     *   public T method() {}
     * }
     * }</pre>
     * <pre>{@code
     * public class ClassB extends classA<String>{}
     * }</pre>
     *
     * @param clazz  the sub class
     * @param method the super class defined method
     * @return a Class of method return actual type
     */
    public static <T> Class<T> getMethodReturnActualType(Class<?> clazz, Method method) {
        return getActualType(clazz, method.getGenericReturnType());
    }

    // public class ClassA extends ClassB<Map<U,V>> implements interfaceC<List<X>>, interfaceD<Y> {}
    public static List<Type> getGenericTypes(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return Collections.emptyList();
        }
        List<Type> types = new ArrayList<>();
        if (!clazz.isInterface()) {
            // Map<U,V>
            types.add(clazz.getGenericSuperclass());
        }
        // List<X>, Y
        Collections.addAll(types, clazz.getGenericInterfaces());
        return types;
    }

    public static Map<String, Class<?>> getActualTypeVariableMapping(Class<?> clazz) {
        Map<String, Class<?>> result = new HashMap<>();
        for (Type type : getGenericTypes(clazz)) {
            resolveMapping(result, type);
        }
        return result.isEmpty() ? Collections.emptyMap() : result;
    }

    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            // cn.ponfee.commons.tree.NodePath<java.lang.Integer>
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else {
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getActualTypeArgument(Type type, int genericArgsIndex) {
        Preconditions.checkArgument(genericArgsIndex >= 0, "Generic args index cannot be negative.");
        if (!(type instanceof ParameterizedType)) {
            return (Class<T>) Object.class;
        }

        Type[] types = ((ParameterizedType) type).getActualTypeArguments();
        return genericArgsIndex >= types.length
             ? (Class<T>) Object.class
             : getActualType(null, types[genericArgsIndex]);
    }

    // -------------------------------------------------------------------private methods

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getActualType(Class<?> clazz, Type type) {
        if (type instanceof Class<?>) {
            // private String name;
            return (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            // public class Sup<E> {
            //   private List<E>      list1; -> java.util.List
            //   private List<String> list2; -> java.util.List
            // }
            return getActualType(clazz, ((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            // private E[] array;
            // E: element type
            Type etype = ((GenericArrayType) type).getGenericComponentType();
            return (Class<T>) Array.newInstance(getActualType(clazz, etype), 0).getClass();
        } else if (type instanceof TypeVariable) {
            // public class Sup<E> { private E id; }
            // public class Sub extends Sup<Long> {}
            return getVariableActualType(clazz, (TypeVariable<?>) type);
        } else if (type instanceof WildcardType) {
            WildcardType wtype = (WildcardType) type;
            if (ArrayUtils.isNotEmpty(wtype.getLowerBounds())) {
                // 下限List<? super A>
                return getActualType(clazz, wtype.getLowerBounds()[0]);
            } else if (ArrayUtils.isNotEmpty(wtype.getUpperBounds())) {
                // 上限List<? extends A>
                return getActualType(clazz, wtype.getUpperBounds()[0]);
            } else {
                // List<?>
                return (Class<T>) Object.class;
            }
        } else {
            return (Class<T>) Object.class;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getVariableActualType(Class<?> clazz, TypeVariable<?> tv) {
        if (clazz == null) {
            return (Class<T>) Object.class;
        }

        return (Class<T>) VARIABLE_TYPE_MAPPING
            .computeIfAbsent(clazz, GenericUtils::getActualTypeVariableMapping)
            .getOrDefault(getTypeVariableName(null, tv).get(0), Object.class);
    }

    private static void resolveMapping(Map<String, Class<?>> result, Type type) {
        if (!(type instanceof ParameterizedType)) {
            return;
        }

        ParameterizedType ptype = (ParameterizedType) type;
        Class<?> rawType = (Class<?>) ptype.getRawType();
        // (Map<U,V>).getRawType() -> Map
        TypeVariable<?>[] vars = rawType.getTypeParameters();
        Type[] acts = ptype.getActualTypeArguments();
        for (int i = 0; i < acts.length; i++) {
            Class<?> varType = getActualType(null, acts[i]);
            getTypeVariableName(rawType, vars[i]).forEach(e -> result.put(e, varType));
            resolveMapping(result, acts[i]);
        }
    }

    private static List<String> getTypeVariableName(Class<?> clazz, TypeVariable<?> tv) {
        List<String> names = new ArrayList<>();
        getTypeVariableName(names, clazz, tv);
        return names;
    }

    private static void getTypeVariableName(List<String> names, Class<?> clazz, TypeVariable<?> tv) {
        // Class, Method, Constructor
        names.add(tv.getGenericDeclaration().toString() + "[" + tv.getName() + "]");
        if (clazz == null || clazz == Object.class) {
            return;
        }
        for (Type type : getGenericTypes(clazz)) {
            if (!(type instanceof ParameterizedType)) {
                continue;
            }

            ParameterizedType ptype = (ParameterizedType) type;
            Type[] types = ptype.getActualTypeArguments();
            if (ArrayUtils.isEmpty(types)) {
                continue;
            }

            for (int i = 0; i < types.length; i++) {
                if (!(types[i] instanceof TypeVariable<?>)) {
                    continue;
                }
                // find the type variable origin defined class
                if (((TypeVariable<?>) types[i]).getName().equals(tv.getTypeName())) {
                    clazz = (Class<?>) ptype.getRawType();
                    getTypeVariableName(names, clazz, clazz.getTypeParameters()[i]);
                    break;
                }
            }
        }
    }

}
