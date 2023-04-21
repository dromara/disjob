/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.util;

import cn.ponfee.scheduler.common.base.ArrayHashKey;
import cn.ponfee.scheduler.common.base.Null;
import cn.ponfee.scheduler.common.tuple.Tuple2;
import cn.ponfee.scheduler.common.tuple.Tuple3;
import com.google.common.base.Joiner;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.objenesis.ObjenesisHelper;
import org.springframework.util.Assert;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class utility
 *
 * @author Ponfee
 */
public final class ClassUtils {

    public static final Pattern QUALIFIED_CLASS_NAME_PATTERN = Pattern.compile("^([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*$");

    private static final Map<Object, Constructor<?>> CONSTRUCTOR_CACHE = new HashMap<>();
    private static final Map<Object, Method> METHOD_CACHE = new HashMap<>();
    private static final Map<String, Class<?>> CLASS_CACHE = new HashMap<>();

    private static final GroovyClassLoader GROOVY_CLASS_LOADER = new GroovyClassLoader();

    /**
     * Returns class object for text, can be class qualifier name or source code
     *
     * @param text the class qualifier name or source code
     * @return class object
     */
    public static <T> Class<T> getClass(String text) {
        String key = DigestUtils.md5Hex(text);
        Class<?> clazz = SynchronizedCaches.get(key, CLASS_CACHE, () -> {
            if (QUALIFIED_CLASS_NAME_PATTERN.matcher(text).matches()) {
                try {
                    return Class.forName(text);
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
            try {
                return GROOVY_CLASS_LOADER.parseClass(text);
            } catch (Exception ignored) {
                ignored.printStackTrace();
                return Null.class;
            }
        });
        return clazz == Null.class ? null : (Class<T>) clazz;
    }

    /**
     * Returns the member field(include super class)
     *
     * @param clazz the type
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
        throw new RuntimeException(firstOccurException);
    }

    /**
     * Returns member field list include super class(exclude transient field)
     *
     * @param clazz the class
     * @return a list filled fields
     */
    public static List<Field> listFields(Class<?> clazz) {
        if (clazz.isInterface() || clazz == Object.class) {
            return null; // error class args
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
     * Returns the static field, find in class pointer chain
     *
     * @param clazz the clazz
     * @param staticFieldName the static field name
     * @return static field object
     */
    public static Tuple2<Class<?>, Field> getStaticFieldInClassChain(Class<?> clazz, String staticFieldName) {
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
                        return Tuple2.of(type, field);
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
        throw new RuntimeException(firstOccurException);
    }

    /**
     * Returns the static field
     *
     * @param clazz the clazz
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
                throw new RuntimeException("Non-static field " + getName(clazz) + "#" + staticFieldName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取类名称<p>
     * ClassUtils.getClassName(ClassUtils.class)  ->  code.ponfee.commons.reflect.ClassUtils
     *
     * @param clazz the class
     * @return class full name
     */
    public static String getName(Class<?> clazz) {
        String name = clazz.getCanonicalName();
        if (name == null) {
            name = clazz.getName();
        }

        return name;
    }

    /**
     * 包名称转目录路径名<p>
     * getPackagePath("code.ponfee.commons.reflect")  ->  code/ponfee/commons/reflect
     *
     * @param packageName the package name
     * @return
     * @see org.springframework.util.ClassUtils#convertClassNameToResourcePath
     */
    public static String getPackagePath(String packageName) {
        return packageName.replace('.', '/') + "/";
    }

    /**
     * 包名称转目录路径名<p>
     * ClassUtils.getPackagePath(ClassUtils.class)  ->  code/ponfee/commons/reflect
     *
     * @param clazz the class
     * @return spec class file path
     */
    public static String getPackagePath(Class<?> clazz) {
        String className = getName(clazz);
        if (className.indexOf('.') < 0) {
            return ""; // none package name
        }

        return getPackagePath(className.substring(0, className.lastIndexOf('.')));
    }

    // -----------------------------------------------------------------------------constructor & instance
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getConstructor(Class<T> type, Class<?>... parameterTypes) {
        boolean noArgs = ArrayUtils.isEmpty(parameterTypes);
        Object key = noArgs ? type : Tuple2.of(type, ArrayHashKey.of((Object[]) parameterTypes));
        Constructor<T> constructor = (Constructor<T>) SynchronizedCaches.get(key, CONSTRUCTOR_CACHE, () -> {
            try {
                return getConstructor0(type, parameterTypes);
            } catch (Exception ignored) {
                // No such constructor, use placeholder
                return Null.BROKEN_CONSTRUCTOR;
            }
        });
        return constructor == Null.BROKEN_CONSTRUCTOR ? null : constructor;
    }

    public static <T> T newInstance(Constructor<T> constructor) {
        return newInstance(constructor, null);
    }

    public static <T> T newInstance(Constructor<T> constructor, Object[] args) {
        checkObjectArray(args);

        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        try {
            return ArrayUtils.isEmpty(args) ? constructor.newInstance() : constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T newInstance(Class<T> type, Class<?>[] parameterTypes, Object[] args) {
        checkObjectArray(args);
        checkSameLength(parameterTypes, args);
        if (ArrayUtils.isEmpty(parameterTypes)) {
            // no args constructor
            return newInstance(type, null);
        }

        Constructor<T> constructor = getConstructor(type, parameterTypes);
        if (constructor == null) {
            throw new RuntimeException("No such constructor: " + getName(type) + toString(parameterTypes));
        }
        return newInstance(constructor, args);
    }

    public static <T> T newInstance(Class<T> type) {
        return newInstance(type, null);
    }

    /**
     * 泛型参数的构造函数需要使用 {{@link #newInstance(Class, Class[], Object[])}} <p>
     * ClassUtils.newInstance(Tuple3.class, new Object[]{1, 2, 3}) <p>
     * ClassUtils.newInstance(Tuple2.class, new Object[]{new String[]{"a", "b"}, new Integer[]{1, 2}}) <p>
     *
     * @param type the type
     * @param args the args
     * @param <T>
     * @return
     */
    public static <T> T newInstance(Class<T> type, Object[] args) {
        checkObjectArray(args);
        if (ArrayUtils.isEmpty(args)) {
            Constructor<T> constructor = getConstructor(type);
            return constructor != null ? newInstance(constructor, null) : ObjenesisHelper.newInstance(type);
        }

        Class<?>[] parameterTypes = parseParameterTypes(args);
        Constructor<T> constructor = obtainConstructor(type, parameterTypes);
        if (constructor == null) {
            throw new RuntimeException("Not found constructor: " + getName(type) + toString(parameterTypes));
        }
        return newInstance(constructor, args);
    }

    // -------------------------------------------------------------------------------------------method & invoke
    public static Method getMethod(Object caller, String methodName, Class<?>... parameterTypes) {
        Tuple2<Class<?>, Predicates> tuple = obtainClass(caller);
        Class<?> type = tuple.a;
        boolean noArgs = ArrayUtils.isEmpty(parameterTypes);
        Object key = noArgs ? Tuple2.of(type, methodName) : Tuple3.of(type, methodName, ArrayHashKey.of((Object[]) parameterTypes));
        Method method = SynchronizedCaches.get(key, METHOD_CACHE, () -> {
            try {
                Method m = getMethod0(type, methodName, parameterTypes);
                return (tuple.b.equals(Modifier.isStatic(m.getModifiers())) && !m.isSynthetic()) ? m : null;
            } catch (Exception ignored) {
                // No such method, use placeholder
                return Null.BROKEN_METHOD;
            }
        });
        return method == Null.BROKEN_METHOD ? null : method;
    }

    public static <T> T invoke(Object caller, Method method) {
        return invoke(caller, method, null);
    }

    public static <T> T invoke(Object caller, Method method, Object[] args) {
        checkObjectArray(args);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        try {
            return (T) (ArrayUtils.isEmpty(args) ? method.invoke(caller) : method.invoke(caller, args));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T invoke(Object caller, String methodName) {
        return invoke(caller, methodName, null, null);
    }

    public static <T> T invoke(Object caller, String methodName, Class<?>[] parameterTypes, Object[] args) {
        checkObjectArray(args);
        checkSameLength(parameterTypes, args);
        Method method = getMethod(caller, methodName, parameterTypes);
        if (method == null) {
            throw new RuntimeException(
                "No such method: " + getName(caller.getClass()) + "#" + methodName + toString(parameterTypes)
            );
        }
        return invoke(caller, method, args);
    }

    public static <T> T invoke(Object caller, String methodName, Object[] args) {
        checkObjectArray(args);
        if (ArrayUtils.isEmpty(args)) {
            return invoke(caller, methodName, null, null);
        }

        Class<?>[] parameterTypes = parseParameterTypes(args);
        Method method = obtainMethod(caller, methodName, parameterTypes);
        if (method == null) {
            Class<?> clazz = (caller instanceof Class<?>) ? (Class<?>) caller : caller.getClass();
            throw new RuntimeException("Not found method: " + getName(clazz) + "#" + methodName + toString(parameterTypes));
        }
        return invoke(caller, method, args);
    }

    public static Tuple2<Class<?>, Predicates> obtainClass(Object obj) {
        if (obj instanceof Class<?> && obj != Class.class) {
            // 静态方法
            // 普通Class类实例(如String.class)：只处理其所表示类的静态方法，如“String.valueOf(1)”。不支持Class类中的实例方法，如“String.class.getName()”
            return Tuple2.of((Class<?>) obj, Predicates.Y);
        } else {
            // 实例方法
            // 对于Class.class对象：只处理Class类中的实例方法，如“Class.class.getName()”。不支持Class类中的静态方法，如“Class.forName("code.ponfee.commons.base.tuple.Tuple0")”
            return Tuple2.of(obj.getClass(), Predicates.N);
        }
    }

    /**
     * 获取类文件的路径（文件）
     *
     * @param clazz the class
     * @return spec class file path
     */
    public static String getClassFilePath(Class<?> clazz) {
        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        String path = new File(URLCodes.decodeURI(url.getPath(), Files.UTF_8)).getAbsolutePath();

        if (path.toLowerCase().endsWith(".jar")) {
            path += "!";
        }
        return path + File.separator + getName(clazz).replace('.', File.separatorChar) + ".class";
    }

    /**
     * 获取指定类的类路径（目录）
     *
     * @param clazz the class
     * @return spec classpath
     */
    public static String getClasspath(Class<?> clazz) {
        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        String path = URLCodes.decodeURI(url.getPath(), Files.UTF_8);
        if (path.toLowerCase().endsWith(".jar")) {
            path = path.substring(0, path.lastIndexOf("/") + 1);
        }
        return new File(path).getAbsolutePath() + File.separator;
    }

    /**
     * 获取当前的类路径（目录）
     *
     * @return current main classpath
     */
    public static String getClasspath() {
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        path = URLCodes.decodeURI(new File(path).getAbsolutePath(), Files.UTF_8);
        return path + File.separator;
    }

    // -------------------------------------------------------------------------------------------private methods

    private static void checkSameLength(Object[] a, Object[] b) {
        if (ArrayUtils.isEmpty(a) && ArrayUtils.isEmpty(b)) {
            return;
        }
        if (a.length != b.length) {
            throw new RuntimeException("Two array are different length: " + a.length + ", " + b.length);
        }
    }

    private static void checkObjectArray(Object[] array) {
        if (array != null && array.getClass() != Object[].class) {
            throw new RuntimeException("Args must Object[] type, but actual is " + array.getClass().getSimpleName());
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
            boolean matches = method.getName().equals(methodName)
                && !method.isSynthetic()
                && flag.equals(Modifier.isStatic(method.getModifiers()))
                && matches(method.getParameterTypes(), actualTypes);
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
     * @param actualTypes 调用方法实际传入的参数类型
     * @return
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
