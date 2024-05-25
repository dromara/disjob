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

package cn.ponfee.disjob.id.snowflake;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.util.Maths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * 起始基准时间点(2015-03-01 00:00:00)
     */
    private static final long TWEPOCH = 1425139200000L;

    /**
     * Timestamp left shift bits length: datacenterIdBitLength + workerIdBitLength + sequenceBitLength
     */
    private final int timestampShift;

    /**
     * Datacenter id left shift bits length: workerIdBitLength + sequenceBitLength
     */
    private final int datacenterIdShift;

    /**
     * Worker id left shift bits length: sequenceBitLength
     */
    private final int workerIdShift;

    /**
     * Sequence mask, such as: 1111111111 11
     */
    private final long sequenceMask;

    /**
     * 数据中心ID
     */
    private final long datacenterId;

    /**
     * 工作机器ID
     */
    private final long workerId;

    /**
     * Last timestamp
     */
    private long lastTimestamp = -1L;

    /**
     * Current sequence
     */
    private long sequence = 0L;

    public Snowflake(int datacenterIdBitLength, int workerIdBitLength, int sequenceBitLength, int datacenterId, int workerId) {
        int len = datacenterIdBitLength + workerIdBitLength + sequenceBitLength;
        if (len > 22) {
            throw new IllegalArgumentException("Bit length(datacenter + worker + sequence) cannot greater than 22, but actual=" + len);
        }

        long maxDatacenterId = Maths.bitsMask(datacenterIdBitLength);
        if (datacenterId < 0 || datacenterId > maxDatacenterId) {
            throw new IllegalArgumentException("Datacenter id must be range [0, " + maxDatacenterId + "], but actual " + datacenterId);
        }

        long maxWorkerId = Maths.bitsMask(workerIdBitLength);
        if (workerId < 0 || workerId > maxWorkerId) {
            throw new IllegalArgumentException("Worker id must be range [0, " + maxWorkerId + "], but actual " + workerId);
        }

        this.timestampShift    = datacenterIdBitLength + workerIdBitLength + sequenceBitLength;
        this.datacenterIdShift = workerIdBitLength + sequenceBitLength;
        this.workerIdShift     = sequenceBitLength;
        this.sequenceMask      = Maths.bitsMask(sequenceBitLength);
        this.datacenterId      = datacenterId;
        this.workerId          = workerId;
    }

    /**
     * Without datacenter id
     *
     * @param workerIdBitLength the worker id bit length
     * @param sequenceBitLength the sequence bit length
     * @param workerId          the worker id
     */
    public Snowflake(int workerIdBitLength, int sequenceBitLength, int workerId) {
        this(0, workerIdBitLength, sequenceBitLength, 0, workerId);
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
                super.wait(offset << 1);
                timestamp = timeGen();
                if (timestamp < lastTimestamp) {
                    String msg = String.format("Clock moved backwards %d ms, wait still backwards %d ms.", offset, (lastTimestamp - timestamp));
                    throw new ClockMovedBackwardsException(msg);
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
