package org.bungeeChat.managers;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bungeeChat.BungeeChat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final BungeeChat bungeeChat;

    public CooldownManager(BungeeChat bungeeChat) {
        this.bungeeChat = bungeeChat;
    }

    public long getCooldown(ProxiedPlayer player, String cooldownType) {
        return cooldowns.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>())
                .getOrDefault(cooldownType, 0L);
    }

    public void setCooldown(ProxiedPlayer player, String cooldownType, long time) {
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(
                player.getUniqueId(),
                k -> new ConcurrentHashMap<>()
        );
        playerCooldowns.put(cooldownType, time);
    }

    public boolean isOnCooldown(ProxiedPlayer player, String cooldownType) {
        long lastUsed = getCooldown(player, cooldownType);
        long currentTime = System.currentTimeMillis() / 1000;
        long cooldownDuration = getCooldownDuration(cooldownType);
        return (currentTime - lastUsed) < cooldownDuration;
    }

    public long getRemainingCooldown(ProxiedPlayer player, String cooldownType) {
        long lastUsed = getCooldown(player, cooldownType);
        long currentTime = System.currentTimeMillis() / 1000;
        long cooldownDuration = getCooldownDuration(cooldownType);
        long remaining = cooldownDuration - (currentTime - lastUsed);
        return remaining > 0 ? remaining : 0;
    }

    private long getCooldownDuration(String cooldownType) {
        // 默认60秒冷却
        return 60;
    }
}