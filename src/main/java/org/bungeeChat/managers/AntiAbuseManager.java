package org.bungeeChat.managers;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import org.bungeeChat.BungeeChat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AntiAbuseManager {
    private final BungeeChat plugin;
    private final Pattern antiMessagePattern;
    private final Map<UUID, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> firstMessageTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> messageCounts = new ConcurrentHashMap<>();

    public AntiAbuseManager(BungeeChat plugin) {
        this.plugin = plugin;
        this.antiMessagePattern = compileAntiMessagePattern();
        loadAntiMessagesConfig();
    }

    private void loadAntiMessagesConfig() {
        // 使用正确的文件名 antimessage.yml
        File file = new File(plugin.getDataFolder(), "antimessage.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResourceAsStream("antimessage.yml")) {
                if (in == null) {
                    plugin.getLogger().severe("无法找到默认的antimessage.yml资源!");
                    return;
                }
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建antimessage.yml文件: " + e.getMessage());
                return;
            }
        }
        plugin.loadAntiMessagesConfig();
    }

    public String handleAntiSwear(ProxiedPlayer player, String message) {
        Configuration config = plugin.getConfigManager().getConfig();
        String original = message;
        boolean hasSwear = false;

        // 从AntiSwear节点获取替换字符串
        String replaceWith = config.getString("AntiSwear.replacemessage", "***");

        // 检查违禁词
        if (antiMessagePattern.matcher(message).find()) {
            hasSwear = true;
            // 替换违禁词
            message = antiMessagePattern.matcher(message).replaceAll(replaceWith);
        }

        if (hasSwear) {
            if (!player.hasPermission("antispam.admin.bypass")) {
                int count = incrementViolationCount(player.getUniqueId());
                if (count < config.getInt("AntiSwear.times", 3)) {
                    String warnMessage = plugin.getMessage("AntiSwear.warnmessage")
                            .replace("{count}", String.valueOf(count))
                            .replace("{PLayer}", player.getName())
                            .replace("{time}", formatDuration(config.getInt("AntiSwear.time", 600)))
                            .replace("&", "§");
                    player.sendMessage(warnMessage);
                }

                if (count >= config.getInt("AntiSwear.times", 3)) {
                    plugin.getMuteManager().mutePlayer(
                            player,
                            config.getInt("AntiSwear.time", 600),
                            "屏蔽词"
                    );
                    String banMessage = config.getString("AntiSwear.banmessage", "由于你多次提到敏感词汇，你将会被禁止发言 {time}")
                            .replace("{time}", formatDuration(config.getInt("AntiSwear.time", 600)));
                    player.sendMessage(banMessage);
                }
            }
            return message; // 返回替换后的消息
        }
        return original;
    }

    public boolean handleAntiSpam(ProxiedPlayer player, String message) {
        Configuration config = plugin.getConfigManager().getConfig();

        // 配置检查
        if (!config.getBoolean("AntiSpam.enable", true)) {
            return false;
        }

        // 权限检查
        if (player.hasPermission("antispam.admin.bypass")) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        int refreshTime = config.getInt("AntiSpam.refresh-time", 10) * 1000;
        int warnThreshold = config.getInt("AntiSpam.times-warn", 3);
        int banThreshold = config.getInt("AntiSpam.times-ban", 5);
        int duration = config.getInt("AntiSpam.ban-time", 600);

        // 检查是否在时间窗口内
        Long firstMessageTime = firstMessageTimes.get(playerId);
        if (firstMessageTime == null || currentTime - firstMessageTime > refreshTime) {
            // 新时间窗口开始
            firstMessageTimes.put(playerId, currentTime);
            messageCounts.put(playerId, 1);
            return false;
        }

        // 增加消息计数
        int msgCount = messageCounts.merge(playerId, 1, Integer::sum);

        // 处理警告
        if (msgCount >= warnThreshold && msgCount < banThreshold) {
            String warnMessage = plugin.getMessage("AntiSpam.warnmessage")
                    .replace("{count}", String.valueOf(msgCount))
                    .replace("{time}", formatDuration(duration))
                    .replace("&", "§");
            player.sendMessage(warnMessage);
            return false;
        }

        // 处理禁言
        if (msgCount >= banThreshold) {
            plugin.getMuteManager().mutePlayer(
                    player,
                    duration,
                    "刷屏"
            );
            return true;
        }

        return false;
    }

    private int incrementViolationCount(UUID playerId) {
        return violationCounts.merge(playerId, 1, Integer::sum);
    }

    public void resetViolationCount(UUID playerId) {
        violationCounts.remove(playerId);
    }

    private Pattern compileAntiMessagePattern() {
        // 确保从正确的配置节点获取违禁词列表
        List<String> badWords = plugin.getConfigManager().getAntiMessages().getStringList("filtered-words");
        if (badWords.isEmpty()) {
            plugin.getLogger().warning("违禁词列表为空，将不会过滤任何消息!");
            return Pattern.compile("(?!)"); // 匹配空模式
        }

        String regex = badWords.stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        return Pattern.compile("(?i)(" + regex + ")");
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return hours + "时" + minutes + "分" + secs + "秒";
    }
}