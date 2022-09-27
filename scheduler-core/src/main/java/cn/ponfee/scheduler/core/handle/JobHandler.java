package cn.ponfee.scheduler.core.handle;

/**
 * Schedule job handler base class.
 *
 * <p>Note: if in spring context and a stateful bean, must be annotated with @Scope("prototype")
 *
 * @author Ponfee
 * @see org.springframework.context.annotation.Scope
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#SCOPE_PROTOTYPE
 */
public abstract class JobHandler<T> extends TaskExecutor<T> implements JobSplitter {

}
