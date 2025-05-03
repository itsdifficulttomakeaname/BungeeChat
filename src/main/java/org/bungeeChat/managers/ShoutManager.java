package org.bungeeChat.managers;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bungeeChat.BungeeChat;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShoutManager {
    private final BungeeChat plugin;
    private final Map<UUID, Long> shoutCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> inviteSettings = new ConcurrentHashMap<>();
    private Configuration shoutPlayerData;
    private Configuration blockedPlayersData;

    public ShoutManager(BungeeChat plugin) {
        this.plugin = plugin;
        loadShoutData();
    }

    private void loadShoutData() {
        File dataFolder = new File(plugin.getDataFolder(), "ShoutPlayerData");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File countFile = new File(dataFolder, "Count.yml");
        File blockedFile = new File(dataFolder, "Blocked.yml");

        try {
            if (!countFile.exists()) countFile.createNewFile();
            if (!blockedFile.exists()) blockedFile.createNewFile();

            shoutPlayerData = ConfigurationProvider.getProvider(YamlConfiguration.class).load(countFile);
            blockedPlayersData = ConfigurationProvider.getProvider(YamlConfiguration.class).load(blockedFile);
        } catch (IOException e) {
            plugin.getLogger().severe("加载喇叭数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getShoutCount(ProxiedPlayer player) {
        return shoutPlayerData.getInt(player.getName(),
                plugin.getConfigManager().getConfig().getInt("GlobalMessage.default_count", 10));
    }

    public void setShoutCount(ProxiedPlayer player, int count) {
        shoutPlayerData.set(player.getName(), count);
        saveShoutData();
    }

    public boolean isInviteEnabled(ProxiedPlayer player) {
        return inviteSettings.getOrDefault(player.getUniqueId(), true);
    }

    public void setInviteEnabled(ProxiedPlayer player, boolean enabled) {
        inviteSettings.put(player.getUniqueId(), enabled);
    }

    public boolean isBlocked(ProxiedPlayer player, ProxiedPlayer target) {
        List<String> blocked = getBlockedPlayers(player);
        return blocked.contains(target.getName()) || blocked.contains("all");
    }

    public List<String> getBlockedPlayers(ProxiedPlayer player) {
        if (blockedPlayersData.getString(player.getName(), "").equalsIgnoreCase("all")) {
            return Collections.singletonList("all");
        }
        return blockedPlayersData.getStringList(player.getName());
    }

    public void blockPlayer(ProxiedPlayer player, String target) {
        List<String> blocked = getBlockedPlayers(player);
        if (!blocked.contains(target)) {
            blocked.add(target);
            blockedPlayersData.set(player.getName(), blocked);
            saveBlockedData();
        }
    }

    public void unblockPlayer(ProxiedPlayer player, String target) {
        List<String> blocked = getBlockedPlayers(player);
        blocked.remove(target);
        blockedPlayersData.set(player.getName(), blocked);
        saveBlockedData();
    }

    public void blockAll(ProxiedPlayer player) {
        blockedPlayersData.set(player.getName(), "all");
        saveBlockedData();
    }

    public void unblockAll(ProxiedPlayer player) {
        blockedPlayersData.set(player.getName(), new ArrayList<>());
        saveBlockedData();
    }

    private void saveShoutData() {
        try {
            File dataFolder = new File(plugin.getDataFolder(), "ShoutPlayerData");
            File countFile = new File(dataFolder, "Count.yml");
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(shoutPlayerData, countFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存喇叭数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveBlockedData() {
        try {
            File dataFolder = new File(plugin.getDataFolder(), "ShoutPlayerData");
            File blockedFile = new File(dataFolder, "Blocked.yml");
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(blockedPlayersData, blockedFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存屏蔽数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}