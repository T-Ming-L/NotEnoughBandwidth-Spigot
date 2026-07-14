package cn.ussshenzhou.notenoughbandwidth.zstd;

import com.github.luben.zstd.EndDirective;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;

import java.io.Closeable;
import java.nio.ByteBuffer;

/**
 * Zstd压缩/解压上下文
 * 维护每个连接的压缩上下文以提升压缩率
 *
 * @author USS_Shenzhou
 */
public class Context implements Closeable {
    private final ZstdCompressCtx compressCtx;
    private final ZstdDecompressCtx decompressCtx;
    private final boolean useContext;

    public Context(boolean useContext, int windowLog) {
        this.compressCtx = new ZstdCompressCtx();
        this.compressCtx.setLevel(3);
        this.compressCtx.setContentSize(false);
        this.compressCtx.setMagicless(true);
        this.compressCtx.setWindowLog(windowLog);

        this.decompressCtx = new ZstdDecompressCtx();
        this.decompressCtx.setMagicless(true);

        this.useContext = useContext;
    }

    /**
     * 压缩数据
     */
    public byte[] compress(byte[] raw) {
        if (useContext) {
            ByteBuffer src = ByteBuffer.wrap(raw);
            int maxDstSize = (int) Zstd.compressBound(raw.length);
            ByteBuffer dst = ByteBuffer.allocateDirect(maxDstSize);
            compressCtx.compressDirectByteBufferStream(dst, src, EndDirective.FLUSH);
            dst.flip();
            byte[] result = new byte[dst.remaining()];
            dst.get(result);
            return result;
        }
        return compressCtx.compress(raw);
    }

    /**
     * 解压数据
     */
    public byte[] decompress(byte[] compressed, int originalSize) {
        if (useContext) {
            ByteBuffer src = ByteBuffer.wrap(compressed);
            ByteBuffer dst = ByteBuffer.allocateDirect(originalSize);
            decompressCtx.decompressDirectByteBufferStream(dst, src);
            dst.flip();
            byte[] result = new byte[dst.remaining()];
            dst.get(result);
            return result;
        }
        return decompressCtx.decompress(compressed, originalSize);
    }

    @Override
    public void close() {
        compressCtx.close();
        decompressCtx.close();
    }
}
