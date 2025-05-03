package org.bungeeChat.managers;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import org.bungeeChat.BungeeChat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class AntiAbuseManager {
    private final BungeeChat plugin;
    private final Pattern antiMessagePattern;
    private final Map<UUID, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> messageTimestamps = new ConcurrentHashMap<>();

    public AntiAbuseManager(BungeeChat plugin) {
        this.plugin = plugin;
        this.antiMessagePattern = compileAntiMessagePattern();
    }

    public boolean handleAntiSwear(ProxiedPlayer player, String message) {
        Configuration config = plugin.getConfigManager().getConfig();
        Configuration messages = plugin.getMessagesConfig(); // 使用正确的方法

        boolean hasViolation = antiMessagePattern.matcher(message).find();
        if (hasViolation) {
            if (!player.hasPermission("antispam.admin.bypass")) {
                int count = incrementViolationCount(player.getUniqueId());
                String count_number = String.valueOf(count);

                String warnMessage = messages.getString("AntiSwear.warnmessage", "&6请勿发送违规内容！这是第{count}次警告")
                        .replace("{count}", count_number)
                        .replace("{PLayer}", player.getName());
                player.sendMessage(warnMessage);

                if (count >= config.getInt("AntiSwear.times", 3)) {
                    plugin.getMuteManager().mutePlayer(
                            player,
                            config.getInt("AntiSwear.time", 600),
                            "屏蔽词"
                    );
                }
            }
            return true;
        }
        return false;
    }

    public boolean handleAntiSpam(ProxiedPlayer player, String originalMessage) {
        // 配置检查
        if (!plugin.getConfigManager().getConfig().getBoolean("AntiSpam.enable", true)) {
            return false;
        }

        // 权限检查
        boolean isBypass = player.hasPermission("antispam.admin.bypass");
        int timeWindow = plugin.getConfigManager().getConfig().getInt("AntiSpam.refresh-time", 10) * 1000;
        int warnThreshold = plugin.getConfigManager().getConfig().getInt("AntiSpam.times-warn", 3);
        int banThreshold = plugin.getConfigManager().getConfig().getInt("AntiSpam.times-ban", 5);
        int duration = plugin.getConfigManager().getConfig().getInt("AntiSpam.ban-time", 600);

        // 获取并清理时间戳
        List<Long> timestamps = getMessageTimestamps(player.getUniqueId());
        timestamps.removeIf(t -> System.currentTimeMillis() - t > timeWindow);
        int msgCount = timestamps.size();

        // 添加当前时间戳
        addMessageTimestamp(player.getUniqueId());

        // 未达到警告阈值
        if (msgCount < warnThreshold) {
            return false;
        }

        // 处理有bypass权限的玩家
        if (isBypass) {
            if (msgCount >= warnThreshold) {
                String warning = "&6请勿发送违规内容！";
                player.sendMessage(warning);
            }
            return false;
        }

        // 处理达到警告阈值而没有超过禁言阈值的玩家（没有bypass权限）
        if (msgCount <= banThreshold){
            String warnMessage = plugin.getMessagesConfig().getString("AntiSpam.warnmessage", "&6请勿发送违规内容！这是第 {count} 次警告")
                    .replace("{count}", String.valueOf(msgCount))
                    .replace("{time}", formatDuration(duration));
            player.sendMessage(warnMessage);
            return false;
        }

        // 处理普通玩家
        if (msgCount >= banThreshold) {
            plugin.getMuteManager().mutePlayer(
                    player,
                    duration,
                    "刷屏"
            );
        }

        return true;
    }

    private int incrementViolationCount(UUID playerId) {
        return violationCounts.merge(playerId, 1, Integer::sum);
    }

    public void resetViolationCount(UUID playerId) {
        violationCounts.remove(playerId);
    }

    private List<Long> getMessageTimestamps(UUID playerId) {
        return messageTimestamps.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    private void addMessageTimestamp(UUID playerId) {
        getMessageTimestamps(playerId).add(System.currentTimeMillis());
    }

    private void sendWarning(ProxiedPlayer player) {
        String message = plugin.formatMessage(
                "AntiSpam.warning",
                player,
                "time", formatDuration(plugin.getConfigManager().getConfig().getInt("AntiSpam.ban-time", 600))
        );

        // 保持原消息格式
        TextComponent warning = plugin.getChatManager().formatChatMessage(
                player,
                plugin.getPrefixManager().getActivePrefix(player.getName()),
                message
        );
        player.sendMessage(warning);
    }

    private Pattern compileAntiMessagePattern() {
        String regex = String.join("|", plugin.getConfigManager().getAntiMessages().getStringList("antimessages"));
        return Pattern.compile("(?i)(" + regex + ")");
    }

    private String getColorCode(String color) {
        switch (color.toLowerCase()) {
            case "red": return "§c";
            case "orange": return "§e";
            case "yellow": return "§6";
            case "light_green": return "§a";
            case "green": return "§2";
            default: return "§f";
        }
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return hours + "时" + minutes + "分" + secs + "秒";
    }
}