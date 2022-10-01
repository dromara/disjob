package cn.ponfee.scheduler.common.base;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.InvocationHandler;

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
    private Optional<T> holder;

    private LazyLoader(Supplier<T> loader) {
        this.loader = loader;
    }

    public static <T> LazyLoader<T> of(Supplier<T> loader) {
        return new LazyLoader<>(loader);
    }

    public static <T, B extends T, C extends T> B of(Class<T> type, Supplier<C> loader) {
        return of(type, of(loader));
    }

    public static <T, A> LazyLoader<T> of(Function<A, T> loader, A arg) {
        return new LazyLoader<>(() -> loader.apply(arg));
    }

    public static <T, A, B extends T, C extends T> B of(Class<T> type, Function<A, C> loader, A arg) {
        return of(type, of(loader, arg));
    }

    @Override
    public T get() {
        lazyLoad();
        return holder.get();
    }

    public void orElse(T defaultValue) {
        lazyLoad();
        holder.orElse(defaultValue);
    }

    public void ifPresent(Consumer<? super T> consumer) {
        lazyLoad();
        holder.ifPresent(consumer);
    }

    // ------------------------------------------------------------------------private methods
    private void lazyLoad() {
        if (holder == null) {
            holder = Optional.ofNullable(loader.get());
        }
    }

    private static <T, B extends T, C extends T> B of(Class<T> type, final LazyLoader<C> lazyLoader) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(type);
        enhancer.setUseCache(true);
        enhancer.setInterceptDuringConstruction(false);
        //enhancer.setCallback(Proxy.getInvocationHandler(lazyLoader.get())); // Error
        //enhancer.setCallback((MethodInterceptor) (beanProxy, method, args, methodProxy) -> method.invoke(lazyLoader.get(), args));
        enhancer.setCallback((InvocationHandler) (beanProxy, method, args) -> method.invoke(lazyLoader.get(), args));
        return (B) enhancer.create();
    }

}
