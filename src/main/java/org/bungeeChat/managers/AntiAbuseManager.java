package org.bungeeChat.managers;

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

        // 广播替换后的消息
        TextComponent component = new TextComponent("<" + player.getDisplayName() + "> " + replaced);
        player.getServer().getInfo().getPlayers().forEach(p -> p.sendMessage(component));

        // 处理违规计数
        if (!player.hasPermission("antispam.admin.bypass")) {
            int count = incrementViolationCount(player.getUniqueId());
            player.sendMessage(TextComponent.fromLegacyText(
                    "§6" + plugin.getConfigManager().getConfig().getString("AntiSwear.warnmessage")
            ));

            if (count >= plugin.getConfigManager().getConfig().getInt("AntiSwear.times", 3)) {
                String banMessage = "§c" + plugin.getConfigManager().getConfig().getString("AntiSwear.banmessage")
                        .replace("{time}", formatDuration(duration));
                player.sendMessage(TextComponent.fromLegacyText(banMessage));
                plugin.getMuteManager().mutePlayer(player, duration, "屏蔽词");
            }
        }
        return true;
    }

    public boolean handleAntiSpam(ProxiedPlayer player) {
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
                sendWarning(player);
            }
            addMessageTimestamp(player.getUniqueId());
            return false;
        }

        if (msgCount >= warnThreshold) {
            if (msgCount >= banThreshold) {
                plugin.getMuteManager().mutePlayer(player, duration, "刷屏");
            } else {
                sendWarning(player);
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
        String color = getColorCode(plugin.getConfigManager().getConfig().getString("AntiSpam.warn-message.color", "yellow"));
        String text = plugin.getConfigManager().getConfig().getString("AntiSpam.warn-message.text", "请停止你的刷屏行为！");
        String prefix = plugin.getConfigManager().getConfig().getString("AntiSpam.prefix");
        player.sendMessage(new TextComponent(color + "[" + prefix + "]" + text));
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