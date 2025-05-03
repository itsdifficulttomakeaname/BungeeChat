package org.bungeeChat.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.bungeeChat.BungeeChat;

import java.util.UUID;

public class SwitchServerCommand extends Command {
    private final BungeeChat plugin;

    public SwitchServerCommand(BungeeChat plugin) {
        super("switchserver");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage(new TextComponent("§c用法: /switchserver <服务器> <令牌>"));
            return;
        }

        try {
            UUID tokenId = UUID.fromString(args[1]);
            String serverName = args[0];

            // 仅验证不删除令牌，由定时任务统一清理
            BungeeChat.ServerSwitchToken token = plugin.getPendingSwitches().get(tokenId);
            if (token == null || token.isExpired() || !token.targetServer().equals(serverName)) {
                String warnMsg = plugin.getConfigManager()
                        .getConfig()
                        .getString("ShoutTempCount.warn_message", "§c跳转令牌已过期或无效");
                player.sendMessage(new TextComponent(warnMsg));
                return;
            }

            ServerInfo target = plugin.getProxy().getServerInfo(serverName);
            if (target != null) {
                player.connect(target);
                // 不再这里移除token，由定时任务处理
            } else {
                player.sendMessage(new TextComponent("§c目标服务器不存在"));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(new TextComponent("§c无效的令牌格式"));
        }
    }
}