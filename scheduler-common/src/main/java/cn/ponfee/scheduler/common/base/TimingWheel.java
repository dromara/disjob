/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.base;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Timing wheel structure.
 *
 * @author Ponfee
 */
public abstract class TimingWheel<T extends TimingWheel.Timing<T>> implements java.io.Serializable {
    private static final long serialVersionUID = 4500377208898808026L;

    /**
     * Millis number of per second.
     */
    private static final int MILLIS_PER_SECOND = 1000;

    /**
     * Second number of per minute.
     */
    private static final int SECONDS_PER_MINUTE = 60;

    /**
     * Millis number of per minute.
     */
    private static final int MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE;

    private final TimingQueue<T>[] ringBuffer;

    public TimingWheel() {
        this(11);
    }

    public TimingWheel(int priorityQueueInitialCapacity) {
        TimingQueue<T>[] array = (TimingQueue<T>[]) new TimingQueue[SECONDS_PER_MINUTE];
        // initialize 0 ~ 59 slots
        for (int i = 0; i < SECONDS_PER_MINUTE; i++) {
            array[i] = new TimingQueue<>(priorityQueueInitialCapacity);
        }
        this.ringBuffer = array;
    }

    /**
     * Verifies the timing data
     *
     * @param timing the timing data
     * @return if {@code true} verify success
     */
    protected boolean verify(T timing) {
        return true;
    }

    public final boolean offer(T timing) {
        // 毫秒在[000 ~ 900]放入下一个刻度
        // 毫秒在[901 ~ 999]放入下两个刻度
        return offer(timing, System.currentTimeMillis() + 1099);
    }

    /**
     * Puts to timing wheel.
     *
     * @param timing          the timing data
     * @param leastTimeMillis the least time millis
     * @return if {@code true} put success
     */
    public final boolean offer(T timing, long leastTimeMillis) {
        if (!verify(timing)) {
            return false;
        }

        // 如果小于leastTimeMillis，则放入leastTimeMillis所在的槽位
        long slotTimeMillis = Math.max(timing.timing(), leastTimeMillis);
        int ringSecond = secondOfMinute(slotTimeMillis);
        return ringBuffer[ringSecond].offer(timing);
    }

    public final List<T> poll() {
        return poll(System.currentTimeMillis());
    }

    /**
     * Gets from timing wheel.
     *
     * @param latestTimeMillis the latest time millis
     * @return list of Timing
     */
    public final List<T> poll(long latestTimeMillis) {
        List<T> ringTrigger = new ArrayList<>();
        int ringSecond = secondOfMinute(latestTimeMillis);
        long maximumTiming = (latestTimeMillis / MILLIS_PER_SECOND) * MILLIS_PER_SECOND + 999;
        // process current and previous tick timingQueue
        for (int i = 0; i < 2; i++) {
            TimingQueue<T> ringTick = ringBuffer[(ringSecond - i + SECONDS_PER_MINUTE) % SECONDS_PER_MINUTE];
            T first;
            while ((first = ringTick.peek()) != null && first.timing() <= maximumTiming) {
                first = ringTick.poll();

                // if run in single thread, there code block unnecessary
                if (first == null) {
                    break;
                }
                if (first.timing() > maximumTiming) {
                    ringTick.offer(first);
                    break;
                }

                ringTrigger.add(first);
            }
        }
        return ringTrigger;
    }

    private static int secondOfMinute(long timeMillis) {
        return (int) ((timeMillis % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND);
    }

    /**
     * Timing of TimingWheel elements
     */
    public interface Timing<T extends Timing<T>> extends Comparable<T> {
        /**
         * Returns millis timestamp
         *
         * @return millis timestamp
         */
        long timing();

        /**
         * Provides default compare
         *
         * @param other the other
         * @return the value 0 if this == other; a value less than 0 if this < other; and a value greater than 0 if this > other
         */
        @Override
        default int compareTo(T other) {
            return Long.compare(this.timing(), other.timing());
        }
    }

    /*
    public static final class TimingQueue<T extends Timing<T>> extends PriorityBlockingQueue<T> {
        public TimingQueue() {
            super();
        }

        public TimingQueue(int initialCapacity) {
            super(initialCapacity);
        }
    }
    */

    /**
     * Timing queue
     *
     * @param <T> element type
     */
    public static final class TimingQueue<T extends Timing<T>> {
        private final PriorityQueue<T> queue;

        public TimingQueue() {
            this.queue = new PriorityQueue<>();
        }

        public TimingQueue(int initialCapacity) {
            this.queue = new PriorityQueue<>(initialCapacity);
        }

        public synchronized T poll() {
            return queue.poll();
        }

        public synchronized boolean offer(T timing) {
            return queue.offer(timing);
        }

        // -------------------------------------------Unnecessary use synchronized

        /**
         * Returns the top element of heap data structure.
         *
         * @return Timing data
         */
        public T peek() {
            return queue.peek();
        }

        public int size() {
            return queue.size();
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }

}
