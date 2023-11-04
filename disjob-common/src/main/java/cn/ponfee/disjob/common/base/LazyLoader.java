/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.base;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.InvocationHandler;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Lazy loader
 *
 * @author Ponfee
 */
public class LazyLoader<T> implements Supplier<T> {

    private final Supplier<T> loader;

    private Optional<T> holder = null;

    private LazyLoader(Supplier<T> loader) {
        this.loader = Objects.requireNonNull(loader);
    }

    public static <T> LazyLoader<T> of(Supplier<T> loader) {
        return new LazyLoader<>(loader);
    }

    public static <T, R extends T> R of(Class<T> type, Supplier<R> loader) {
        return of(type, of(loader));
    }

    public static <T, A> LazyLoader<T> of(Function<A, T> loader, A arg) {
        return new LazyLoader<>(() -> loader.apply(arg));
    }

    public static <T, A, R extends T> R of(Class<T> type, Function<A, R> loader, A arg) {
        return of(type, of(loader, arg));
    }

    @Override
    public T get() {
        return holder().orElseThrow(() -> new NullPointerException("Not load target object."));
    }

    public T orElse(T defaultValue) {
        return holder().orElse(defaultValue);
    }

    public T orElseGet(Supplier<? extends T> other) {
        return holder().orElseGet(other);
    }

    public void ifPresent(Consumer<? super T> consumer) {
        holder().ifPresent(consumer);
    }

    // ------------------------------------------------------------------------private methods

    private Optional<T> holder() {
        if (holder == null) {
            holder = Optional.ofNullable(loader.get());
        }
        return holder;
    }

    /**
     * 注意：
     *   1、调用目标类的final方法时，此时的调用对象是代理对象，其所有成员变量都会是null
     *   2、用的是lazyLoader.get()，如果延时加载到null则会抛`NullPointerException("Not load target object.")`
     *
     * @param type       目标类
     * @param lazyLoader 延时加载器
     * @param <T>        目标类型
     * @param <R>        代理对象(目标类的子类)
     * @return 代理对象
     */
    private static <T, R extends T> R of(Class<T> type, final LazyLoader<R> lazyLoader) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(type);
        enhancer.setUseCache(true);
        enhancer.setInterceptDuringConstruction(false);
        //enhancer.setCallback(org.springframework.cglib.proxy.Proxy.getInvocationHandler(proxy)); // occur error
        //enhancer.setCallback((org.springframework.cglib.proxy.MethodInterceptor) (beanProxy, method, args, methodProxy) -> method.invoke(lazyLoader.get(), args));
        enhancer.setCallback((InvocationHandler) (beanProxy, method, args) -> method.invoke(lazyLoader.get(), args));
        return (R) enhancer.create();
    }

}
