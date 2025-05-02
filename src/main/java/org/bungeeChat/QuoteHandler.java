package org.bungeeChat;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bungeeChat.managers.ChatManager;
import org.bungeeChat.managers.PlayerDataManager;

import java.util.*;

public class QuoteHandler extends Command implements TabExecutor {
    private final BungeeChat plugin;

    public QuoteHandler(BungeeChat plugin) {
        super("quote", null, "q");
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
            showUsage(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                handleQuoteAdd(player, args);
                break;
            case "send":
                handleQuoteSend(player, args);
                break;
            case "mapsize":
                handleMapSize(player);
                break;
            case "cleanupmap":
                handleCleanupMap(player);
                break;
            default:
                showUsage(player);
        }
    }

    private void showUsage(ProxiedPlayer player) {
        player.sendMessage(new TextComponent("§c用法:"));
        player.sendMessage(new TextComponent("§e/quote add <ID> - 添加引用"));
        player.sendMessage(new TextComponent("§e/quote send <回复内容> - 发送引用消息"));
        if (player.hasPermission("bungeechat.quote.mapsize")) {
            player.sendMessage(new TextComponent("§e/quote mapsize - 查看引用缓存大小"));
        }
        if (player.hasPermission("bungeechat.quote.cleanupmap")) {
            player.sendMessage(new TextComponent("§e/quote cleanupmap - 清理引用缓存"));
        }
    }

    private void handleMapSize(ProxiedPlayer player) {
        if (!player.hasPermission("bungeechat.quote.mapsize")) {
            player.sendMessage(new TextComponent("§c你没有权限使用此命令！"));
            return;
        }
        int size = plugin.getPlayerDataManager().getMessageMapSize();
        player.sendMessage(new TextComponent("§a当前引用缓存大小: " + size));
    }

    private void handleCleanupMap(ProxiedPlayer player) {
        if (!player.hasPermission("bungeechat.quote.cleanupmap")) {
            player.sendMessage(new TextComponent("§c你没有权限使用此命令！"));
            return;
        }
        plugin.getPlayerDataManager().forceCleanupMessages();
        player.sendMessage(new TextComponent("§a已清理所有引用缓存！"));
    }

    private void handleQuoteAdd(ProxiedPlayer player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(new TextComponent("§c格式错误 [100]! 用法: /quote add <玩家> <消息>"));
            return;
        }
        try {
            UUID messageId = UUID.fromString(args[1]);

            // 存储到玩家临时引用
            plugin.getPlayerDataManager().setQuote(player.getUniqueId(),
                    new PlayerDataManager.QuoteData(PlayerDataManager.messageHistory.get(messageId).sender, PlayerDataManager.messageHistory.get(messageId).content));

            player.sendMessage(new TextComponent("§a已添加引用: " + PlayerDataManager.messageHistory.get(messageId).sender + ": " + PlayerDataManager.messageHistory.get(messageId).content));
            player.sendMessage(new TextComponent("§e现在输入 /quote send <回复内容> 发送消息"));
        } catch (Exception e){
            player.sendMessage(new TextComponent("§c格式错误 [101]! 用法: /quote add <玩家> <消息>"));
            return;
        }
    }

    private void handleQuoteSend(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(new TextComponent("§c用法: /quote send <回复内容>"));
            return;
        }

        PlayerDataManager.QuoteData quote = plugin.getPlayerDataManager().getQuote(player.getUniqueId());
        if (quote == null) {
            player.sendMessage(new TextComponent("§c你没有待发送的引用！"));
            return;
        }

        String reply = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sendQuotedMessage(player, quote.getPlayer(), quote.getMessage(), reply);

        // 清除引用缓存
        plugin.getPlayerDataManager().clearQuote(player.getUniqueId());
    }

    private void sendQuotedMessage(ProxiedPlayer player, String quotedPlayer,
                                   String quotedMessage, String reply) {
        String format = plugin.getConfigManager().getConfig().getString("Quote.format",
                "§7[引用] §f{quoted_player}: {quoted_message}\n§7[回复] §f{player}: {reply}");

        String formatted = format
                .replace("{quoted_player}", quotedPlayer)
                .replace("{quoted_message}", quotedMessage)
                .replace("{player}", player.getName())
                .replace("{reply}", reply);

        // 发送给同服务器所有玩家
        for (ProxiedPlayer p : player.getServer().getInfo().getPlayers()) {
            p.sendMessage(TextComponent.fromLegacyText(formatted));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("add");
            completions.add("send");
            if (sender.hasPermission("bungeechat.quote.mapsize")) {
                completions.add("mapsize");
            }
            if (sender.hasPermission("bungeechat.quote.cleanupmap")) {
                completions.add("cleanupmap");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            // 显示可引用的消息ID
            for (Map.Entry<UUID, PlayerDataManager.Message> entry :
                    PlayerDataManager.messageHistory.entrySet()) {
                completions.add(entry.getKey().toString());
            }
        }

        return completions;
    }
}