package org.bungeeChat.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;
import org.bungeeChat.BungeeChat;

public class PrefixesCommand extends Command {
    private final BungeeChat plugin;

    public PrefixesCommand(BungeeChat plugin) {
        super("prefixes");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            if (args.length == 0) {
                showPrefixList(player);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("enable")) {
                String prefix = args[1];
                plugin.getPrefixManager().enablePrefix(player, prefix);
                plugin.getPlayerDataManager().savePlayerData(player.getName(), prefix);
                showPrefixList(player); // 操作后重新显示列表
            } else if (args.length == 1 && args[0].equalsIgnoreCase("disable")) {
                plugin.getPrefixManager().disablePrefix(player);
                plugin.getPlayerDataManager().savePlayerData(player.getName(), null);
                showPrefixList(player); // 操作后重新显示列表
            } else {
                player.sendMessage(new TextComponent(ChatColor.RED + "用法: /prefixes [enable|disable] [称号]"));
                showPrefixList(player); // 错误输入也显示列表
            }
        } else {
            sender.sendMessage(new TextComponent(ChatColor.RED + "该指令只能由玩家使用！"));
        }
    }

    private void showPrefixList(ProxiedPlayer player) {
        String serverPrefix = plugin.getConfigManager().getConfig().getString("Server-Prefix", "称号系统");
        TextComponent header = new TextComponent(ChatColor.GOLD + "================" + serverPrefix + "================");
        player.sendMessage(header);

        Configuration prefixConfig = plugin.getConfigManager().getConfig().getSection("Prefixes.BungeePrefix.level");
        if (prefixConfig == null) {
            player.sendMessage(new TextComponent(ChatColor.RED + "配置文件格式错误，未找到 Prefixes.BungeePrefix.level 部分！"));
            return;
        }

        for (String level : prefixConfig.getKeys()) {
            String permission = "BungeePrefix.level." + level;
            if (player.hasPermission(permission)) {
                Configuration levelSection = prefixConfig.getSection(level);
                for (String prefixKey : levelSection.getKeys()) {
                    Configuration prefixData = levelSection.getSection(prefixKey);
                    String name = prefixData.getString("name");
                    String color = prefixData.getString("color", "white");
                    ChatColor chatColor = getChatColor(color);

                    // 使用新的getActivePrefixKey方法比较
                    boolean isActive = prefixKey.equals(plugin.getPrefixManager().getActivePrefixKey(player.getName()));

                    TextComponent prefixLine = new TextComponent("· " + chatColor + name + " ");
                    if (isActive) {
                        prefixLine.addExtra(createButton("[禁用]", "/prefixes disable", ChatColor.RED));
                    } else {
                        prefixLine.addExtra(createButton("[启用]", "/prefixes enable " + prefixKey, ChatColor.GREEN));
                    }
                    player.sendMessage(prefixLine);
                }
            }
        }
    }

    private TextComponent createButton(String text, String command, ChatColor color) {
        TextComponent button = new TextComponent(text);
        button.setColor(color);
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("点击执行操作").create()));
        return button;
    }

    private ChatColor getChatColor(String color) {
        try {
            return ChatColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChatColor.WHITE;
        }
    }
}