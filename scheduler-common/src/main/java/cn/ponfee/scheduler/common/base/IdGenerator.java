package cn.ponfee.scheduler.common.base;

import cn.ponfee.scheduler.common.util.Maths;

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
 * 47 ~ 51：5位workerId（并不算标识符，实际是为线程标识），
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
public final class IdGenerator {

    // Long.toBinaryString(Long.MAX_VALUE).length()
    private static final int SIZE = Long.SIZE - 1; // 63位（除去最开头的一个符号位）
    private static final long TWEPOCH = 1514736000000L; // 起始基准时间点(2018-01-01)

    private final int datacenterId; // 数据中心ID
    private final int workerId;     // 工作机器ID

    private final int workerIdShift;
    private final int datacenterIdShift;
    private final int timestampShift;

    private final long sequenceMask;
    private final long timestampMask;

    private long lastTimestamp = -1L;
    private long sequence      = 0L;

    public IdGenerator(int workerId, int datacenterId,
                       int sequenceBits, int workerIdBits,
                       int datacenterIdBits) {
        long maxWorkerId = Maths.bitsMask(workerIdBits);
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("worker Id can't be greater than %d or less than 0", maxWorkerId)
            );
        }

        long maxDatacenterId = Maths.bitsMask(datacenterIdBits);
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(
                String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId)
            );
        }

        this.workerIdShift     = sequenceBits;
        this.datacenterIdShift = sequenceBits + workerIdBits;
        this.timestampShift    = sequenceBits + workerIdBits + datacenterIdBits;

        this.sequenceMask      = Maths.bitsMask(sequenceBits);
        this.timestampMask     = Maths.bitsMask(SIZE - this.timestampShift);

        this.workerId          = workerId;
        this.datacenterId      = datacenterId;
    }

    /**
     * sequenceBits: 12 bit, value range of 0 ~ 4095(111111111111)
     * workerIdBits:  5 bit, value range of 0 ~   31(11111)
     * datacenterIdBits: 5 bit, value range of 0 ~ 31(11111)
     * 
     * workerIdShift: sequenceBits，左移12位(seq12位)
     * datacenterIdShift: sequenceBits+workerIdBits，即左移17位(wid5位+seq12位)
     * timestampShift: sequenceBits+workerIdBits+datacenterIdBits，
     *                 即左移22位(did5位+wid5位+seq12位)
     * timestampMask: (1L<<(MAX_SIZE-timestampShift))-1 = (1L<<41)-1
     * 
     * @param workerId
     * @param datacenterId
     */
    public IdGenerator(int workerId, int datacenterId) {
        this(workerId, datacenterId, 12, 5, 5);
    }

    /**
     * no datacenterId
     * max sequence count: 16384
     * max work count    : 32
     * max time at       : 2527-06-23 14:20:44.415
     * 
     * @param workerId
     */
    public IdGenerator(int workerId) {
        this(workerId, 0, 14, 5, 0);
    }

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < this.lastTimestamp) {
            // 时间戳只能单调递增
            throw new RuntimeException(
                String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", this.lastTimestamp - timestamp)
            );
        }
        if (this.lastTimestamp == timestamp) {
            // sequence递增
            this.sequence = (this.sequence + 1) & this.sequenceMask;
            if (this.sequence == 0) {
                // 已经到最大，则获取下一个时间点的毫秒数
                timestamp = tilNextMillis(this.lastTimestamp);
            }
        } else {
            this.sequence = 0L;
        }
        this.lastTimestamp = timestamp;

        return (((timestamp - TWEPOCH) << this.timestampShift) & this.timestampMask)
             | (this.datacenterId << this.datacenterIdShift)
             | (this.workerId << this.workerIdShift)
             | this.sequence;
    }

    /**
     * 获取下一个时间戳毫秒，一直循环直到获取到为止
     * 
     * @param lastTimestamp the lastTimestamp
     * @return
     */
    private long tilNextMillis(long lastTimestamp) {
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
