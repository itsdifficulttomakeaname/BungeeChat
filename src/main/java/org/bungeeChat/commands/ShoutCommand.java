package org.bungeeChat.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bungeeChat.BungeeChat;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.yaml.snakeyaml.Yaml;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.stream.Collectors;

public class ShoutCommand extends Command implements TabExecutor {
    private final BungeeChat plugin;
    private final File blockedFile;  // 屏蔽列表存储文件
    private Map<String, List<String>> blockedPlayers;  // 内存中的屏蔽数据

    public ShoutCommand(BungeeChat plugin) {
        super("shout", null, "broadcast");
        this.plugin = plugin;
        this.blockedFile = new File(plugin.getDataFolder(), "Blocked.yml");
        this.blockedPlayers = new HashMap<>();
        loadBlockedPlayers();  // 初始化时加载屏蔽列表
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // 检查发送者是否为玩家
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("§c只有玩家可以使用此命令"));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length == 0) {
            sendUsage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        // 根据子命令类型处理不同功能
        switch (subCommand) {
            case "m":
                handleSimpleShout(player, args);
                break;
            case "im":
                handleInteractiveShout(player, args);
                break;
            case "pb":
                handlePlayerBlock(player, args);
                break;
            default:
                handleDefaultShout(player, args);
                break;
        }
    }

    // 显示命令用法帮助
    private void sendUsage(ProxiedPlayer player) {
        player.sendMessage(new TextComponent("§6===== 喇叭命令帮助 ====="));
        player.sendMessage(new TextComponent("§a/shout m <消息> §7- 发送普通全服喇叭"));
        player.sendMessage(new TextComponent("§a/shout im <消息> §7- 发送带加入按钮的喇叭"));
        player.sendMessage(new TextComponent("§a/shout pb <玩家名> §7- 屏蔽/取消屏蔽玩家"));
        player.sendMessage(new TextComponent("§a/shout <消息> §7- 默认喇叭功能"));
    }

    // 处理普通喊话功能
    private void handleSimpleShout(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(new TextComponent("§c用法: /shout m <消息>"));
            return;
        }

        if (!checkShoutConditions(player)) {
            return;
        }

        // 拼接消息内容(去掉"m "前缀)
        String message = String.join(" ", args).substring(2);
        broadcastMessage(player, message);
        applyShoutCost(player);
    }

    // 处理交互式喊话(带加入按钮)
    private void handleInteractiveShout(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(new TextComponent("§c用法: /shout im <消息>"));
            return;
        }

        if (!checkShoutConditions(player)) {
            return;
        }

        // 拼接消息内容(去掉"im "前缀)
        String message = String.join(" ", args).substring(3);
        broadcastInteractiveMessage(player, message);
        applyShoutCost(player);
    }

    // 处理玩家屏蔽功能
    private void handlePlayerBlock(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(new TextComponent("§c用法: /shout pb <玩家名>"));
            return;
        }

        String targetName = args[1];
        togglePlayerBlock(player, targetName);
    }

    // 处理默认喊话(无子命令)
    private void handleDefaultShout(ProxiedPlayer player, String[] args) {
        if (!checkShoutConditions(player)) {
            return;
        }

        String message = String.join(" ", args);
        broadcastMessage(player, message);
        applyShoutCost(player);
    }

    // 检查喊话条件(冷却时间和剩余次数)
    private boolean checkShoutConditions(ProxiedPlayer player) {
        // 检查权限绕过
        if (player.hasPermission("bungeechat.shout.bypass")) {
            return true;
        }

        // 检查冷却时间
        if (plugin.getCooldownManager().isOnCooldown(player, "shout")) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(player, "shout");
            player.sendMessage(new TextComponent("§c喇叭冷却中，剩余时间: " +
                    plugin.getMuteManager().formatDuration(remaining)));
            return false;
        }

        // 检查喇叭剩余数量
        int shoutCount = plugin.getShoutManager().getShoutCount(player);
        if (shoutCount <= 0) {
            player.sendMessage(new TextComponent("§c你的喇叭数量不足！"));
            return false;
        }
        return true;
    }

    // 扣除喇叭次数并设置冷却
    private void applyShoutCost(ProxiedPlayer player) {
        int shoutCount = plugin.getShoutManager().getShoutCount(player);
        plugin.getShoutManager().setShoutCount(player, shoutCount - 1);
        plugin.getCooldownManager().setCooldown(player, "shout", System.currentTimeMillis() / 1000);
    }

    // 广播普通消息
    private void broadcastMessage(ProxiedPlayer sender, String message) {
        String formatted = "§6[喇叭]§e " + sender.getName() + ": §f" + message;
        TextComponent component = new TextComponent(formatted);

        // 向所有未被屏蔽的玩家发送消息
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            if (!isBlocked(player, sender)) {
                player.sendMessage(component);
            }
        }
    }

    // 广播交互消息(带服务器加入按钮)
    private void broadcastInteractiveMessage(ProxiedPlayer sender, String message) {
        // 生成一次性令牌
        BungeeChat.ServerSwitchToken token = plugin.generateSwitchToken(sender);

        String formatted = String.format("§6[喇叭]§e %s: §f%s §b[点击加入]",
                sender.getName(),
                message
        );

        // 构建带校验的跳转指令
        String command = String.format("/switchserver %s %s",
                sender.getServer().getInfo().getName(),
                token.tokenId()
        );

        TextComponent component = new TextComponent(formatted);
        component.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                command
        ));

        // 发送给所有未屏蔽的玩家
        plugin.getProxy().getPlayers().stream()
                .filter(p -> !isBlocked(p, sender))
                .forEach(p -> p.sendMessage(component));
    }

    // 切换玩家屏蔽状态
    private void togglePlayerBlock(ProxiedPlayer player, String targetName) {
        String playerName = player.getName().toLowerCase();
        List<String> blocked = blockedPlayers.computeIfAbsent(playerName, k -> new ArrayList<>());

        if (blocked.contains(targetName.toLowerCase())) {
            // 如果已屏蔽则取消屏蔽
            blocked.remove(targetName.toLowerCase());
            player.sendMessage(new TextComponent("§a已取消屏蔽玩家 " + targetName));
        } else {
            // 如果未屏蔽则添加屏蔽
            blocked.add(targetName.toLowerCase());
            player.sendMessage(new TextComponent("§a已屏蔽玩家 " + targetName + " 的喇叭消息"));
        }

        saveBlockedPlayers();  // 保存修改到文件
    }

    // 检查目标玩家是否被屏蔽
    private boolean isBlocked(ProxiedPlayer receiver, ProxiedPlayer sender) {
        List<String> blocked = blockedPlayers.get(receiver.getName().toLowerCase());
        return blocked != null && blocked.contains(sender.getName().toLowerCase());
    }

    // 从文件加载屏蔽列表
    @SuppressWarnings("unchecked")
    private void loadBlockedPlayers() {
        if (!blockedFile.exists()) {
            return;
        }

        Yaml yaml = new Yaml();
        try (FileReader reader = new FileReader(blockedFile)) {
            blockedPlayers = yaml.loadAs(reader, Map.class);
            if (blockedPlayers == null) {
                blockedPlayers = new HashMap<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 保存屏蔽列表到文件
    private void saveBlockedPlayers() {
        try {
            // 确保插件数据目录存在
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdir();
            }

            Yaml yaml = new Yaml();
            try (FileWriter writer = new FileWriter(blockedFile)) {
                yaml.dump(blockedPlayers, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 第一个参数补全子命令
            return Arrays.asList("m", "im", "pb").stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("pb")) {
            // pb子命令的玩家名补全
            return plugin.getProxy().getPlayers().stream()
                    .map(ProxiedPlayer::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}