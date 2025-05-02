package org.bungeeChat.managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import org.bungeeChat.BungeeChat;

import java.util.concurrent.TimeUnit;

public class TabListManager {
    private final BungeeChat plugin;
    private final BungeeAudiences adventure;
    private ScheduledTask refreshTask;

    public TabListManager(BungeeChat plugin) {
        this.plugin = plugin;
        this.adventure = BungeeAudiences.create(plugin);
        startAutoRefresh();
    }

    public void updatePlayerTabList(ProxiedPlayer player) {
        if (player.getServer() == null) return;

        // 获取当前称号
        String prefixKey = plugin.getPrefixManager().getActivePrefixKey(player.getName());
        String prefixName = prefixKey != null ?
                plugin.getPrefixManager().getActivePrefix(player.getName()).name() : "";
        String prefixColor = prefixKey != null ?
                plugin.getPrefixManager().getActivePrefix(player.getName()).color().name() : "WHITE";

        // 发送到 Bukkit
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(player.getName());
        out.writeUTF(prefixName);
        out.writeUTF(prefixColor);
        player.getServer().sendData("bungeechat-tablistner:channel", out.toByteArray());

        // 更新 BungeeCord 的 TabList
        String serverName = player.getServer().getInfo().getName();
        Configuration config = plugin.getConfigManager().getConfig();

        String header = config.getString("TabList.header", "§6欢迎来到服务器\n§a在线玩家: {count}")
                .replace("{count}", String.valueOf(ProxyServer.getInstance().getOnlineCount()))
                .replace("{server}", serverName)
                .replace("{name}", player.getName());

        String footer = config.getString("TabList.footer", "§e当前服务器: {server}")
                .replace("{count}", String.valueOf(ProxyServer.getInstance().getOnlineCount()))
                .replace("{server}", serverName)
                .replace("{name}", player.getName());

        adventure.player(player).sendPlayerListHeaderAndFooter(
                Component.text(header),
                Component.text(footer)
        );
    }

    public void updateAllPlayersTabList() {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            updatePlayerTabList(player);
        }
    }

    public void startAutoRefresh() {
        // 取消现有任务（如果存在）
        stopAutoRefresh();

        // 每秒刷新一次（20 ticks = 1秒）
        refreshTask = plugin.getProxy().getScheduler().schedule(
                plugin,
                this::updateAllPlayersTabList,
                0, 1, TimeUnit.SECONDS
        );
    }

    public void stopAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public void reload() {
        stopAutoRefresh();
        startAutoRefresh();
    }
}