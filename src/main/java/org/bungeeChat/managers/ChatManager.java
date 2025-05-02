package org.bungeeChat.managers;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.bungeeChat.BungeeChat;
import org.bungeeChat.managers.PrefixManager.Prefix;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatManager implements Listener {
    private final BungeeChat plugin;
    private Pattern antiMessagePattern;
    private final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final List<ChatMessage> messageHistory = Collections.synchronizedList(new ArrayList<>());

    public ChatManager(BungeeChat plugin) {
        this.plugin = plugin;
        compileAntiMessagePattern();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) return;

        if (event.isCancelled()) return;

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        String message = event.getMessage();
        boolean isCommand = event.isCommand();

        // 记录日志（无论消息是否被取消）
        logMessage(player, message, isCommand);

        if (isCommand) return;

        // 处理禁言检查
        if (handleMuteCheck(player, event)) return;

        // 处理反垃圾/刷屏
        if (handleAntiAbuse(player, event)) return;

        // 处理正常消息
        handleNormalMessage(player, message, event);
    }

    private boolean handleMuteCheck(ProxiedPlayer player, ChatEvent event) {
        if (plugin.getMuteManager().isMuted(player.getUniqueId())) {
            long remaining = plugin.getMuteManager().getRemainingMuteTime(player.getUniqueId());
            player.sendMessage(new TextComponent("§c你已被禁言，剩余时间: " +
                    formatDuration(remaining)));
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    private boolean handleAntiAbuse(ProxiedPlayer player, ChatEvent event) {
        boolean cancelled = false;

        // 检查垃圾消息
        if (plugin.getConfigManager().getConfig().getBoolean("AntiSwear.enable", true)) {
            cancelled = handleAntiSwear(player, event);
        }

        // 检查刷屏
        if (!cancelled && plugin.getConfigManager().getConfig().getBoolean("AntiSpam.enable", true)) {
            cancelled = handleAntiSpam(player, event);
        }

        return cancelled;
    }

    private boolean handleAntiSwear(ProxiedPlayer player, ChatEvent event) {
        String message = event.getMessage();
        if (antiMessagePattern.matcher(message).find()) {
            event.setCancelled(true);
            String replaced = antiMessagePattern.matcher(message)
                    .replaceAll(plugin.getConfigManager().getConfig()
                            .getString("AntiSwear.replacemessage", "***"));

            // 发送替换后的消息（带称号）
            broadcastFilteredMessage(player, replaced);

            if (!player.hasPermission("antispam.admin.bypass")) {
                plugin.getMuteManager().handleViolation(player);
            }
            return true;
        }
        return false;
    }

    private boolean handleAntiSpam(ProxiedPlayer player, ChatEvent event) {
        // 实现刷屏检测逻辑
        // 返回true表示消息被取消
        return false;
    }

    private void handleNormalMessage(ProxiedPlayer player, String message, ChatEvent event) {
        // 获取玩家称号
        Prefix prefix = plugin.getPrefixManager().getActivePrefix(player.getName());

        // 构建格式化消息
        String formatted = formatChatMessage(player, prefix, message);
        TextComponent mainMessage = new TextComponent(TextComponent.fromLegacyText(formatted));

        // 添加交互按钮到主消息
        addMessageButtons(mainMessage, player, message);

        // 广播消息
        broadcastFilteredMessage(player, mainMessage);

        // 处理提及
        handleMentions(player, message);

        event.setCancelled(true);
    }

    private String formatChatMessage(ProxiedPlayer player, Prefix prefix, String message) {
        StringBuilder builder = new StringBuilder();

        // 添加称号
        if (prefix != null && player.hasPermission(prefix.permission())) {
            builder.append("[")
                    .append(prefix.color())
                    .append(prefix.name())
                    .append(ChatColor.RESET)
                    .append("] ");
        }

        // 添加玩家名和消息
        builder.append("<")
                .append(player.getName())
                .append("> ")
                .append(message);

        return builder.toString();
    }

    private void broadcastFilteredMessage(ProxiedPlayer sender, TextComponent message) {
        String serverName = sender.getServer().getInfo().getName();
        for (ProxiedPlayer recipient : plugin.getProxy().getServerInfo(serverName).getPlayers()) {
            if (!plugin.getShoutManager().isBlocked(recipient, sender)) {
                recipient.sendMessage(message);
            }
        }
    }

    private void logMessage(ProxiedPlayer player, String message, boolean isCommand) {
        try {
            File logFile = getLogFile(player.getServer().getInfo().getName(), isCommand);

            Prefix prefix = plugin.getPrefixManager().getActivePrefix(player.getName());
            String prefixTag = prefix != null ? "[" + prefix.name() + "]" : "";

            String logEntry = String.format("[%s] %s%s: %s",
                    logDateFormat.format(new Date()),
                    prefixTag,
                    player.getName(),
                    message);

            try (FileWriter writer = new FileWriter(logFile, StandardCharsets.UTF_8, true)) {
                writer.write(logEntry + "\n");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("记录日志失败: " + e.getMessage());
        }
    }

    private File getLogFile(String serverName, boolean isCommand) {
        boolean divideServer = plugin.getConfigManager().getConfig()
                .getBoolean("ChatLog.IfDevide-server", false);
        boolean divideType = plugin.getConfigManager().getConfig()
                .getBoolean("ChatLog.IfDevide-type", false);

        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        File serverDir = divideServer ? new File(logsDir, serverName) : logsDir;
        if (!serverDir.exists()) serverDir.mkdirs();

        String fileName = divideType ?
                (isCommand ? "commands.log" : "chat.log") : "all.log";

        return new File(serverDir, fileName);
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return hours + "时" + minutes + "分" + secs + "秒";
    }

    public void reloadAntiMessagePattern() {
        compileAntiMessagePattern();
    }

    private void compileAntiMessagePattern() {
        String regex = plugin.getConfigManager().getAntiMessages().getStringList("antimessages").stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        antiMessagePattern = Pattern.compile("(?i)(" + regex + ")");
    }

    private void handleMentions(ProxiedPlayer sender, String message) {
        if (message.contains("@")) {
            String[] parts = message.split(" ");
            for (String part : parts) {
                if (part.startsWith("@")) {
                    String targetName = part.substring(1);
                    for (ProxiedPlayer target : sender.getServer().getInfo().getPlayers()) {
                        if (target.getName().equalsIgnoreCase(targetName)) {
                            target.sendMessage(new TextComponent("§e玩家 " + sender.getName() + " 提到了你！"));
                            break;
                        }
                    }
                }
            }
        }
    }

    public static class ChatMessage {
        private final String player;
        private final String message;
        private final long timestamp;

        public ChatMessage(String player, String message, long timestamp) {
            this.player = player;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getPlayer() {
            return player;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    private void addMessageButtons(TextComponent mainMessage, ProxiedPlayer player, String message) {
        // 两个空格作为间隔
        TextComponent buttons = new TextComponent("  ");

        // @按钮
        TextComponent atButton = new TextComponent("[@]");
        atButton.setColor(ChatColor.GREEN);
        atButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "@"));
        atButton.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("点击提及玩家").create()
        ));

        // 引用按钮

        UUID id = UUID.randomUUID();
        PlayerDataManager.messageHistory.put(id, new PlayerDataManager.Message(player.getName(), message));

        TextComponent quoteButton = new TextComponent("[引用]");
        quoteButton.setColor(ChatColor.YELLOW);
        quoteButton.setClickEvent(new ClickEvent(
                ClickEvent.Action.SUGGEST_COMMAND,
                "/quote add " + id.toString()
        ));
        quoteButton.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("点击引用此消息").create()
        ));

        buttons.addExtra(atButton);
        buttons.addExtra(" ");
        buttons.addExtra(quoteButton);

        // 将按钮附加到主消息
        mainMessage.addExtra(buttons);
    }

    // 保留旧方法供其他代码调用
    private void broadcastFilteredMessage(ProxiedPlayer sender, String message) {
        broadcastFilteredMessage(sender, new TextComponent(TextComponent.fromLegacyText(message)));
    }
}