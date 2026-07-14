package cn.ussshenzhou.notenoughbandwidth.aggregation;

import cn.ussshenzhou.notenoughbandwidth.zstd.ZstdHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合数据包 - 将多个小数据包合并为一个大数据包并压缩
 *
 * @author USS_Shenzhou
 */
public class PacketAggregationPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketAggregationPacket> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath("neb", "packet_aggregation"));

    public static final StreamCodec<FriendlyByteBuf, PacketAggregationPacket> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> value.write(buf),
            PacketAggregationPacket::new);

    private final ArrayList<AggregatedEncodePacket> packetsToEncode;
    private final Connection connection;
    private List<AggregatedDecodePacket> decodedPackets;

    // 编码端
    public PacketAggregationPacket(ArrayList<AggregatedEncodePacket> packetsToEncode, Connection connection) {
        this.packetsToEncode = packetsToEncode;
        this.connection = connection;
        this.decodedPackets = null;
    }

    // 解码端
    public PacketAggregationPacket(FriendlyByteBuf buffer) {
        this.packetsToEncode = null;
        this.connection = null;
        decodeFromBuf(buffer);
    }

    /**
     * 编码聚合包
     * 格式: B(压缩标记) [S(原始大小)] p0 s0 d0 p1 s1 d1 ...
     */
    public void write(FriendlyByteBuf buffer) {
        ByteBuf rawBuf = Unpooled.buffer();
        FriendlyByteBuf raw = new FriendlyByteBuf(rawBuf);

        for (AggregatedEncodePacket p : packetsToEncode) {
            raw.writeIdentifier(p.type);
            int sizePos = rawBuf.writerIndex();
            raw.writeVarInt(0); // placeholder
            int dataStart = rawBuf.writerIndex();
            raw.writeUtf(p.type.toString());
            int dataSize = rawBuf.writerIndex() - dataStart;
            int curPos = rawBuf.writerIndex();
            rawBuf.writerIndex(sizePos);
            raw.writeVarInt(dataSize);
            rawBuf.writerIndex(curPos);
        }

        int rawSize = rawBuf.readableBytes();
        boolean compress = rawSize >= 32;
        buffer.writeBoolean(compress);

        if (compress) {
            buffer.writeVarInt(rawSize);
            byte[] rawData = new byte[rawSize];
            rawBuf.readBytes(rawData);
            buffer.writeBytes(ZstdHelper.compress(rawData, rawSize));
        } else {
            buffer.writeBytes(rawBuf);
        }
        rawBuf.release();
    }

    /**
     * 解码聚合包
     */
    private void decodeFromBuf(FriendlyByteBuf buffer) {
        this.decodedPackets = new ArrayList<>();
        boolean compressed = buffer.readBoolean();
        ByteBuf data;

        if (compressed) {
            int rawSize = buffer.readVarInt();
            byte[] cmp = new byte[buffer.readableBytes()];
            buffer.readBytes(cmp);
            data = Unpooled.wrappedBuffer(ZstdHelper.decompress(cmp, rawSize));
        } else {
            data = Unpooled.wrappedBuffer(buffer.readBytes(buffer.readableBytes()).array());
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(data);
        while (buf.readableBytes() > 0) {
            Identifier type = buf.readIdentifier();
            int size = buf.readVarInt();
            byte[] d = new byte[size];
            buf.readBytes(d);
            decodedPackets.add(new AggregatedDecodePacket(type, d));
        }
        data.release();
    }

    public List<AggregatedDecodePacket> getDecodedPackets() {
        return decodedPackets;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
