package org.bungeeChat.managers;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bungeeChat.BungeeChat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlayerDataManager {
    public static class Message {
        public final String sender, content;
        public Message(String sender, String content){
            this.sender=sender;
            this.content=content;
        }
    }
    private final BungeeChat plugin;
    private Configuration playerData;
    private File dataFile;
    private final Map<UUID, QuoteData> playerQuotes = new ConcurrentHashMap<>();
    public static final Map<UUID, Message> messageHistory = new ConcurrentHashMap<>();
    private static final long QUOTE_TIMEOUT = 5 * 60 * 1000; // 5分钟过期时间

    public PlayerDataManager(BungeeChat plugin) {
        this.plugin = plugin;
        loadPlayerData();
    }

    private void loadPlayerData() {
        try {
            dataFile = new File(plugin.getDataFolder(), "PlayerData.yml");
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
            playerData = ConfigurationProvider.getProvider(YamlConfiguration.class).load(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("加载玩家数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void savePlayerData(String playerName, String prefix) {
        try {
            if (prefix == null) {
                playerData.set(playerName, null);
            } else {
                playerData.set(playerName, prefix);
            }
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(playerData, dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存玩家数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getPlayerPrefix(String playerName) {
        return playerData.getString(playerName);
    }

    public void loadPlayerPrefix(ProxiedPlayer player) {
        String prefixKey = getPlayerPrefix(player.getName());
        if (prefixKey != null) {
            plugin.getPrefixManager().enablePrefix(player, prefixKey);
        }
    }

    public void setQuote(UUID playerId, QuoteData quote) {
        playerQuotes.put(playerId, quote);

        // 设置过期清理任务
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            QuoteData currentQuote = playerQuotes.get(playerId);
            if (currentQuote != null && currentQuote.equals(quote)) {
                clearQuote(playerId);
                ProxiedPlayer player = plugin.getProxy().getPlayer(playerId);
                if (player != null) {
                    player.sendMessage(new TextComponent("§c你的引用已过期，请重新选择"));
                }
            }
        }, QUOTE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public QuoteData getQuote(UUID playerId) {
        QuoteData quote = playerQuotes.get(playerId);
        if (quote != null && quote.isExpired(QUOTE_TIMEOUT)) {
            clearQuote(playerId);
            return null;
        }
        return quote;
    }

    public void clearQuote(UUID playerId) {
        playerQuotes.remove(playerId);
    }

    // 调试用方法
    public Map<UUID, QuoteData> getAllQuotes() {
        return Collections.unmodifiableMap(playerQuotes);
    }

    public static class QuoteData {
        private final String player;
        private final String message;
        private final long timestamp;

        public QuoteData(String player, String message) {
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

        public boolean isExpired(long timeoutMs) {
            return (System.currentTimeMillis() - timestamp) > timeoutMs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuoteData quoteData = (QuoteData) o;
            return timestamp == quoteData.timestamp &&
                    Objects.equals(player, quoteData.player) &&
                    Objects.equals(message, quoteData.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(player, message, timestamp);
        }
    }

    // 清理过期消息
    public void cleanupExpiredMessages() {
        long expireTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(MESSAGE_EXPIRE_HOURS);
        messageHistory.entrySet().removeIf(entry -> entry.getValue().timestamp < expireTime);
    }

    // 获取当前消息数量
    public int getMessageMapSize() {
        return messageHistory.size();
    }

    // 强制清理所有消息
    public void forceCleanupMessages() {
        messageHistory.clear();
    }
}