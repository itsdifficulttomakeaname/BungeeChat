package org.bungeeChat.listeners;

import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bungeeChat.BungeeChat;

public class PlayerLoginListener implements Listener {
    private final BungeeChat plugin;

    public PlayerLoginListener(BungeeChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoinServer(ServerSwitchEvent event){
        if(event.getPlayer().getServer()==null) return;
        // 加载玩家称号
        plugin.getPlayerDataManager().loadPlayerPrefix(event.getPlayer());

        // 更新Tab列表
        plugin.getTabListManager().updatePlayerTabList(event.getPlayer());

        // 更新所有玩家的Tab列表（确保新玩家能看到其他人的称号）
        plugin.getTabListManager().updateAllPlayersTabList();
    }
}