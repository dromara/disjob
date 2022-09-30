package cn.ponfee.scheduler.common.base;

/**
 * Generate id
 *
 * @author Ponfee
 */
@FunctionalInterface
public interface IdGenerator {

    /**
     * Returns id.
     *
     * @return id long value
     */
    long generateId();
}
