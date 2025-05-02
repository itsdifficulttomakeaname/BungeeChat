package org.bungeeChat.listeners;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bungeeChat.BungeeChat;
import org.bungeeChat.managers.PrefixManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("all")
public class TabListListener implements Listener {

    private final BungeeAudiences adventure;
    private final BungeeChat plugin;
    private final Map<String, PrefixManager.Prefix> prefixes = new HashMap<>();

    public TabListListener(BungeeChat plugin, BungeeAudiences adventure) {
        this.plugin = plugin;
        this.adventure = adventure;
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        updateTabList(player);
        sendPrefixUpdate(player); // 发送称号信息给下游服务器
    }

    // 新增方法：更新单个玩家的 TAB 列表
    private void updateTabList(ProxiedPlayer player) {
        if (player.getServer().getInfo().getName() == null){
            return;
        }
        String header = plugin.getConfigManager().getConfig().getString("TabList.header")
                .replace("{count}", String.format("%d", ProxyServer.getInstance().getOnlineCount()))
                .replace("{server}", player.getServer().getInfo().getName())
                .replace("{name}", player.getName());
        String footer = plugin.getConfigManager().getConfig().getString("TabList.footer")
                .replace("{count}", String.format("%d", ProxyServer.getInstance().getOnlineCount()))
                .replace("{server}", player.getServer().getInfo().getName())
                .replace("{name}", player.getName());

        // 使用 Adventure API 设置头部和底部
        adventure.player(player).sendPlayerListHeaderAndFooter(
                Component.text(footer),
                Component.text(footer)
        );
    }

    private void sendPrefixUpdate(ProxiedPlayer player) {
        String playerName = player.getName();
        PrefixManager.Prefix prefix = PrefixManager.getActivePrefix(playerName);
        String prefixName = prefix != null ? prefix.name() : "";
        String prefixColor = prefix != null ? prefix.color().toString() : ""; // 获取称号颜色

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF(playerName); // 写入玩家名
            out.writeUTF(prefixName); // 写入称号
            out.writeUTF(prefixColor); // 写入称号颜色
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 发送 PluginMessage 到下游服务器
        player.getServer().sendData("bungeechat-tablistner:channel", stream.toByteArray());
    }
}