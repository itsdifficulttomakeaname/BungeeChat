package org.bungeeChat.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bungeeChat.BungeeChat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShoutCountCommand extends Command implements TabExecutor {
    private final BungeeChat plugin;

    public ShoutCountCommand(BungeeChat plugin) {
        super("shoutcount", "bungeechat.admin.shoutcount", "sc");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(createText("用法: /shoutcount <add/reduce/set> <玩家名> <数量>", ChatColor.RED));
            return;
        }

        String operation = args[0].toLowerCase();
        String playerName = args[1];
        int amount = parseAmount(args[2], sender);
        if (amount == Integer.MIN_VALUE) return;

        ProxiedPlayer targetPlayer = plugin.getProxy().getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(createText("未找到玩家 " + playerName + "！", ChatColor.RED));
            return;
        }

        handleShoutCountOperation(sender, operation, targetPlayer, playerName, amount);
    }

    private int parseAmount(String amountStr, CommandSender sender) {
        try {
            return Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(createText("数量必须是一个整数！", ChatColor.RED));
            return Integer.MIN_VALUE;
        }
    }

    private void handleShoutCountOperation(CommandSender sender, String operation,
                                           ProxiedPlayer target, String playerName, int amount) {
        int currentCount = plugin.getShoutManager().getShoutCount(target);
        int newCount;

        switch (operation) {
            case "add":
                newCount = currentCount + amount;
                if (newCount < 0) {
                    sender.sendMessage(createText("这个数太大了，玩家没有这么多喇叭！", ChatColor.RED));
                    return;
                }
                break;
            case "reduce":
                newCount = currentCount - amount;
                if (newCount < 0) {
                    sender.sendMessage(createText("这个数太大了，玩家没有这么多喇叭！", ChatColor.RED));
                    return;
                }
                break;
            case "set":
                if (amount <= 0) {
                    sender.sendMessage(createText("数量必须是一个大于零的整数！", ChatColor.RED));
                    return;
                }
                newCount = amount;
                break;
            default:
                sender.sendMessage(createText("未知的操作类型！请使用 add/reduce/set。", ChatColor.RED));
                return;
        }

        plugin.getShoutManager().setShoutCount(target, newCount);
        sendSuccessMessage(sender, operation, playerName, amount, newCount);
    }

    private void sendSuccessMessage(CommandSender sender, String operation,
                                    String playerName, int amount, int newCount) {
        String message;
        switch (operation) {
            case "add":
                message = "已为玩家 " + playerName + " 添加 " + amount + " 个喇叭，当前数量: " + newCount;
                break;
            case "reduce":
                message = "已为玩家 " + playerName + " 减少 " + amount + " 个喇叭，当前数量: " + newCount;
                break;
            case "set":
                message = "已将玩家 " + playerName + " 的喇叭数量设置为 " + newCount;
                break;
            default:
                return;
        }
        sender.sendMessage(createText(message, ChatColor.GREEN));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("add", "reduce", "set"));
        } else if (args.length == 2) {
            plugin.getProxy().getPlayers().forEach(player -> completions.add(player.getName()));
        } else if (args.length == 3) {
            completions.add("<number>");
        }

        String currentInput = args.length > 0 ? args[args.length - 1].toLowerCase() : "";
        completions.removeIf(s -> !s.toLowerCase().startsWith(currentInput));

        return completions;
    }

    private TextComponent createText(String text, ChatColor color) {
        return new TextComponent(color + text);
    }
}