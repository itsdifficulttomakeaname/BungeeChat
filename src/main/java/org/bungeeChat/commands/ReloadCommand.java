package org.bungeeChat.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import org.bungeeChat.BungeeChat;

public class ReloadCommand extends Command {
    private final BungeeChat plugin;

    public ReloadCommand(BungeeChat plugin) {
        super("bungeechatreload", "bungeechat.admin", "bcreload");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getConfigManager().reloadConfigs();
        plugin.getChatManager().reloadAntiMessagePattern();
        sender.sendMessage(new TextComponent("§a[BungeeChat] 配置已重载！"));
    }
}