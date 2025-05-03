package org.bungeeChat.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class AtMenuCommand extends Command {
    public AtMenuCommand() {
        super("atmenu", null, "@"); // 添加别名"@"
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) return;

        ProxiedPlayer player = (ProxiedPlayer) sender;

        // 获取当前服务器所有玩家
        java.util.Collection<ProxiedPlayer> serverPlayers =
                player.getServer().getInfo().getPlayers();

        if (serverPlayers.size() <= 1) {
            player.sendMessage(new TextComponent("§c当前服务器没有其他在线玩家"));
            return;
        }

        // 发送玩家列表标题
        player.sendMessage(new TextComponent(
                "§a=== 当前服务器玩家 (" + serverPlayers.size() + ") ==="
        ));

        // 列出所有玩家（排除自己）
        for (ProxiedPlayer target : serverPlayers) {
            if (!target.equals(player)) { // 不显示自己
                TextComponent entry = new TextComponent("§7- §e" + target.getName());
                entry.setClickEvent(new ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND,
                        "@" + target.getName() + " " // 自动补全格式
                ));
                entry.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("点击提及 " + target.getName()).create()
                ));
                player.sendMessage(entry);
            }
        }
    }
}