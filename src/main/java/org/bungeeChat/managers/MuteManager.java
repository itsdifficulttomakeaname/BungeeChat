package org.bungeeChat.managers;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.bungeeChat.BungeeChat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MuteManager {
    private final BungeeChat plugin;
    private final Map<UUID, String> mutedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> muteEndTimes = new ConcurrentHashMap<>();
    private final Map<UUID, String> muteReasons = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> muteTasks = new ConcurrentHashMap<>();

    public MuteManager(BungeeChat plugin) {
        this.plugin = plugin;
    }

    public boolean isMuted(UUID playerId) {
        return mutedPlayers.containsKey(playerId);
    }

    public long getRemainingMuteTime(UUID playerId) {
        if (!isMuted(playerId)) return 0;
        return (muteEndTimes.get(playerId) - System.currentTimeMillis()) / 1000;
    }

    public void mutePlayer(ProxiedPlayer player, long duration, String reason) {
        UUID playerId = player.getUniqueId();
        mutedPlayers.put(playerId, player.getName());
        muteReasons.put(playerId, reason);

        long endTime = System.currentTimeMillis() + duration * 1000L;
        muteEndTimes.put(playerId, endTime);

        // 取消现有的解禁任务（如果有）
        if (muteTasks.containsKey(playerId)) {
            muteTasks.get(playerId).cancel();
        }

        // 安排新的解禁任务
        ScheduledTask task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            unmutePlayer(playerId, "自动解禁");
            String text = plugin.getConfigManager().getMessages().getString("mute.unmute-success", "§a你已被解禁")
                    .replace("{player}", player.getName())
                    .replace("&", "§");
            player.sendMessage(TextComponent.fromLegacyText(text));
        }, duration, TimeUnit.SECONDS);

        muteTasks.put(playerId, task);

        String banMsg = plugin.getConfigManager().getMessages().getString("mute.mute-success",
                        "§4你已被禁言，原因: {reason}, 时间: {time}")
                .replace("{player}", player.getName())
                .replace("{reason}", reason)
                .replace("{time}", formatDuration(duration))
                .replace("&", "§");
        player.sendMessage(TextComponent.fromLegacyText(banMsg));
    }

    public void unmutePlayer(UUID playerId, String reason) {
        mutedPlayers.remove(playerId);
        muteReasons.remove(playerId);
        plugin.getAntiAbuseManager().resetViolationCount(playerId); // 重置违规计数

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerId);
        if (player != null) {
            player.sendMessage(new TextComponent("§a你已被解禁，原因: " + reason));
        }
    }

    public String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return hours + "时" + minutes + "分" + secs + "秒";
    }

    public Map<UUID, String> getMutedPlayers() {
        return new HashMap<>(mutedPlayers);
    }

    public String getMuteReason(UUID playerId) {
        return muteReasons.getOrDefault(playerId, "未知原因");
    }

    public boolean unmutePlayer(String playerName) {
        Optional<UUID> uuid = mutedPlayers.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(playerName))
                .map(Map.Entry::getKey)
                .findFirst();

        if (uuid.isPresent()) {
            unmutePlayer(uuid.get(), "管理员解禁");
            return true;
        }
        return false;
    }
}