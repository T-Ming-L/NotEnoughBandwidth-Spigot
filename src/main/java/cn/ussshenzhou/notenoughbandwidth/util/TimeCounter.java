package cn.ussshenzhou.notenoughbandwidth.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时间计数器 - 用于计算一段时间内的平均值
 *
 * @author USS_Shenzhou
 */
public class TimeCounter {
    private final ConcurrentHashMap<Long, Integer> container = new ConcurrentHashMap<>();
    private final int windowsSizeMs;

    public TimeCounter(int windowsSizeMs) {
        this.windowsSizeMs = windowsSizeMs;
    }

    public TimeCounter() {
        this(2000);
    }

    public void put(int value) {
        long now = System.currentTimeMillis();
        container.put(now, value);
        update();
    }

    private void update() {
        long now = System.currentTimeMillis();
        container.keySet().removeIf(then -> now - then > windowsSizeMs);
    }

    public double averageIn1s() {
        update();
        long sum = container.values().stream().mapToInt(Integer::intValue).sum();
        return sum / (double) windowsSizeMs * 1000;
    }
}
