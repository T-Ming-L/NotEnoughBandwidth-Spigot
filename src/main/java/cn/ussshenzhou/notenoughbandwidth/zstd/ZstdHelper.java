package cn.ussshenzhou.notenoughbandwidth.zstd;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthPlugin;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Zstd压缩助手 - 管理每个连接的压缩上下文
 *
 * @author USS_Shenzhou
 */
public class ZstdHelper {

    private static final Cache<String, Context> ZSTD_CONTEXT_CACHE = CacheBuilder.newBuilder()
            .maximumSize(200)
            .removalListener((RemovalListener<String, Context>) notification -> {
                if (notification.getValue() != null) {
                    notification.getValue().close();
                }
            })
            .build();

    private static int getWindowLog() {
        return NotEnoughBandwidthPlugin.getInstance().getNebConfig().getContextLevel();
    }

    /**
     * 压缩数据
     */
    public static byte[] compress(byte[] raw, int length) {
        try {
            Context ctx = getContext("global");
            return ctx.compress(raw);
        } catch (Exception e) {
            // 回退：使用独立上下文
            Context ctx = new Context(false, getWindowLog());
            return ctx.compress(raw);
        }
    }

    /**
     * 解压数据
     */
    public static byte[] decompress(byte[] compressed, int originalSize) {
        try {
            Context ctx = getContext("global");
            return ctx.decompress(compressed, originalSize);
        } catch (Exception e) {
            Context ctx = new Context(false, getWindowLog());
            return ctx.decompress(compressed, originalSize);
        }
    }

    /**
     * 使用玩家特定的上下文压缩
     */
    public static byte[] compressWithContext(UUID playerUUID, byte[] raw, int length) {
        try {
            Context ctx = getContext(playerUUID.toString());
            return ctx.compress(raw);
        } catch (Exception e) {
            return compress(raw, length);
        }
    }

    /**
     * 使用玩家特定的上下文解压
     */
    public static byte[] decompressWithContext(UUID playerUUID, byte[] compressed, int originalSize) {
        try {
            Context ctx = getContext(playerUUID.toString());
            return ctx.decompress(compressed, originalSize);
        } catch (Exception e) {
            return decompress(compressed, originalSize);
        }
    }

    private static Context getContext(String key) throws ExecutionException {
        return ZSTD_CONTEXT_CACHE.get(key, () -> new Context(true, getWindowLog()));
    }

    /**
     * 清理所有上下文
     */
    public static void clearCache() {
        ZSTD_CONTEXT_CACHE.invalidateAll();
        ZSTD_CONTEXT_CACHE.cleanUp();
    }
}
