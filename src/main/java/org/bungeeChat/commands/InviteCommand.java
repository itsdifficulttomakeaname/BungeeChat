package org.bungeeChat.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bungeeChat.BungeeChat;
import org.bungeeChat.managers.PrefixManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InviteCommand extends Command implements TabExecutor {
    private final BungeeChat plugin;

    public InviteCommand(BungeeChat plugin) {
        super("invite", null, "inv");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(createText("只有玩家可以使用此指令！", ChatColor.RED));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length == 0) {
            showUsage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "on":
                handleToggle(player, true);
                break;
            case "off":
                handleToggle(player, false);
                break;
            case "i":
                handleInvite(player);
                break;
            default:
                player.sendMessage(createText("未知的子命令！", ChatColor.RED));
        }
    }

    private void showUsage(ProxiedPlayer player) {
        player.sendMessage(createText("用法: /invite <on|off|i>", ChatColor.RED));
    }

    private void handleToggle(ProxiedPlayer player, boolean enable) {
        if (plugin.getShoutManager().isInviteEnabled(player) == enable) {
            String message = enable ? "你已经打开了此选项！" : "你已经关闭了此选项！";
            player.sendMessage(createText(message, ChatColor.RED));
            return;
        }

        plugin.getShoutManager().setInviteEnabled(player, enable);
        String message = enable ? "你已开启接收广播！" : "你已关闭接收广播！";
        player.sendMessage(createText(message, ChatColor.GREEN));
    }

    private void handleInvite(ProxiedPlayer player) {
        if (plugin.getCooldownManager().isOnCooldown(player, "invite")) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(player, "invite");
            player.sendMessage(createText("请等待 " + remaining + " 秒后再发送邀请！", ChatColor.RED));

            plugin.getCooldownManager().setCooldown(
                    player,
                    "invite",
                    System.currentTimeMillis() / 1000
            );
            return;
        }

        broadcastInvite(player);
        plugin.getCooldownManager().setCooldown(player, "invite",
                plugin.getConfigManager().getConfig().getInt("delay_invite", 60));
    }

    private void broadcastInvite(ProxiedPlayer player) {
        String serverName = player.getServer().getInfo().getName();
        PrefixManager.Prefix prefix = plugin.getPrefixManager().getActivePrefix(player.getName());

        String playerNameWithPrefix = (prefix != null) ?
                "[" + prefix.color() + prefix.name() + ChatColor.RESET + "] " + player.getName() :
                player.getName();

        String border = "§6§l————————————————————————————————§r\n";
        String message = border +
                "玩家 §b" + playerNameWithPrefix + "§r 邀请你加入 §b§l[" + serverName + "]§r\n" +
                border;

        TextComponent clickableMessage = new TextComponent("§b§l[点击加入 §n" + serverName + "§b§l]");
        clickableMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + serverName));

        for (ProxiedPlayer target : plugin.getProxy().getPlayers()) {
            if (plugin.getShoutManager().isInviteEnabled(target)) {
                target.sendMessage(new TextComponent(message));
                target.sendMessage(clickableMessage);
                target.sendMessage(new TextComponent(border));
            }
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("on", "off", "i"));
        }

        String input = args.length > 0 ? args[0].toLowerCase() : "";
        completions.removeIf(completion -> !completion.toLowerCase().startsWith(input));

        return completions;
    }

    private TextComponent createText(String text, ChatColor color) {
        return new TextComponent(color + text);
    }
}