package org.bungeeChat.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.bungeeChat.BungeeChat;

public class ShoutCommand extends Command {
    private final BungeeChat plugin;

    public ShoutCommand(BungeeChat plugin) {
        super("shout", null, "broadcast", "bc");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("§c只有玩家可以使用此命令"));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length == 0) {
            player.sendMessage(new TextComponent("§c用法: /shout <消息>"));
            return;
        }

        if (plugin.getCooldownManager().isOnCooldown(player, "shout")) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(player, "shout");
            player.sendMessage(new TextComponent("§c喇叭冷却中，剩余时间: " +
                    plugin.getMuteManager().formatDuration(remaining)));
            return;
        }

        int shoutCount = plugin.getShoutManager().getShoutCount(player);
        if (shoutCount <= 0) {
            player.sendMessage(new TextComponent("§c你的喇叭数量不足！"));
            return;
        }

        String message = String.join(" ", args);
        broadcastMessage(player, message);

        plugin.getShoutManager().setShoutCount(player, shoutCount - 1);
        plugin.getCooldownManager().setCooldown(player, "shout", System.currentTimeMillis() / 1000);
    }

    private void broadcastMessage(ProxiedPlayer sender, String message) {
        String formatted = "§6[喇叭]§e " + sender.getName() + ": §f" + message;
        TextComponent component = new TextComponent(formatted);

        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            if (!plugin.getShoutManager().isBlocked(player, sender)) {
                player.sendMessage(component);
            }
        }
    }
}