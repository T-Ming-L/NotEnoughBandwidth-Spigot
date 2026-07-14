package cn.ussshenzhou.notenoughbandwidth.stat;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * NEB 命令处理器 - 显示统计信息和重载配置
 *
 * @author USS_Shenzhou
 */
public class StatCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component
                    .text("NotEnoughBandwidth " + NotEnoughBandwidthPlugin.getInstance().getPluginMeta().getVersion())
                    .color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("使用 /neb stat 查看统计").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("使用 /neb reload 重载配置").color(NamedTextColor.GRAY));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "stat" -> showStats(sender);
            case "reload" -> reloadConfig(sender);
            default -> sender.sendMessage(Component.text("未知子命令").color(NamedTextColor.RED));
        }
        return true;
    }

    private void showStats(CommandSender sender) {
        var data = SimpleStatManager.LOCAL;

        long inBaked = data.inboundBytesBaked().get();
        long inRaw = data.inboundBytesRaw().get();
        long outBaked = data.outboundBytesBaked().get();
        long outRaw = data.outboundBytesRaw().get();

        double inRatio = inRaw > 0 ? 100.0 * inBaked / inRaw : 0;
        double outRatio = outRaw > 0 ? 100.0 * outBaked / outRaw : 0;

        double inSpeed = data.inboundSpeedBaked().averageIn1s();
        double outSpeed = data.outboundSpeedBaked().averageIn1s();

        sender.sendMessage(Component.text("=== NEB 带宽统计 ===").color(NamedTextColor.GREEN));
        sender.sendMessage(
                Component.text("↓ 入站: " + getReadableSpeed((int) inSpeed) + "  总计: " + getReadableSize(inBaked))
                        .color(NamedTextColor.AQUA));
        sender.sendMessage(
                Component.text("↑ 出站: " + getReadableSpeed((int) outSpeed) + "  总计: " + getReadableSize(outBaked))
                        .color(NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text(String.format("压缩率 ↓ %.2f%%  ↑ %.2f%%", inRatio, outRatio))
                .color(NamedTextColor.YELLOW));
    }

    private void reloadConfig(CommandSender sender) {
        NotEnoughBandwidthPlugin.getInstance().reloadNebConfig();
        sender.sendMessage(Component.text("配置已重载").color(NamedTextColor.GREEN));
    }

    private String getReadableSpeed(int bytes) {
        if (bytes < 1000) {
            return bytes + " B/s";
        } else if (bytes < 1000 * 1000) {
            return String.format("%.1f KiB/s", bytes / 1024f);
        } else {
            return String.format("%.2f MiB/s", bytes / (1024 * 1024f));
        }
    }

    private String getReadableSize(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        } else if (bytes < 1000 * 1000) {
            return String.format("%.1f KiB", bytes / 1024d);
        } else if (bytes < 1000 * 1000 * 1000) {
            return String.format("%.2f MiB", bytes / (1024 * 1024d));
        } else {
            return String.format("%.2f GiB", bytes / (1024 * 1024 * 1024d));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("stat", "reload");
        }
        return List.of();
    }
}
