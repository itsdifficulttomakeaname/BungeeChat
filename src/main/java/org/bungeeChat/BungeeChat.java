package org.bungeeChat;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.ChatColor;
import org.bungeeChat.commands.MuteCommand;
import org.bungeeChat.commands.PrefixesCommand;
import org.bungeeChat.commands.ReloadCommand;
import org.bungeeChat.commands.UnmuteCommand;
import org.bungeeChat.listeners.PlayerLoginListener;
import org.bungeeChat.managers.*;

import java.util.concurrent.TimeUnit;

public class BungeeChat extends Plugin {
    private ConfigurationManager configManager;
    private PlayerDataManager playerDataManager;
    private ChatManager chatManager;
    private PrefixManager prefixManager;
    private MuteManager muteManager;
    private CooldownManager cooldownManager;
    private ShoutManager shoutManager;
    private TabListManager tabListManager;

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

        // 注册命令
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand(this));
        getProxy().getPluginManager().registerCommand(this, new MuteCommand(this));
        getProxy().getPluginManager().registerCommand(this, new UnmuteCommand(this));
        getProxy().getPluginManager().registerCommand(this, new PrefixesCommand(this));
        getProxy().getPluginManager().registerCommand(this, new QuoteHandler(this));

        // 注册监听器
        getProxy().getPluginManager().registerListener(this, chatManager);
        getProxy().getPluginManager().registerListener(this, new PlayerLoginListener(this));

        // TAB列表刷新任务
        tabListManager.startAutoRefresh();

        // 注册 Plugin Message 信道
        getProxy().registerChannel("bungeechat-tablistner:channel");

        // 每12小时清理一次过期消息
        getProxy().getScheduler().schedule(this, () -> {
            plugin.getPlayerDataManager().cleanupExpiredMessages();
        }, 1, 1, TimeUnit.HOURS);

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
}