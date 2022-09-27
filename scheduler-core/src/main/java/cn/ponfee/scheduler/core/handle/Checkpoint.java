package cn.ponfee.scheduler.core.handle;

/**
 * Save task execution snapshot
 *
 * @author Ponfee
 */
@FunctionalInterface
public interface Checkpoint {

    /**
     * Save the task execution snapshot
     *
     * @param taskId         the task id
     * @param executeSnapshot the execution snapshot data
     * @return {@code true} if saved successfully
     * @throws Exception if saved occur exception
     */
    boolean checkpoint(long taskId, String executeSnapshot) throws Exception;

    Checkpoint DISCARD = (taskId, executeSnapshot) -> true;
}
