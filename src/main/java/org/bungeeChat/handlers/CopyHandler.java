package org.bungeeChat.handlers;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bungeeChat.BungeeChat;
import org.bungeeChat.managers.PlayerDataManager;

import java.util.UUID;

public class CopyHandler {
    private final BungeeChat plugin;

    public CopyHandler(BungeeChat plugin) {
        this.plugin = plugin;
    }

    public void addCopyButton(TextComponent mainMessage, ProxiedPlayer player, String originalMessage) {
        // 两个空格作为间隔
        TextComponent buttons = new TextComponent("  ");

        // 生成唯一ID用于存储原始消息
        UUID id = UUID.randomUUID();
        PlayerDataManager.messageHistory.put(id, new PlayerDataManager.Message(player.getName(), originalMessage));

        // 创建复制按钮
        TextComponent copyButton = new TextComponent("[复制]");
        copyButton.setColor(ChatColor.GREEN);
        copyButton.setClickEvent(new ClickEvent(
                ClickEvent.Action.COPY_TO_CLIPBOARD,
                originalMessage
        ));
        copyButton.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("点击复制原始消息").create()
        ));

        buttons.addExtra(copyButton);

        // 将按钮附加到主消息
        mainMessage.addExtra(buttons);
    }
} 