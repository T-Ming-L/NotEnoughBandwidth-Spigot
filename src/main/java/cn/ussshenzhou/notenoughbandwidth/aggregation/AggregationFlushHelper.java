package cn.ussshenzhou.notenoughbandwidth.aggregation;

/**
 * 聚合刷新配置助手
 *
 * @author USS_Shenzhou
 */
public class AggregationFlushHelper {

    /**
     * @return 刷新周期（毫秒）
     */
    public static int getFlushPeriodInMilliseconds() {
        return 20; // 50 TPS，每tick刷新一次
    }

    /**
     * @return 每秒刷新次数
     */
    public static int getFlushCountInSeconds() {
        return Math.max(1000 / getFlushPeriodInMilliseconds(), 1);
    }

    /**
     * @return 1秒内触发强制刷新的阈值
     */
    public static int getThresholdCount1s() {
        return 20 * 2;
    }
}
