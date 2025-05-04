package org.bungeeChat.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bungeeChat.BungeeChat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MuteCommand extends Command implements TabExecutor {
    private final BungeeChat plugin;

    public MuteCommand(BungeeChat plugin) {
        super("mute", "bungeechat.admin.mute");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(new TextComponent("§c用法: /mute <time> <unit(s/m/h)> <player> [reason]"));
            return;
        }

        try {
            int time = Integer.parseInt(args[0]);
            String unit = args[1].toLowerCase();
            String playerName = args[2];
            String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "管理员禁言";

            long duration;
            switch (unit) {
                case "s":
                    duration = time;
                    break;
                case "m":
                    duration = time * 60L;
                    break;
                case "h":
                    duration = time * 3600L;
                    break;
                default:
                    sender.sendMessage(new TextComponent("§c单位必须是秒(s)、分钟(m)或小时(h)！"));
                    return;
            }

            ProxiedPlayer target = plugin.getProxy().getPlayer(playerName);
            if (target == null) {
                sender.sendMessage(new TextComponent("§c玩家未在线或不存在！"));
                return;
            }

            if (plugin.getMuteManager().isMuted(target.getUniqueId())) {
                sender.sendMessage(new TextComponent("§c该玩家已被禁言！"));
                return;
            }

            plugin.getMuteManager().mutePlayer(target, duration, reason);
            sender.sendMessage(new TextComponent("§a已禁言玩家 " + target.getName() + "，时长: " +
                    plugin.getMuteManager().formatDuration(duration) + "，原因: " + reason));

        } catch (NumberFormatException e) {
            sender.sendMessage(new TextComponent("§c时间必须是整数！"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,  String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 补全玩家名
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            // 补全时间单位
            String[] timeUnits = {"s", "m", "h", "d"};
            for (String unit : timeUnits) {
                if (unit.startsWith(args[1].toLowerCase())) {
                    completions.add(unit);
                }
            }
            // 添加时间提示
            if (args[1].isEmpty()) {
                completions.add("<time>");
            }
        }

        return completions;
    }
}