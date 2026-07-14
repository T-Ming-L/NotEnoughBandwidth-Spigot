package cn.ussshenzhou.notenoughbandwidth.aggregation;

import net.minecraft.resources.Identifier;

/**
 * 已解码的聚合子数据包
 *
 * @author USS_Shenzhou
 */
public class AggregatedDecodePacket {
    private final Identifier type;
    private final byte[] data;

    public AggregatedDecodePacket(Identifier type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public Identifier getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }
}
