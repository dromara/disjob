/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.base;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Timing wheel structure.
 *
 * <p>TimingWheel(100, 60):
 * <pre>
 *   00.000[00, 59]
 *   00.100[01, 00]
 *   00.200[02, 01]
 *   00.300[03, 02]
 *   00.400[04, 03]
 *   00.500[05, 04]
 *   00.600[06, 05]
 *   00.700[07, 06]
 *   00.800[08, 07]
 *   00.900[09, 08]
 *   01.000[10, 09]
 *   01.100[11, 10]
 *   01.200[12, 11]
 *   01.300[13, 12]
 *   01.400[14, 13]
 *   01.500[15, 14]
 *   01.600[16, 15]
 *   01.700[17, 16]
 *   01.800[18, 17]
 *   01.900[19, 18]
 *   02.000[20, 19]
 *   02.120[21, 20]
 *   02.200[22, 21]
 *   02.300[23, 22]
 *   02.400[24, 23]
 *   02.500[25, 24]
 *   02.600[26, 25]
 *   02.700[27, 26]
 *   02.800[28, 27]
 *   02.900[29, 28]
 *   03.000[30, 29]
 *   03.100[31, 30]
 *   03.200[32, 31]
 *   03.300[33, 32]
 *   03.400[34, 33]
 *   03.500[35, 34]
 *   03.600[36, 35]
 *   03.700[37, 36]
 *   03.800[38, 37]
 *   03.900[39, 38]
 *   04.000[40, 39]
 *   04.100[41, 40]
 *   04.200[42, 41]
 *   04.300[43, 42]
 *   04.400[44, 43]
 *   04.500[45, 44]
 *   04.600[46, 45]
 *   04.700[47, 46]
 *   04.800[48, 47]
 *   04.900[49, 48]
 *   05.000[50, 49]
 *   05.100[51, 50]
 *   05.210[52, 51]
 *   05.300[53, 52]
 *   05.400[54, 53]
 *   05.500[55, 54]
 *   05.600[56, 55]
 *   05.700[57, 56]
 *   05.800[58, 57]
 *   05.900[59, 58]
 *
 *   06.000[00, 59]
 *   06.100[01, 00]
 *   06.200[02, 01]
 * </pre>
 *
 * @author Ponfee
 */
public abstract class TimingWheel<T extends TimingWheel.Timing<T>> implements java.io.Serializable {
    private static final long serialVersionUID = 4500377208898808026L;

    private static final int PROCESS_SLOTS_SIZE = 2;

    /**
     * Tick duration milliseconds
     */
    private final long tickMs;

    /**
     * A round duration milliseconds
     */
    private final long roundMs;

    /**
     * Ring buffer of wheel
     */
    private final TimingQueue<T>[] wheel;

    public TimingWheel(long tickMs, int ringSize) {
        Assert.isTrue(tickMs > 0, "Tick milliseconds must be greater than 0");
        Assert.isTrue(ringSize > 0, "Ring size must be greater than 0");
        this.tickMs = tickMs;
        this.roundMs = tickMs * ringSize;

        TimingQueue<T>[] ring = new TimingQueue[ringSize];
        // initialize 0 ~ ringSize slots
        for (int i = 0; i < ring.length; i++) {
            ring[i] = new TimingQueue<>();
        }
        this.wheel = ring;
    }

    public final long getTickMs() {
        return tickMs;
    }

    public final int getRingSize() {
        return wheel.length;
    }

    /**
     * Verifies the timing data
     *
     * @param timing the timing data
     * @return if {@code true} verify success
     */
    protected boolean verify(T timing) {
        return timing != null;
    }

    public final boolean offer(T timing) {
        // “+ tickMs”：如果小于当前时间(要立即触发)，则放入下一个刻度
        return offer(timing, System.currentTimeMillis() + tickMs);
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
        int ringIndex = calculateIndex(slotTimeMillis);
        return wheel[ringIndex].offer(timing);
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
        int ringIndex = calculateIndex(latestTimeMillis);
        long maximumTiming = (latestTimeMillis / tickMs) * tickMs + tickMs;
        // process current and previous tick timingQueue
        for (int i = 0; i < PROCESS_SLOTS_SIZE; i++) {
            TimingQueue<T> ringTick = wheel[(ringIndex - i + wheel.length) % wheel.length];
            T first;
            while ((first = ringTick.peek()) != null && first.timing() < maximumTiming) {
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

    private int calculateIndex(long timeMillis) {
        return (int) ((timeMillis % roundMs) / tickMs);
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

    /**
     * Timing queue
     *
     * @param <T> element type
     */
    private static final class TimingQueue<T extends Timing<T>> extends PriorityQueue<T> /*PriorityBlockingQueue<T>*/ {
        private static final long serialVersionUID = -1728596471728230208L;

        @Override
        public synchronized T poll() {
            return super.poll();
        }

        @Override
        public synchronized boolean offer(T timing) {
            return super.offer(timing);
        }
    }

}
