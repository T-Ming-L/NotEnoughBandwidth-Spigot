package cn.ussshenzhou.notenoughbandwidth.stat;

import cn.ussshenzhou.notenoughbandwidth.util.TimeCounter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 带宽统计数据
 *
 * @author USS_Shenzhou
 */
public record SimpleStatData(
        AtomicLong inboundBytesBaked,
        AtomicLong inboundBytesRaw,
        AtomicLong outboundBytesBaked,
        AtomicLong outboundBytesRaw,
        TimeCounter inboundSpeedBaked,
        TimeCounter inboundSpeedRaw,
        TimeCounter outboundSpeedBaked,
        TimeCounter outboundSpeedRaw) {

    public SimpleStatData() {
        this(
                new AtomicLong(), new AtomicLong(),
                new AtomicLong(), new AtomicLong(),
                new TimeCounter(), new TimeCounter(),
                new TimeCounter(), new TimeCounter());
    }
}
