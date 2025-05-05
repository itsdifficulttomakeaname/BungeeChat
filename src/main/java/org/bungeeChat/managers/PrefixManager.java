package org.bungeeChat.managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import org.bungeeChat.BungeeChat;

import java.util.HashMap;
import java.util.Map;

public class PrefixManager {
    public record Prefix(String name, ChatColor color, String permission) {}

    private final BungeeChat plugin;
    private static final Map<String, Prefix> prefixes = new HashMap<>();
    private static final Map<String, String> activePrefixes = new HashMap<>();

    public PrefixManager(BungeeChat plugin) {
        this.plugin = plugin;
        loadPrefixes();
    }

    private void loadPrefixes() {
        Configuration levelSection = plugin.getConfigManager().getConfig().getSection("Prefixes.BungeePrefix.level");
        if (levelSection != null) {
            for (String level : levelSection.getKeys()) {
                Configuration sec1 = levelSection.getSection(level);
                for(String key : sec1.getKeys()) {
                    Configuration prefixConfig = sec1.getSection(key);
                    String name = prefixConfig.getString("name");
                    String color = prefixConfig.getString("color", "white");
                    ChatColor chatColor = getChatColor(color);
                    prefixes.put(key, new Prefix(name, chatColor, "BungeePrefix.level." + level));
                }
            }
        }
    }

    public static Prefix getActivePrefix(String playerName) {
        String prefixKey = activePrefixes.get(playerName);
        return prefixes.get(prefixKey);
    }

    public String getActivePrefixKey(String playerName) {
        return activePrefixes.get(playerName);
    }

    public void enablePrefix(ProxiedPlayer player, String prefixKey) {
        if (!prefixes.containsKey(prefixKey)) return;
        if (!player.hasPermission(prefixes.get(prefixKey).permission())) return;

        activePrefixes.put(player.getName(), prefixKey);
        String enableInfo = plugin.getMessage("prefix.enable-success")
                .replace("{prefix}", prefixKey);
        player.sendMessage("\n§6[" + plugin.getConfigManager().getConfig().getString("Server-Prefix") + "]§r " + enableInfo + "\n");

        // 保存数据
        plugin.getPlayerDataManager().savePlayerData(player.getName(), prefixKey);

        // 更新 TabList
        plugin.getTabListManager().updatePlayerTabList(player);

        // 发送称号数据到 Bukkit
        sendPrefixDataToBukkit(player, prefixKey);
    }

    public void disablePrefix(ProxiedPlayer player) {
        activePrefixes.remove(player.getName());
        String disableInfo = plugin.getMessage("prefix.disable-success");
        player.sendMessage("\n§6[" + plugin.getConfigManager().getConfig().getString("Server-Prefix") + "]§r " + disableInfo + "\n");

        // 保存数据
        plugin.getPlayerDataManager().savePlayerData(player.getName(), null);

        // 更新 TabList
        plugin.getTabListManager().updatePlayerTabList(player);

        // 发送空称号数据到 Bukkit
        sendPrefixDataToBukkit(player, "");
    }

    /**
     * 发送称号数据到 Bukkit 服务器
     */
    private void sendPrefixDataToBukkit(ProxiedPlayer player, String prefixKey) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(player.getName()); // 玩家名
        out.writeUTF(prefixKey); // 称号名称
        out.writeUTF(prefixes.containsKey(prefixKey) ?
                prefixes.get(prefixKey).color().name() : "WHITE"); // 称号颜色

        // 发送到玩家当前所在的 Bukkit 服务器
        player.getServer().sendData("bungeechat-tablistner:channel", out.toByteArray());
    }

    private ChatColor getChatColor(String color) {
        try {
            return ChatColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChatColor.WHITE;
        }
    }
}