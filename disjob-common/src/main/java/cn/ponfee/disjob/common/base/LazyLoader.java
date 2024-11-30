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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<T> holder;

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

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        return holder().orElseThrow(exceptionSupplier);
    }

    public T orElseGet(Supplier<? extends T> other) {
        return holder().orElseGet(other);
    }

    public void ifPresent(Consumer<? super T> consumer) {
        holder().ifPresent(consumer);
    }

    // ------------------------------------------------------------------------private methods

    @SuppressWarnings("OptionalAssignedToNull")
    private Optional<T> holder() {
        if (holder == null) {
            holder = Optional.ofNullable(loader.get());
        }
        return holder;
    }

    /**
     * <pre>
     * 注意：
     *   1）调用目标类的final方法时，此时的调用对象是代理对象，其所有成员变量都会是null
     *   2）用的是lazyLoader.get()，如果延时加载到null则会抛`NullPointerException("Not load target object.")`
     * </pre>
     *
     * @param type       目标类
     * @param lazyLoader 延时加载器
     * @param <T>        目标类型
     * @param <R>        代理对象(目标类的子类)
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    private static <T, R extends T> R of(Class<T> type, final LazyLoader<R> lazyLoader) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(type);
        //enhancer.setCallback(org.springframework.cglib.proxy.Proxy.getInvocationHandler(proxy)); // occur error
        //enhancer.setCallback((org.springframework.cglib.proxy.MethodInterceptor) (beanProxy, method, args, methodProxy) -> method.invoke(lazyLoader.get(), args));
        enhancer.setCallback((InvocationHandler) (beanProxy, method, args) -> method.invoke(lazyLoader.get(), args));
        return (R) enhancer.create();
    }

}
