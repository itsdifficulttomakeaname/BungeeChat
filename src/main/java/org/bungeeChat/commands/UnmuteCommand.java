package org.bungeeChat.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import org.bungeeChat.BungeeChat;

import java.util.Map;
import java.util.UUID;

public class UnmuteCommand extends Command {
    private final BungeeChat plugin;

    public UnmuteCommand(BungeeChat plugin) {
        super("unmute", "antispam.admin.unmute");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showMutedList(sender);
            return;
        }

        String target = args[0];
        if (plugin.getMuteManager().unmutePlayer(target)) {
            sender.sendMessage(new TextComponent("§a已解禁玩家 " + target));
        } else {
            sender.sendMessage(new TextComponent("§c未找到被禁言的玩家 " + target));
        }
    }

    private void showMutedList(CommandSender sender) {
        Map<UUID, String> mutedPlayers = plugin.getMuteManager().getMutedPlayers();

        if (mutedPlayers.isEmpty()) {
            sender.sendMessage(new TextComponent("§a当前没有禁言中的玩家"));
            return;
        }

        TextComponent message = new TextComponent("§6禁言玩家列表 (§e" + mutedPlayers.size() + "§6):\n");

        mutedPlayers.forEach((uuid, name) -> {
            long remaining = plugin.getMuteManager().getRemainingMuteTime(uuid);
            String reason = plugin.getMuteManager().getMuteReason(uuid);

            TextComponent line = new TextComponent("§8- §e" + name + " §7(原因: §f" + reason +
                    "§7, 剩余: §f" + plugin.getMuteManager().formatDuration(remaining) + "§7) ");

            TextComponent button = new TextComponent("§c[解禁]");
            button.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/unmute " + name
            ));
            button.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§a点击解禁 " + name).create()
            ));

            line.addExtra(button);
            line.addExtra("\n");
            message.addExtra(line);
        });

        sender.sendMessage(message);
    }
}