/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.util.Maths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * <pre>
 * 基于snowflake算法的ID生成器
 *
 * BINARY(Long.MAX_VALUE         )=0111111111111111111111111111111111111111111111111111111111111111
 * BINARY(2039-09-07 23:47:35.551)=0000000000000000000000011111111111111111111111111111111111111111
 *
 * 0 | 0000000000 0000000000 0000000000 0000000000 0 | 00000 | 00000 | 0000000000 00
 * - | ------------------timestamp------------------ | -did- | -wid- | -----seq-----
 *
 * 00 ~ 00：1位未使用（实际上也是作为long的符号位）
 * 01 ~ 41：41位为毫秒级时间（能到“2039-09-07 23:47:35.551”，41位bit的最大Long值，超过会溢出）
 * 42 ~ 46：5位datacenterId
 * 47 ~ 51：5位workerId（并不算标识符，实际是为线程标识）
 * 52 ~ 63：12位该毫秒内的当前毫秒内的计数
 *
 * 毫秒内序列 （由datacenter和机器ID作区分），并且效率较高。经测试，
 * snowflake每秒能够产生26万ID左右，完全满足需要。
 *
 * 计算掩码的三种方式：
 *   a：(1 << bits) - 1
 *   b：-1L ^ (-1L << bits)
 *   c：Long.MAX_VALUE >>> (63 - bits)
 * </pre>
 *
 * @author Ponfee
 */
public final class Snowflake implements IdGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(Snowflake.class);

    /**
     * 起始基准时间点(2023-01-01)
     */
    private static final long TWEPOCH = 1425139200000L;

    /**
     * 数据中心ID
     */
    private final long datacenterId;

    /**
     * 工作机器ID
     */
    private final long workerId;

    private final int workerIdShift;
    private final int datacenterIdShift;
    private final int timestampShift;

    private final long sequenceMask;

    private long lastTimestamp = -1L;
    private long sequence      = 0L;

    public Snowflake(int workerId,
                     int datacenterId,
                     int sequenceBitLength,
                     int workerIdBitLength,
                     int datacenterIdBitLength) {
        int len = sequenceBitLength + workerIdBitLength + datacenterIdBitLength;
        Assert.isTrue(len <= 22, () -> "Bit length(sequence + worker + datacenter) cannot greater than 22, but actual=" + len);

        long maxWorkerId = Maths.bitsMask(workerIdBitLength);
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("Worker id must be range [0, " + maxWorkerId + "], but was " + workerId);
        }

        long maxDatacenterId = Maths.bitsMask(datacenterIdBitLength);
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException("Worker id must be range [0, " + maxDatacenterId + "], but was " + datacenterId);
        }

        this.workerIdShift     = sequenceBitLength;
        this.datacenterIdShift = sequenceBitLength + workerIdBitLength;
        this.timestampShift    = sequenceBitLength + workerIdBitLength + datacenterIdBitLength;

        this.sequenceMask      = Maths.bitsMask(sequenceBitLength);

        this.workerId          = workerId;
        this.datacenterId      = datacenterId;
    }

    /**
     * Without datacenter id
     *
     * @param workerId          the worker id
     * @param sequenceBitLength the sequence bit length
     * @param workerIdBitLength the worker id bit length
     */
    public Snowflake(int workerId, int sequenceBitLength, int workerIdBitLength) {
        this(workerId, 0, sequenceBitLength, workerIdBitLength, 0);
    }

    @Override
    public long generateId() {
        return nextId();
    }

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset > 50) {
                throw new ClockMovedBackwardsException("Clock moved backwards " + offset + "ms exceed 50ms.");
            }

            LOG.warn("Clock moved backwards {}ms, will be wait moment.", offset);
            try {
                wait(offset << 1);
                timestamp = timeGen();
                if (timestamp < lastTimestamp) {
                    throw new ClockMovedBackwardsException("Clock moved backwards " + offset + " ms, wait still backwards " + (lastTimestamp - timestamp) + " ms.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ClockMovedBackwardsException("Clock moved backwards " + offset + " ms, wait occur error.", e);
            }
        }

        if (lastTimestamp == timestamp) {
            // sequence递增
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 当前毫秒的sequence已用完，需要循环等待获取下一毫秒
                timestamp = tillNextMillis(lastTimestamp);
                lastTimestamp = timestamp;
            }
        } else {
            // 上一毫秒的sequence未超用，当前毫秒第一次使用
            sequence = 0L;
            lastTimestamp = timestamp;
        }

        return ((timestamp - TWEPOCH) << timestampShift)
             | (datacenterId << datacenterIdShift)
             | (workerId << workerIdShift)
             | sequence;
    }

    /**
     * 获取下一个时间戳毫秒，一直循环直到获取到为止
     *
     * @param lastTimestamp the lastTimestamp
     * @return next timestamp
     */
    private long tillNextMillis(long lastTimestamp) {
        LOG.warn("Snowflake til next millis.");

        long timestamp;
        do {
            timestamp = timeGen();
        } while (timestamp <= lastTimestamp);
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

}
