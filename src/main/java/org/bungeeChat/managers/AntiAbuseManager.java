package org.bungeeChat.managers;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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
        if (!plugin.getConfigManager().getConfig().getBoolean("AntiSwear.enable", true)) {
            return false;
        }

        boolean hasViolation = antiMessagePattern.matcher(message).find();
        if (!hasViolation) {
            return false;
        }

        int duration = plugin.getConfigManager().getConfig().getInt("AntiSwear.time", 600);
        String replaced = antiMessagePattern.matcher(message)
                .replaceAll(plugin.getConfigManager().getConfig().getString("AntiSwear.replacemessage", "***"));

        // 直接取消事件，不发送原始消息
        if (!player.hasPermission("antispam.admin.bypass")) {
            int count = incrementViolationCount(player.getUniqueId());
            player.sendMessage(TextComponent.fromLegacyText(
                    "§6" + plugin.getConfigManager().getConfig().getString("AntiSwear.warnmessage")
            ));

            if (count >= plugin.getConfigManager().getConfig().getInt("AntiSwear.times", 3)) {
                String banMessage = plugin.getConfigManager().getConfig().getString("AntiSwear.banmessage", "§c你因发送违规内容被禁言 {time}")
                        .replace("{time}", formatDuration(duration));
                player.sendMessage(TextComponent.fromLegacyText(banMessage));
                plugin.getMuteManager().mutePlayer(player, duration, "屏蔽词");
            }
        }

        // 返回true表示取消原始消息
        return true;
    }

    public boolean handleAntiSpam(ProxiedPlayer player, String originalMessage) {
        if (!plugin.getConfigManager().getConfig().getBoolean("AntiSpam.enable", true)) {
            return false;
        }

        boolean isBypass = player.hasPermission("antispam.admin.bypass");
        int timeWindow = plugin.getConfigManager().getConfig().getInt("AntiSpam.refresh-time", 5) * 1000;
        int warnThreshold = plugin.getConfigManager().getConfig().getInt("AntiSpam.times-warn", 3);
        int banThreshold = plugin.getConfigManager().getConfig().getInt("AntiSpam.times-ban", 5);
        int duration = plugin.getConfigManager().getConfig().getInt("AntiSpam.ban-time", 600);

        List<Long> timestamps = getMessageTimestamps(player.getUniqueId());
        timestamps.removeIf(t -> System.currentTimeMillis() - t > timeWindow);
        int msgCount = timestamps.size();

        if (isBypass) {
            if (msgCount >= warnThreshold) {
                // 保持原消息格式发送警告
                TextComponent warning = new TextComponent(ChatColor.RED + "[警告] 请降低你的发言频率！");
                player.sendMessage(warning);
            }
            addMessageTimestamp(player.getUniqueId());
            return false;
        }

        if (msgCount >= warnThreshold) {
            if (msgCount >= banThreshold && !player.hasPermission("antispam.admin.bypass")) {
                plugin.getMuteManager().mutePlayer(
                        player,
                        duration,
                        plugin.formatMessage("mute.reason.spam", player)
                );
                return true;
            } else {
                // 保持原消息格式发送警告
                TextComponent warning = plugin.getChatManager().formatChatMessage(
                        player,
                        plugin.getPrefixManager().getActivePrefix(player.getName()),
                        ChatColor.RED + "[警告] 请降低你的发言频率！"
                );
                player.sendMessage(warning);
            }
            return true;
        }

        addMessageTimestamp(player.getUniqueId());
        return false;
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