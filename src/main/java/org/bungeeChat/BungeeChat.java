package org.bungeeChat;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import org.bungeeChat.commands.*;
import org.bungeeChat.listeners.PlayerLoginListener;
import org.bungeeChat.managers.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class BungeeChat extends Plugin {
    private AntiAbuseManager antiAbuseManager;
    private ConfigurationManager configManager;
    private PlayerDataManager playerDataManager;
    private ChatManager chatManager;
    private PrefixManager prefixManager;
    private MuteManager muteManager;
    private CooldownManager cooldownManager;
    private ShoutManager shoutManager;
    private TabListManager tabListManager;
    private final Map<UUID, ServerSwitchToken> pendingSwitches = new ConcurrentHashMap<>();
    private Configuration messagesConfig; // 使用明确的变量名
    private Configuration messages;

    @Override
    public void onEnable() {
        // 初始化各管理器
        configManager = new ConfigurationManager(this);
        playerDataManager = new PlayerDataManager(this);
        chatManager = new ChatManager(this);
        prefixManager = new PrefixManager(this);
        muteManager = new MuteManager(this);
        cooldownManager = new CooldownManager(this);
        shoutManager = new ShoutManager(this);
        tabListManager = new TabListManager(this);
        antiAbuseManager = new AntiAbuseManager(this);

        // 注册命令
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand(this));
        getProxy().getPluginManager().registerCommand(this, new MuteCommand(this));
        getProxy().getPluginManager().registerCommand(this, new UnmuteCommand(this));
        getProxy().getPluginManager().registerCommand(this, new PrefixesCommand(this));
        getProxy().getPluginManager().registerCommand(this, new QuoteHandler(this));
        getProxy().getPluginManager().registerCommand(this, new InviteCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ShoutCommand(this));
        getProxy().getPluginManager().registerCommand(this, new ShoutCountCommand(this));
        getProxy().getPluginManager().registerCommand(this, new SwitchServerCommand(this));
        getProxy().getPluginManager().registerCommand(this, new AtMenuCommand());

        // 注册监听器
        getProxy().getPluginManager().registerListener(this, chatManager);
        getProxy().getPluginManager().registerListener(this, new PlayerLoginListener(this));

        // TAB列表刷新任务
        tabListManager.startAutoRefresh();

        // 注册 Plugin Message 信道
        getProxy().registerChannel("bungeechat-tablistner:channel");
        getProxy().registerChannel("bungeechat-mention:channel");

        // 每1小时清理一次过期消息
        getProxy().getScheduler().schedule(this, () -> {
            this.getPlayerDataManager().cleanupExpiredMessages();
        }, 1, 1, TimeUnit.MINUTES);

        //每分钟清理一次临时校验码
        getProxy().getScheduler().schedule(this, () -> {
            long now = System.currentTimeMillis();
            pendingSwitches.entrySet().removeIf(entry ->
                    entry.getValue().expireTime() < now
            );
        }, 1, 1, TimeUnit.MINUTES);

        // 初始化配置
        configManager.reloadConfigs();

        getProxy().getConsole().sendMessage(ChatColor.GREEN + "[BungeeChat] 插件已启用！");
    }

    public void onDisable(){
        // 停止Tab列表自动刷新
        tabListManager.stopAutoRefresh();

        // 注销 Plugin Message 信道
        getProxy().unregisterChannel("bungeechat-tablistner:channel");

        getProxy().getConsole().sendMessage(ChatColor.GREEN + "[BungeeChat] 插件已禁用！");
    }

    // Getter方法
    public ConfigurationManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public PrefixManager getPrefixManager() {
        return prefixManager;
    }

    public MuteManager getMuteManager() {
        return muteManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public ShoutManager getShoutManager() {
        return shoutManager;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public AntiAbuseManager getAntiAbuseManager() {
        return antiAbuseManager;
    }

    public Map<UUID, ServerSwitchToken> getPendingSwitches() {
        return pendingSwitches;
    }

    // 生成带时效的跳转令牌
    public ServerSwitchToken generateSwitchToken(ProxiedPlayer player) {
        int expireMinutes = configManager.getConfig().getInt("ShoutTempCount.expired", 1);
        ServerSwitchToken token = new ServerSwitchToken(
                UUID.randomUUID(),
                player.getServer().getInfo().getName(),
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(expireMinutes)
        );
        pendingSwitches.put(token.tokenId(), token);
        return token;
    }

    // 修改验证逻辑添加尝试次数限制
    public boolean validateToken(UUID tokenId, String serverName) {
        ServerSwitchToken token = pendingSwitches.get(tokenId);
        return token != null &&
                !token.isExpired() &&
                token.targetServer().equals(serverName);
    }

    public record ServerSwitchToken(UUID tokenId, String targetServer, long expireTime) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        // 添加剩余时间方法(用于调试)
        public long getRemainingMillis() {
            return Math.max(0, expireTime - System.currentTimeMillis());
        }
    }

    public String getMessage(String path) {
        String message = configManager.getMessages().getString(path, "§c未配置的消息: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String formatMessage(String path, ProxiedPlayer player, Object... replacements) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            getLogger().warning("消息路径未找到: " + path);
            return ChatColor.RED + "[错误] 未配置的消息";
        }

        message = message.replace("{player}", player.getName());

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(
                        "{" + replacements[i] + "}",
                        String.valueOf(replacements[i + 1])
                );
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void reloadConfigs() {
        configManager.reloadConfigs();
    }
}