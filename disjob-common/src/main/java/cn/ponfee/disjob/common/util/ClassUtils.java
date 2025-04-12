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

import cn.ponfee.disjob.common.base.Null;
import cn.ponfee.disjob.common.base.Symbol;
import cn.ponfee.disjob.common.collect.ArrayHashKey;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.tuple.Tuple2;
import cn.ponfee.disjob.common.tuple.Tuple3;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.objenesis.ObjenesisHelper;
import org.springframework.util.Assert;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Class utility
 *
 * @author Ponfee
 */
public final class ClassUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ClassUtils.class);

    /**
     * Constructor cache
     */
    private static final ConcurrentMap<Object, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * Method cache
     */
    private static final ConcurrentMap<Object, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * Returns the member field(include super class)
     *
     * @param clazz     the type
     * @param fieldName the field name
     * @return member field object
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (clazz.isInterface() || clazz == Object.class) {
            return null;
        }

        Exception firstOccurException = null;
        do {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                if (!Modifier.isStatic(field.getModifiers())) {
                    return field;
                }
            } catch (Exception e) {
                if (firstOccurException == null) {
                    firstOccurException = e;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null && clazz != Object.class);

        // not found
        return ExceptionUtils.rethrow(firstOccurException);
    }

    public static Set<String> fieldDiff(Class<?> a, Class<?> b) {
        Set<String> set1 = getFields(a).stream().map(Field::getName).collect(Collectors.toSet());
        Set<String> set2 = getFields(b).stream().map(Field::getName).collect(Collectors.toSet());
        return Sets.symmetricDifference(set1, set2);
    }

    /**
     * Returns member field list include super class(exclude transient field)
     *
     * @param clazz the class
     * @return a list filled fields
     */
    public static List<Field> getFields(Class<?> clazz) {
        if (clazz.isInterface() || clazz == Object.class) {
            return Collections.emptyList();
        }

        List<Field> list = new ArrayList<>();
        do {
            try {
                for (Field field : clazz.getDeclaredFields()) {
                    int mdf = field.getModifiers();
                    if (!Modifier.isStatic(mdf) && !Modifier.isTransient(mdf)) {
                        list.add(field);
                    }
                }
            } catch (Exception ignored) {
                // ignored
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null && clazz != Object.class);

        return list;
    }

    /**
     * Returns the static field, get in class pointer chain
     *
     * @param clazz           the clazz
     * @param staticFieldName the static field name
     * @return static field object
     */
    public static Field getStaticFieldIncludeSuperClass(Class<?> clazz, String staticFieldName) {
        if (clazz == Object.class) {
            return null;
        }

        Exception firstOccurException = null;
        Queue<Class<?>> queue = new LinkedList<>();
        queue.offer(clazz);
        while (!queue.isEmpty()) {
            for (int i = queue.size(); i > 0; i--) {
                Class<?> type = queue.poll();
                try {
                    Field field = type.getDeclaredField(staticFieldName);
                    if (Modifier.isStatic(field.getModifiers())) {
                        return field;
                    }
                } catch (Exception e) {
                    if (firstOccurException == null) {
                        firstOccurException = e;
                    }
                }
                // 可能是父类/父接口定义的属性（如：Tuple1.HASH_FACTOR，非继承，而是查找Class的指针链）
                if (type.getSuperclass() != Object.class) {
                    queue.offer(type.getSuperclass());
                }
                Arrays.stream(type.getInterfaces()).forEach(queue::offer);
            }
        }

        // not found
        return ExceptionUtils.rethrow(firstOccurException);
    }

    /**
     * Returns the static field
     *
     * @param clazz           the clazz
     * @param staticFieldName the static field name
     * @return static field object
     */
    public static Field getStaticField(Class<?> clazz, String staticFieldName) {
        if (clazz == Object.class) {
            return null;
        }
        try {
            Field field = clazz.getDeclaredField(staticFieldName);
            if (Modifier.isStatic(field.getModifiers())) {
                return field;
            } else {
                throw new IllegalArgumentException("Non-static field " + clazz + "#" + staticFieldName);
            }
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static String getObjectClassName(Object obj) {
        return obj == null ? null : obj.getClass().getName();
    }

    public static Class<?> findAnnotatedClass(Class<?> supClass, Class<?> subClass, Class<? extends Annotation> annClass) {
        if (supClass == null || subClass == null || !supClass.isAssignableFrom(subClass)) {
            return null;
        }
        Deque<Class<?>> stack = Collects.newArrayDeque(subClass);
        while (!stack.isEmpty()) {
            subClass = stack.pop();
            if (subClass.isAnnotationPresent(annClass)) {
                return subClass;
            }
            for (Class<?> cls : Collects.concat(subClass.getInterfaces(), subClass.getSuperclass())) {
                if (cls != null && supClass.isAssignableFrom(cls)) {
                    stack.push(cls);
                }
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------------------------package path & class path

    /**
     * 包名称转目录路径名<p>
     * getPackagePath("cn.ponfee.commons.reflect")  ->  cn/ponfee/commons/reflect
     *
     * @param packageName the package name
     * @return package name
     * @see org.springframework.util.ClassUtils#convertClassNameToResourcePath
     */
    public static String getPackagePath(String packageName) {
        return packageName.replace('.', '/');
    }

    /**
     * 包名称转目录路径名<p>
     * ClassUtils.getPackagePath(ClassUtils.class)  ->  code/ponfee/commons/reflect
     *
     * @param clazz the class
     * @return spec class file path
     */
    public static String getPackagePath(Class<?> clazz) {
        String className = clazz.getName();
        if (className.indexOf('.') < 0) {
            return ""; // none package name
        }
        return getPackagePath(className.substring(0, className.lastIndexOf('.')));
    }

    /**
     * 获取类文件的路径（文件）
     *
     * @param clazz the class
     * @return spec class file path
     */
    public static String getClassFilePath(Class<?> clazz) {
        String path = Files.toFile(clazz.getProtectionDomain().getCodeSource().getLocation()).getAbsolutePath();
        if (path.toLowerCase().endsWith(".jar")) {
            path += "!";
        }
        return path + File.separator + clazz.getName().replace('.', File.separatorChar) + ".class";
    }

    /**
     * 获取指定类的类路径（目录）
     *
     * @param clazz the class
     * @return spec classpath
     */
    public static String getClasspath(Class<?> clazz) {
        String path = Files.toFile(clazz.getProtectionDomain().getCodeSource().getLocation()).getAbsolutePath();
        if (path.toLowerCase().endsWith(".jar")) {
            path = path.substring(0, path.lastIndexOf(File.separator));
        }
        return path + File.separator;
    }

    /**
     * 获取当前的类路径（目录）
     *
     * @return current main classpath
     */
    public static String getClasspath() {
        URL url = Thread.currentThread().getContextClassLoader().getResource("");
        return url == null ? null : Files.toFile(url).getAbsolutePath() + File.separator;
    }

    // -----------------------------------------------------------------------------constructor & newInstance

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getConstructor(Class<T> type, Class<?>... parameterTypes) {
        Object key = (parameterTypes.length == 0) ? type : Tuple2.of(type, ArrayHashKey.of((Object[]) parameterTypes));
        Constructor<T> constructor = (Constructor<T>) CONSTRUCTOR_CACHE.computeIfAbsent(key, k -> {
            try {
                return getConstructor0(type, parameterTypes);
            } catch (Exception e) {
                // No such constructor, use placeholder
                LOG.warn("Get constructor occur error: {}", e.getMessage());
                return Null.BROKEN_CONSTRUCTOR;
            }
        });
        return constructor == Null.BROKEN_CONSTRUCTOR ? null : constructor;
    }

    public static <T> T newInstance(Constructor<T> constructor, Object... args) {
        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        try {
            return constructor.newInstance(args);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * ClassUtils.newInstance(Tuple3.class, 1, 2, 3) <br/>
     * ClassUtils.newInstance(Tuple2.class, new String[]{"a", "b"}, new Integer[]{1, 2}) <br/>
     *
     * @param type the class
     * @param args the args
     * @param <T>  class type
     * @return instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> type, Object... args) {
        if (args.length == 0) {
            if (Map.class == type) {
                return (T) new HashMap<>(8);
            }
            if (Set.class == type) {
                return (T) new HashSet<>();
            }
            if (Collection.class == type || List.class == type) {
                return (T) new ArrayList<>();
            }
            if (Dictionary.class == type) {
                return (T) new Hashtable<>();
            }
            if (boolean.class == type || Boolean.class == type) {
                return (T) Boolean.FALSE;
            }
            if (char.class == type || Character.class == type) {
                return (T) (Character) Symbol.Char.ZERO;
            }
            if (type.isPrimitive() || PrimitiveTypes.isWrapperType(type)) {
                Class<?> wrapper = PrimitiveTypes.ofPrimitiveOrWrapper(type).wrapper();
                return (T) newInstance(wrapper, new Class<?>[]{String.class}, new Object[]{"0"});
            }

            Constructor<T> constructor = getConstructor(type);
            return constructor != null ? newInstance(constructor) : ObjenesisHelper.newInstance(type);
        }

        Class<?>[] parameterTypes = parseParameterTypes(args);
        Constructor<T> constructor = obtainConstructor(type, parameterTypes);
        if (constructor == null) {
            throw new IllegalArgumentException("Not found constructor: " + type + toString(parameterTypes));
        }
        return newInstance(constructor, args);
    }

    public static <T> T newInstance(Class<T> type, Class<?>[] parameterTypes, Object[] args) {
        Assert.isTrue(parameterTypes.length == args.length, "Inconsistent constructor parameter count.");
        Constructor<T> constructor = getConstructor(type, parameterTypes);
        if (constructor == null) {
            throw new IllegalArgumentException("No such constructor: " + type + toString(parameterTypes));
        }
        return newInstance(constructor, args);
    }

    // -------------------------------------------------------------------------------------------method & invoke

    public static Method getMethod(Object caller, String methodName, Class<?>... parameterTypes) {
        Tuple2<Class<?>, Predicates> tuple = obtainClass(caller);
        Class<?> type = tuple.a;
        Object key = (parameterTypes.length == 0) ?
            Tuple2.of(type, methodName) : Tuple3.of(type, methodName, ArrayHashKey.of((Object[]) parameterTypes));
        Method method = METHOD_CACHE.computeIfAbsent(key, k -> {
            try {
                Method m = getMethod0(type, methodName, parameterTypes);
                return (tuple.b.equals(Modifier.isStatic(m.getModifiers())) && !m.isSynthetic()) ? m : null;
            } catch (Exception e) {
                // No such method, use placeholder
                LOG.info("Get method failed: {}", e.getMessage());
                return Null.BROKEN_METHOD;
            }
        });
        return method == Null.BROKEN_METHOD ? null : method;
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object caller, Method method, Object... args) {
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return (T) method.invoke(caller, args);
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static <T> T invoke(Object caller, String methodName, Object... args) {
        Class<?>[] parameterTypes = parseParameterTypes(args);
        Method method = obtainMethod(caller, methodName, parameterTypes);
        if (method == null) {
            Class<?> clazz = (caller instanceof Class<?>) ? (Class<?>) caller : caller.getClass();
            throw new IllegalArgumentException("Not found method: " + clazz + "#" + methodName + toString(parameterTypes));
        }
        return invoke(caller, method, args);
    }

    public static <T> T invoke(Object caller, String methodName, Class<?>[] parameterTypes, Object[] args) {
        Assert.isTrue(parameterTypes.length == args.length, "Inconsistent method parameter count.");
        Method method = getMethod(caller, methodName, parameterTypes);
        if (method == null) {
            throw new IllegalArgumentException("No such method: " + caller.getClass() + "#" + methodName + toString(parameterTypes));
        }
        return invoke(caller, method, args);
    }

    // -------------------------------------------------------------------------------------------private methods

    private static Tuple2<Class<?>, Predicates> obtainClass(Object obj) {
        if (obj instanceof Class<?> && obj != Class.class) {
            // 静态方法
            // 普通Class类实例(如String.class)：只处理其所表示类的静态方法，如“String.valueOf(1)”。不支持Class类中的实例方法，如“String.class.getName()”
            return Tuple2.of((Class<?>) obj, Predicates.Y);
        } else {
            // 实例方法
            // 对于Class.class对象：只处理Class类中的实例方法，如“Class.class.getName()”。不支持Class类中的静态方法，如“Class.forName("cn.ponfee.commons.base.tuple.Tuple0")”
            return Tuple2.of(obj.getClass(), Predicates.N);
        }
    }

    private static Class<?>[] parseParameterTypes(Object[] args) {
        Assert.notEmpty(args, "Should be always non empty.");
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0, n = args.length; i < n; i++) {
            parameterTypes[i] = (args[i] == null) ? null : args[i].getClass();
        }
        return parameterTypes;
    }

    private static Method getMethod0(Class<?> type, String methodName, Class<?>[] parameterTypes) throws Exception {
        try {
            return type.getMethod(methodName, parameterTypes);
        } catch (Exception e) {
            try {
                return type.getDeclaredMethod(methodName, parameterTypes);
            } catch (Exception ignored) {
                // ignored
            }
            throw e;
        }
    }

    private static <T> Constructor<T> getConstructor0(Class<T> type, Class<?>[] parameterTypes) throws Exception {
        try {
            return type.getConstructor(parameterTypes);
        } catch (Exception e) {
            try {
                return type.getDeclaredConstructor(parameterTypes);
            } catch (Exception ignored) {
                // ignored
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> obtainConstructor(Class<T> type, Class<?>[] actualTypes) {
        Assert.notEmpty(actualTypes, "Should be always non empty.");
        Constructor<T> constructor = obtainConstructor((Constructor<T>[]) type.getConstructors(), actualTypes);
        if (constructor != null) {
            return constructor;
        }
        return obtainConstructor((Constructor<T>[]) type.getDeclaredConstructors(), actualTypes);
    }

    private static <T> Constructor<T> obtainConstructor(Constructor<T>[] constructors, Class<?>[] actualTypes) {
        if (ArrayUtils.isEmpty(constructors)) {
            return null;
        }
        for (Constructor<T> constructor : constructors) {
            if (matches(constructor.getParameterTypes(), actualTypes)) {
                return constructor;
            }
        }
        return null;
    }

    private static Method obtainMethod(Object caller, String methodName, Class<?>[] actualTypes) {
        Assert.notEmpty(actualTypes, "Should be always non empty.");
        Tuple2<Class<?>, Predicates> tuple = obtainClass(caller);
        Method method = obtainMethod(tuple.a.getMethods(), methodName, tuple.b, actualTypes);
        if (method != null) {
            return method;
        }
        return obtainMethod(tuple.a.getDeclaredMethods(), methodName, tuple.b, actualTypes);
    }

    private static Method obtainMethod(Method[] methods, String methodName,
                                       Predicates flag, Class<?>[] actualTypes) {
        if (ArrayUtils.isEmpty(methods)) {
            return null;
        }
        for (Method method : methods) {
            boolean matches = method.getName().equals(methodName) &&
                !method.isSynthetic() &&
                flag.equals(Modifier.isStatic(method.getModifiers())) &&
                matches(method.getParameterTypes(), actualTypes);
            if (matches) {
                return method;
            }
        }
        return null;
    }

    /**
     * 方法匹配
     *
     * @param definedTypes 方法体中定义的参数类型
     * @param actualTypes  调用方法实际传入的参数类型
     * @return boolean
     */
    private static boolean matches(Class<?>[] definedTypes, Class<?>[] actualTypes) {
        if (definedTypes.length != actualTypes.length) {
            return false;
        }
        for (int i = 0, n = definedTypes.length; i < n; i++) {
            Class<?> definedType = definedTypes[i], actualType = actualTypes[i];
            if (definedType.isPrimitive()) {
                // 方法参数为基本数据类型
                PrimitiveTypes ept = PrimitiveTypes.ofPrimitive(definedType);
                PrimitiveTypes apt = PrimitiveTypes.ofPrimitiveOrWrapper(actualType);
                if (apt == null || !apt.isCastable(ept)) {
                    return false;
                }
            } else if (actualType != null && !definedType.isAssignableFrom(actualType)) {
                // actualType为空则可转任何对象类型（非基本数据类型）
                return false;
            }
        }
        return true;
    }

    private static String toString(Class<?>[] parameterTypes) {
        return ArrayUtils.isEmpty(parameterTypes)
            ? "()"
            : "(" + Joiner.on(", ").join(parameterTypes) + ")";
    }

}
